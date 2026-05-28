package com.alcohol.compat.service;

import com.alcohol.common.BizException;
import com.alcohol.compat.CheckInAccessHelper;
import com.alcohol.compat.CompatAuthSupport;
import com.alcohol.compat.FrontendMapper;
import com.alcohol.compat.dto.*;
import com.alcohol.compat.vo.*;
import com.alcohol.context.UserContext;
import com.alcohol.dto.checkin.CreateCheckInRequest;
import com.alcohol.entity.Bar;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.BarMapper;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import com.alcohol.service.*;
import com.alcohol.service.auth.UserAccountService;
import com.alcohol.util.CheckInStatsUtil;
import com.alcohol.util.GeoUtil;
import com.alcohol.config.GooglePlacesProperties;
import com.alcohol.places.BarPlacesSyncService;
import com.alcohol.places.GooglePlacesService;
import com.alcohol.places.PlacesToBarMapper;
import com.alcohol.util.PhoneEmailUtil;
import com.alcohol.vo.persona.PersonaVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 同事 Expo 前端契约实现（路径与 JSON 形状对齐 mock-server）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrontendCompatService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserAccountService userAccountService;
    private final CompatAuthSupport compatAuthSupport;
    private final FrontendMapper mapper;
    private final CheckInService checkInService;
    private final CheckInMapper checkInMapper;
    private final UserMapper userMapper;
    private final BarMapper barMapper;
    private final PersonaService personaService;
    private final UserDrinkService userDrinkService;
    private final GooglePlacesService googlePlacesService;
    private final GooglePlacesProperties googlePlacesProperties;
    private final BarPlacesSyncService barPlacesSyncService;
    private final PlacesToBarMapper placesToBarMapper;
    private final com.alcohol.community.CommunityFeedService communityFeedService;
    private final com.alcohol.chat.ChatService chatService;
    private final CheckInAccessHelper checkInAccessHelper;

    public FrontendAuthResponse login(FrontendLoginRequest req) {
        String email = PhoneEmailUtil.normalizeEmail(req.getEmail());
        User user = userAccountService.findByEmail(email);
        compatAuthSupport.assertPassword(user, req.getPassword());
        userAccountService.assertActive(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public FrontendAuthResponse register(FrontendRegisterRequest req) {
        String email = PhoneEmailUtil.normalizeEmail(req.getEmail());
        if (userAccountService.findByEmail(email) != null) {
            throw new BizException("Email already registered", 409);
        }
        User user = userAccountService.createEmailUser(email, req.getPassword(), req.getDisplayName());
        return buildAuthResponse(user);
    }

    public FrontendAuthResponse refresh(FrontendRefreshRequest req) {
        User user = resolveUserForRefresh(req);
        return buildAuthResponse(user);
    }

    private User resolveUserForRefresh(FrontendRefreshRequest req) {
        String userId = UserContext.getUserId();
        if (StringUtils.hasText(userId)) {
            return requireCurrentUser();
        }
        if (req != null && StringUtils.hasText(req.getRefreshToken())) {
            String token = req.getRefreshToken();
            if (token.endsWith(".refresh")) {
                token = token.substring(0, token.length() - ".refresh".length());
            }
            if (compatAuthSupport.validateAccessToken(token)) {
                userId = compatAuthSupport.userIdFromToken(token);
                User user = userMapper.selectById(userId);
                if (user != null) {
                    return user;
                }
            }
        }
        return compatAuthSupport.ensureDemoUser();
    }

    public FrontendUserVO me() {
        User user = requireCurrentUser();
        return mapper.toUser(user, resolvePersonaStatement(user.getId()));
    }

    public FrontendCheckInVO createCheckIn(FrontendCreateCheckInRequest req) {
        checkInAccessHelper.requireUserId();
        if (StringUtils.hasText(req.getBarId())) {
            barPlacesSyncService.ensureBarExists(req.getBarId());
        }
        var vo = checkInService.create(toBackendCreate(req));
        CheckIn entity = checkInMapper.selectById(vo.getId());
        return mapper.toCheckIn(entity, userMapper.selectById(entity.getUserId()), resolveBar(entity.getBarId()));
    }

    public FrontendItemsResponse<FrontendCheckInVO> recentCheckIns() {
        String userId = checkInAccessHelper.requireUserId();
        List<CheckIn> list = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 50"));
        return FrontendItemsResponse.of(toCheckInList(list));
    }

    public FrontendCheckInVO getCheckIn(String id) {
        CheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BizException("Check-in not found", 404);
        }
        checkInAccessHelper.assertReadable(checkIn);
        return mapper.toCheckIn(checkIn, userMapper.selectById(checkIn.getUserId()), resolveBar(checkIn.getBarId()));
    }

    @Transactional
    public void deleteCheckIn(String id) {
        CheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BizException("Check-in not found", 404);
        }
        checkInAccessHelper.assertSelf(checkIn.getUserId());
        checkInMapper.deleteById(id);
    }

    public FrontendItemsResponse<FrontendCheckInVO> userCheckIns(String userId) {
        checkInAccessHelper.assertSelf(userId);
        List<CheckIn> list = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .orderByDesc(CheckIn::getCreatedAt));
        return FrontendItemsResponse.of(toCheckInList(list));
    }

    public FrontendDiarySummaryVO diarySummary(String month) {
        YearMonth ym = parseMonth(month);
        String userId = checkInAccessHelper.requireUserId();
        List<CheckIn> monthList = loadMonthCheckIns(userId, ym);

        FrontendDiarySummaryVO vo = new FrontendDiarySummaryVO();
        vo.setMonth(ym.format(MONTH));
        vo.setCheckInCount(monthList.size());
        vo.setBarsVisited((int) CheckInStatsUtil.distinctBars(monthList));
        vo.setAverageRating(CheckInStatsUtil.avgRatingForFrontend(monthList));
        List<CheckIn> allUserCheckIns = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId));
        vo.setCurrentStreak(CheckInStatsUtil.currentStreak(allUserCheckIns));
        return vo;
    }

    public List<FrontendCalendarDayVO> diaryCalendar(String month) {
        YearMonth ym = parseMonth(month);
        String userId = checkInAccessHelper.requireUserId();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (CheckIn checkIn : loadMonthCheckIns(userId, ym)) {
            if (checkIn.getCreatedAt() == null) {
                continue;
            }
            String date = checkIn.getCreatedAt().toLocalDate().toString();
            counts.merge(date, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(e -> {
                    FrontendCalendarDayVO day = new FrontendCalendarDayVO();
                    day.setDate(e.getKey());
                    day.setCount(e.getValue());
                    return day;
                })
                .toList();
    }

    public FrontendDiaryStatsVO diaryStats() {
        String userId = checkInAccessHelper.requireUserId();
        List<CheckIn> list = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId));
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<String, Integer> moodCounts = new HashMap<>();
        for (CheckIn checkIn : list) {
            String cat = mapper.toFrontendCategory(checkIn.getDrinkCategory());
            if (StringUtils.hasText(cat)) {
                categoryCounts.merge(cat, 1, Integer::sum);
            }
            for (String mood : com.alcohol.util.JsonUtil.parseStringList(checkIn.getMoodTags())) {
                moodCounts.merge(mood, 1, Integer::sum);
            }
        }
        FrontendDiaryStatsVO vo = new FrontendDiaryStatsVO();
        vo.setCategoryCounts(categoryCounts);
        vo.setMoodCounts(moodCounts);
        return vo;
    }

    public List<FrontendBarVO> nearbyBars(String city, Double lat, Double lng) {
        if (googlePlacesService.isAvailable()) {
            try {
                String effectiveCity = StringUtils.hasText(city)
                        ? city
                        : googlePlacesProperties.getDefaultCity();
                List<FrontendBarVO> fromGoogle = googlePlacesService.searchNearby(lat, lng, effectiveCity);
                if (!fromGoogle.isEmpty()) {
                    return enrichBarCheckInCounts(fromGoogle);
                }
            } catch (Exception e) {
                log.warn("Google Places nearby failed, falling back to seed data: {}", e.getMessage());
            }
        }
        return nearbyBarsFromSeed(city, lat, lng);
    }

    public List<FrontendBarVO> barRankings(String city, Double lat, Double lng) {
        return nearbyBars(city, lat, lng).stream()
                .sorted(Comparator.comparing(FrontendBarVO::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public FrontendBarVO barDetail(String id, Double lat, Double lng) {
        if (placesToBarMapper.isGoogleBarId(id) && googlePlacesService.isAvailable()) {
            try {
                FrontendBarVO fromGoogle = googlePlacesService.getDetails(id, lat, lng);
                if (fromGoogle != null) {
                    barPlacesSyncService.ensureBarExists(id);
                    fromGoogle.setCheckInCount(countCheckIns(id));
                    return fromGoogle;
                }
            } catch (Exception e) {
                log.warn("Google Places detail failed for {}: {}", id, e.getMessage());
            }
        }
        Bar bar = barMapper.selectById(id);
        if (bar == null || bar.getIsActive() == null || bar.getIsActive() != 1) {
            throw new BizException("Bar not found", 404);
        }
        FrontendBarVO vo = mapper.toBar(bar, estimateDistance(bar, lat, lng));
        vo.setCheckInCount(countCheckIns(id));
        return vo;
    }

    private List<FrontendBarVO> nearbyBarsFromSeed(String city, Double lat, Double lng) {
        String normalized = mapper.normalizeCityIn(city);
        List<Bar> bars = barMapper.selectList(new LambdaQueryWrapper<Bar>()
                .eq(Bar::getIsActive, 1)
                .and(StringUtils.hasText(normalized), w -> w.eq(Bar::getCity, normalized)
                        .or()
                        .eq(Bar::getCity, city)));
        return bars.stream()
                .map(b -> mapper.toBar(b, estimateDistance(b, lat, lng)))
                .sorted(Comparator.comparingInt(b -> b.getDistanceMeters() != null ? b.getDistanceMeters() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    private List<FrontendBarVO> enrichBarCheckInCounts(List<FrontendBarVO> bars) {
        for (FrontendBarVO bar : bars) {
            bar.setCheckInCount(countCheckIns(bar.getId()));
        }
        return bars;
    }

    private int countCheckIns(String barId) {
        Long count = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getBarId, barId));
        return count != null ? count.intValue() : 0;
    }

    public FrontendItemsResponse<FrontendCheckInVO> barCheckIns(String barId) {
        LocalDateTime now = LocalDateTime.now();
        List<CheckIn> list = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getBarId, barId)
                .in(CheckIn::getVisibility, "PUBLIC", "TONIGHT_ONLY")
                .gt(CheckIn::getExpiresAt, now)
                .orderByDesc(CheckIn::getCreatedAt));
        return FrontendItemsResponse.of(toCheckInList(list));
    }

    public FrontendItemsResponse<FrontendGalleryPostVO> galleryFeed(String city) {
        return communityFeedService.galleryFeed(city, "24h");
    }

    public Map<String, Object> uploadImageStub() {
        String id = UUID.randomUUID().toString();
        return Map.of(
                "imageUrl", "https://images.barlog.local/uploads/" + id + ".jpg",
                "width", 1200,
                "height", 1600,
                "mimeType", "image/jpeg");
    }

    public List<Map<String, Object>> drinkCollection() {
        return userDrinkService.listMyCollection().stream()
                .map(ud -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", ud.getDrinkId());
                    item.put("name", ud.getDrinkName());
                    item.put("category", mapper.toFrontendCategory(ud.getCategory()));
                    item.put("collectedAt", ud.getLastCheckInAt() != null
                            ? ud.getLastCheckInAt().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_INSTANT)
                            : null);
                    item.put("imageUrl", ud.getIconUrl());
                    return item;
                })
                .toList();
    }

    public Map<String, Object> drinkDetail(String drinkId) {
        return drinkCollection().stream()
                .filter(d -> drinkId.equals(d.get("id")))
                .findFirst()
                .orElseThrow(() -> new BizException("Drink not found", 404));
    }

    public Map<String, Object> personaCurrent() {
        PersonaVO persona = personaService.getMyPersona();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "persona_" + UserContext.getUserId());
        body.put("statement", persona.getGeneratedText());
        body.put("traits", persona.getFlavorProfile() != null ? persona.getFlavorProfile() : List.of());
        body.put("updatedAt", persona.getUpdatedAt() != null
                ? persona.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_INSTANT)
                : java.time.Instant.now().toString());
        return body;
    }

    public Map<String, Object> aiRecognizeDrink() {
        return Map.of(
                "drinkName", "Paper Plane",
                "drinkCategory", "cocktail",
                "confidence", 0.82);
    }

    public Map<String, Object> aiGenerateCardCopy(Map<String, Object> body) {
        String drink = body != null && body.get("drinkName") != null ? body.get("drinkName").toString() : "Tonight's pour";
        return Map.of(
                "title", "A bright little landing",
                "subtitle", drink + " at Amber Room",
                "body", "Citrus, bourbon, and a softer finish than expected.",
                "style", "receipt");
    }

    public Map<String, Object> aiGeneratePersona() {
        return Map.of("statement", "Late-night citrus explorer");
    }

    public Map<String, Object> aiMatchReason() {
        return Map.of("reason", "You both prefer quiet cocktail bars and bitter drinks.");
    }

    public Map<String, Object> aiIcebreakers() {
        return Map.of("suggestions", List.of(
                "What drink tells you a bar knows what it is doing?",
                "Quiet counter or lively table tonight?",
                "Pick one: martini, negroni, or highball?"));
    }

    public Map<String, Object> matchSession() {
        return Map.of("sessionId", "match_" + UUID.randomUUID());
    }

    public Map<String, Object> matchStatus(String status) {
        return Map.of("status", status);
    }

    public List<Map<String, Object>> matchCandidates() {
        return List.of(
                Map.of("id", "user_101", "displayName", "Alex Wu",
                        "avatarUrl", "https://images.barlog.local/avatars/alex.png",
                        "reason", "Also likes bitter cocktails and quiet counters.",
                        "distanceMeters", 700),
                Map.of("id", "user_102", "displayName", "Rin Zhao",
                        "avatarUrl", "https://images.barlog.local/avatars/rin.png",
                        "reason", "Looking for low-pressure wine bar plans tonight.",
                        "distanceMeters", 1250));
    }

    public FrontendItemsResponse<Map<String, Object>> conversations() {
        return chatService.listConversations();
    }

    public FrontendItemsResponse<Map<String, Object>> messages(String conversationId) {
        return chatService.listMessages(conversationId, null, null);
    }

    public Map<String, Object> sendMessage(String conversationId, Map<String, Object> body) {
        String text = body != null ? String.valueOf(body.getOrDefault("body", "")) : "";
        return chatService.sendMessage(conversationId, text);
    }

    public void markRead(String conversationId) {
        chatService.markRead(conversationId);
    }

    private FrontendAuthResponse buildAuthResponse(User user) {
        FrontendAuthResponse resp = new FrontendAuthResponse();
        resp.setUser(mapper.toUser(user, resolvePersonaStatement(user.getId())));
        resp.setAccessToken(compatAuthSupport.issueAccessToken(user));
        resp.setRefreshToken(compatAuthSupport.issueRefreshToken(user));
        return resp;
    }

    private User requireCurrentUser() {
        String userId = checkInAccessHelper.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("User not found", 401, "AUTH_REQUIRED");
        }
        return user;
    }

    private String resolvePersonaStatement(String userId) {
        String previous = UserContext.getUserId();
        try {
            UserContext.setUserId(userId);
            PersonaVO persona = personaService.getMyPersona();
            return persona != null && StringUtils.hasText(persona.getGeneratedText())
                    ? persona.getGeneratedText()
                    : "BarLog explorer";
        } finally {
            if (StringUtils.hasText(previous)) {
                UserContext.setUserId(previous);
            } else {
                UserContext.clear();
            }
        }
    }

    private CreateCheckInRequest toBackendCreate(FrontendCreateCheckInRequest req) {
        CreateCheckInRequest backend = new CreateCheckInRequest();
        backend.setPhotoUrl(req.getPhotoUrl());
        backend.setCardImageUrl(req.getCardImageUrl());
        backend.setDrinkName(req.getDrinkName());
        backend.setDrinkCategory(mapper.toBackendCategory(req.getDrinkCategory()));
        backend.setBarId(req.getBarId());
        backend.setLocationName(req.getBarName());
        backend.setCity(mapper.normalizeCityIn(req.getCity()));
        backend.setArea(req.getArea());
        backend.setMoodTags(req.getMoodTags());
        backend.setVibeMumbling(req.getVibeMumbling());
        backend.setCardStyle(mapper.toBackendCardStyle(req.getCardStyle()));
        backend.setVisibility(mapper.toBackendVisibility(req.getVisibility()));
        backend.setSocialStatus(mapper.toBackendSocialStatus(req.getSocialStatus()));
        if (req.getRating() != null) {
            backend.setRating((int) Math.round(req.getRating() * 2));
        }
        return backend;
    }

    private List<CheckIn> loadMonthCheckIns(String userId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, start.atStartOfDay())
                .lt(CheckIn::getCreatedAt, end.plusDays(1).atStartOfDay()));
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        return YearMonth.parse(month, MONTH);
    }

    private List<FrontendCheckInVO> toCheckInList(List<CheckIn> list) {
        Map<String, Bar> barCache = new HashMap<>();
        return list.stream()
                .map(c -> mapper.toCheckIn(c, userMapper.selectById(c.getUserId()),
                        barCache.computeIfAbsent(
                                c.getBarId() != null ? c.getBarId() : "",
                                id -> StringUtils.hasText(id) ? barMapper.selectById(id) : null)))
                .toList();
    }

    private Bar resolveBar(String barId) {
        return StringUtils.hasText(barId) ? barMapper.selectById(barId) : null;
    }

    private Integer estimateDistance(Bar bar, Double lat, Double lng) {
        if (bar.getLatitude() == null || bar.getLongitude() == null) {
            return ThreadLocalRandom.current().nextInt(300, 1500);
        }
        if (lat != null && lng != null) {
            return (int) GeoUtil.distanceMeters(lat, lng, bar.getLatitude(), bar.getLongitude());
        }
        double refLat = 31.2304;
        double refLng = 121.4737;
        if (bar.getCity() != null && ("Singapore".equalsIgnoreCase(bar.getCity().trim())
                || "新加坡".equals(bar.getCity().trim()))) {
            refLat = 1.29027;
            refLng = 103.851959;
        }
        return (int) GeoUtil.distanceMeters(refLat, refLng, bar.getLatitude(), bar.getLongitude());
    }
}
