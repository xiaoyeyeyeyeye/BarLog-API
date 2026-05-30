package com.alcohol.community;

import com.alcohol.common.BizException;
import com.alcohol.compat.MediaUrlResolver;
import com.alcohol.community.vo.CommunityEligibilityVO;
import com.alcohol.community.vo.CommunityPostVO;
import com.alcohol.compat.FrontendMapper;
import com.alcohol.compat.vo.FrontendGalleryPostVO;
import com.alcohol.compat.vo.FrontendItemsResponse;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.CheckInComment;
import com.alcohol.entity.CheckInReaction;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInCommentMapper;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.CheckInReactionMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityFeedService {

    private static final DateTimeFormatter CURSOR_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CommunityAccessHelper accessHelper;
    private final MediaUrlResolver mediaUrlResolver;
    private final CheckInMapper checkInMapper;
    private final CheckInReactionMapper reactionMapper;
    private final CheckInCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final FrontendMapper frontendMapper;

    @Value("${alcohol.community.max-feed-limit:50}")
    private int maxFeedLimit;

    public CommunityEligibilityVO eligibility(String city, String barId) {
        String userId = accessHelper.requireUserId();
        boolean unlocked = accessHelper.hasTodayCheckIn(userId);
        CommunityEligibilityVO vo = new CommunityEligibilityVO();
        vo.setCanViewCommunity(unlocked);
        vo.setCanViewCityFeed(unlocked);
        vo.setCanViewBarFeed(unlocked);

        CheckIn today = accessHelper.findLatestTodayCheckIn(userId);
        if (today != null) {
            vo.setTodayCheckInId(today.getId());
            vo.setTodayBarId(today.getBarId());
            vo.setTodayCity(frontendMapper.normalizeCityOut(today.getCity()));
        }
        return vo;
    }

    public FrontendItemsResponse<FrontendGalleryPostVO> galleryFeed(String city, String range) {
        accessHelper.assertCommunityUnlocked();
        return toGalleryResponse(feedPosts("global", null, null, range, null, 30, false));
    }

    public FrontendItemsResponse<CommunityPostVO> communityFeed(String scope, String city, String barId,
                                                               String range, String cursor, Integer limit) {
        accessHelper.assertCommunityUnlocked();
        int pageSize = normalizeLimit(limit);
        FeedPage page = feedPosts("global", null, null, range, cursor, pageSize, true);
        FrontendItemsResponse<CommunityPostVO> response = new FrontendItemsResponse<>();
        response.setItems(page.posts());
        response.setNextCursor(page.nextCursor());
        return response;
    }

    private FeedPage feedPosts(String scope, String city, String barId, String range, String cursor,
                               int limit, boolean includeInteractionFields) {
        LocalDateTime now = LocalDateTime.now();
        int hours = accessHelper.resolveRangeHours(range);
        LocalDateTime since = now.minusHours(hours);

        LambdaQueryWrapper<CheckIn> qw = new LambdaQueryWrapper<CheckIn>()
                .in(CheckIn::getVisibility, "PUBLIC", "TONIGHT_ONLY")
                .gt(CheckIn::getExpiresAt, now)
                .ge(CheckIn::getCreatedAt, since)
                .orderByDesc(CheckIn::getCreatedAt)
                .orderByDesc(CheckIn::getId)
                .last("LIMIT " + (limit + 1));

        if ("bar".equalsIgnoreCase(scope)) {
            if (!StringUtils.hasText(barId)) {
                throw new BizException("barId is required for bar scope", 400);
            }
            qw.eq(CheckIn::getBarId, barId);
        } else if (!"global".equalsIgnoreCase(scope) && StringUtils.hasText(city)) {
            String normalized = frontendMapper.normalizeCityIn(city);
            qw.and(w -> w.eq(CheckIn::getCity, normalized).or().eq(CheckIn::getCity, city.trim()));
        }

        applyCursor(qw, cursor);

        List<CheckIn> rows = checkInMapper.selectList(qw);
        String nextCursor = null;
        if (rows.size() > limit) {
            CheckIn last = rows.get(limit - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, limit);
        }

        String viewerId = includeInteractionFields ? accessHelper.requireUserId() : null;
        Map<String, Long> likeCounts = includeInteractionFields ? loadLikeCounts(rows) : Map.of();
        Map<String, Long> commentCounts = includeInteractionFields ? loadCommentCounts(rows) : Map.of();
        Set<String> likedByMe = includeInteractionFields ? loadLikedByMe(rows, viewerId) : Set.of();

        List<CommunityPostVO> posts = rows.stream()
                .map(checkIn -> toPost(checkIn, likeCounts, commentCounts, likedByMe, includeInteractionFields))
                .collect(Collectors.toList());
        return new FeedPage(posts, nextCursor);
    }

    private CommunityPostVO toPost(CheckIn checkIn, Map<String, Long> likeCounts,
                                   Map<String, Long> commentCounts, Set<String> likedByMe,
                                   boolean includeInteractionFields) {
        User user = userMapper.selectById(checkIn.getUserId());
        CommunityPostVO post = new CommunityPostVO();
        post.setId(checkIn.getId());
        post.setUserId(checkIn.getUserId());
        post.setAuthorName(user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname() : "BarLog");
        post.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
        post.setImageUrl(mediaUrlResolver.resolveCheckInImage(
                StringUtils.hasText(checkIn.getCardImageUrl()) ? checkIn.getCardImageUrl() : checkIn.getPhotoUrl(),
                checkIn.getId()));
        post.setCaption(checkIn.getVibeMumbling());
        post.setCity(frontendMapper.normalizeCityOut(checkIn.getCity()));
        post.setBarId(checkIn.getBarId());
        post.setBarName(checkIn.getLocationName());
        post.setSocialStatus(toFrontendSocialStatus(checkIn.getSocialStatus()));
        post.setVisibility(toFrontendVisibility(checkIn.getVisibility()));
        post.setExpiresAt(formatInstant(checkIn.getExpiresAt()));
        post.setCreatedAt(formatInstant(checkIn.getCreatedAt()));
        if (includeInteractionFields) {
            post.setLikedCount(likeCounts.getOrDefault(checkIn.getId(), 0L).intValue());
            post.setCommentCount(commentCounts.getOrDefault(checkIn.getId(), 0L).intValue());
            post.setLikedByMe(likedByMe.contains(checkIn.getId()));
        }
        return post;
    }

    private FrontendItemsResponse<FrontendGalleryPostVO> toGalleryResponse(FeedPage page) {
        List<FrontendGalleryPostVO> items = page.posts().stream().map(post -> {
            FrontendGalleryPostVO vo = new FrontendGalleryPostVO();
            vo.setId(post.getId());
            vo.setUserId(post.getUserId());
            vo.setAuthorName(post.getAuthorName());
            vo.setImageUrl(post.getImageUrl());
            vo.setCaption(post.getCaption());
            vo.setLikedCount(post.getLikedCount());
            vo.setCreatedAt(post.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        FrontendItemsResponse<FrontendGalleryPostVO> response = FrontendItemsResponse.of(items);
        response.setNextCursor(page.nextCursor());
        return response;
    }

    private Map<String, Long> loadLikeCounts(List<CheckIn> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<String> ids = rows.stream().map(CheckIn::getId).toList();
        return reactionMapper.selectList(new LambdaQueryWrapper<CheckInReaction>()
                        .in(CheckInReaction::getCheckInId, ids))
                .stream()
                .collect(Collectors.groupingBy(CheckInReaction::getCheckInId, Collectors.counting()));
    }

    private Map<String, Long> loadCommentCounts(List<CheckIn> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<String> ids = rows.stream().map(CheckIn::getId).toList();
        return commentMapper.selectList(new LambdaQueryWrapper<CheckInComment>()
                        .in(CheckInComment::getCheckInId, ids))
                .stream()
                .collect(Collectors.groupingBy(CheckInComment::getCheckInId, Collectors.counting()));
    }

    private Set<String> loadLikedByMe(List<CheckIn> rows, String viewerId) {
        if (rows.isEmpty()) {
            return Set.of();
        }
        List<String> ids = rows.stream().map(CheckIn::getId).toList();
        return reactionMapper.selectList(new LambdaQueryWrapper<CheckInReaction>()
                        .in(CheckInReaction::getCheckInId, ids)
                        .eq(CheckInReaction::getUserId, viewerId))
                .stream()
                .map(CheckInReaction::getCheckInId)
                .collect(Collectors.toSet());
    }

    private void applyCursor(LambdaQueryWrapper<CheckIn> qw, String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return;
        }
        Cursor decoded = decodeCursor(cursor);
        qw.and(w -> w.lt(CheckIn::getCreatedAt, decoded.createdAt())
                .or(o -> o.eq(CheckIn::getCreatedAt, decoded.createdAt()).lt(CheckIn::getId, decoded.id())));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, maxFeedLimit);
    }

    private String encodeCursor(LocalDateTime createdAt, String id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((createdAt.format(CURSOR_TIME) + "|" + id).getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.indexOf('|');
            if (idx <= 0) {
                throw new IllegalArgumentException("bad cursor");
            }
            return new Cursor(LocalDateTime.parse(raw.substring(0, idx), CURSOR_TIME), raw.substring(idx + 1));
        } catch (Exception e) {
            throw new BizException("Invalid cursor", 400);
        }
    }

    private String formatInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toString();
    }

    private String toFrontendVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return "private";
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> "public";
            case "TONIGHT_ONLY" -> "tonight_only";
            default -> "private";
        };
    }

    private String toFrontendSocialStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "not_social";
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "CHAT_OK", "OPEN_TO_CHAT" -> "open_to_chat";
            case "FIND_BUDDY", "LOOKING_FOR_BUDDY" -> "looking_for_buddy";
            case "VIEW_ONLY", "FRIENDS_ONLY" -> "friends_only";
            default -> "not_social";
        };
    }

    private record FeedPage(List<CommunityPostVO> posts, String nextCursor) {
    }

    private record Cursor(LocalDateTime createdAt, String id) {
    }
}
