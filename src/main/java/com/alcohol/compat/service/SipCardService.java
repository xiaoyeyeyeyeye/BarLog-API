package com.alcohol.compat.service;

import com.alcohol.common.BizException;
import com.alcohol.compat.CheckInAccessHelper;
import com.alcohol.compat.MediaUrlResolver;
import com.alcohol.compat.FrontendMapper;
import com.alcohol.compat.vo.FrontendSipCardAuthorVO;
import com.alcohol.compat.vo.FrontendSipCardDetailVO;
import com.alcohol.entity.Bar;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.CheckInComment;
import com.alcohol.entity.CheckInReaction;
import com.alcohol.entity.User;
import com.alcohol.mapper.BarMapper;
import com.alcohol.mapper.CheckInCommentMapper;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.CheckInReactionMapper;
import com.alcohol.mapper.UserMapper;
import com.alcohol.util.JsonUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SipCardService {

    private final CheckInMapper checkInMapper;
    private final UserMapper userMapper;
    private final BarMapper barMapper;
    private final CheckInReactionMapper reactionMapper;
    private final CheckInCommentMapper commentMapper;
    private final CheckInAccessHelper checkInAccessHelper;
    private final FrontendMapper mapper;
    private final MediaUrlResolver mediaUrlResolver;

    public FrontendSipCardDetailVO getDetail(String checkInId) {
        CheckIn checkIn = checkInMapper.selectById(checkInId);
        if (checkIn == null) {
            throw new BizException("Check-in not found", 404);
        }
        checkInAccessHelper.assertReadable(checkIn);

        String viewerId = checkInAccessHelper.requireUserId();
        User authorUser = userMapper.selectById(checkIn.getUserId());
        Bar bar = StringUtils.hasText(checkIn.getBarId()) ? barMapper.selectById(checkIn.getBarId()) : null;

        FrontendSipCardDetailVO vo = new FrontendSipCardDetailVO();
        vo.setId(checkIn.getId());
        vo.setUserId(checkIn.getUserId());
        vo.setAuthor(toAuthor(authorUser));
        vo.setPhotoUrl(mediaUrlResolver.resolveCheckInImage(checkIn.getPhotoUrl(), checkIn.getId()));
        vo.setCardImageUrl(mediaUrlResolver.resolveCheckInImage(resolveCardImageUrl(checkIn), checkIn.getId()));
        vo.setDrinkName(checkIn.getDrinkName());
        vo.setDrinkCategory(mapper.toFrontendCategory(checkIn.getDrinkCategory()));
        vo.setBarId(checkIn.getBarId());
        vo.setBarName(bar != null && StringUtils.hasText(bar.getName()) ? bar.getName() : checkIn.getLocationName());
        vo.setCity(mapper.normalizeCityOut(checkIn.getCity()));
        vo.setArea(checkIn.getArea());
        List<String> moods = JsonUtil.parseStringList(checkIn.getMoodTags());
        vo.setMoodTags(moods != null ? moods : Collections.emptyList());
        vo.setRating(toFrontendRating(checkIn.getRating()));
        vo.setVibeMumbling(checkIn.getVibeMumbling());
        vo.setCardStyle(toFrontendCardStyle(checkIn.getCardStyle()));
        vo.setVisibility(toFrontendVisibility(checkIn.getVisibility()));
        vo.setSocialStatus(toFrontendSocialStatus(checkIn.getSocialStatus()));
        vo.setCreatedAt(formatInstant(checkIn.getCreatedAt()));
        vo.setExpiresAt(formatInstant(checkIn.getExpiresAt()));
        vo.setLikedCount(countLikes(checkIn.getId()));
        vo.setCommentCount(countComments(checkIn.getId()));
        vo.setLikedByMe(hasLiked(checkIn.getId(), viewerId));
        vo.setOwner(viewerId.equals(checkIn.getUserId()));
        return vo;
    }

    private FrontendSipCardAuthorVO toAuthor(User user) {
        FrontendSipCardAuthorVO author = new FrontendSipCardAuthorVO();
        if (user == null) {
            author.setId("");
            author.setDisplayName("BarLog");
            return author;
        }
        author.setId(user.getId());
        author.setDisplayName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "BarLog");
        author.setAvatarUrl(mediaUrlResolver.resolveAvatarUrl(user.getAvatarUrl(), user.getId()));
        return author;
    }

    private String resolveCardImageUrl(CheckIn checkIn) {
        if (StringUtils.hasText(checkIn.getCardImageUrl())) {
            return checkIn.getCardImageUrl();
        }
        return checkIn.getPhotoUrl();
    }

    private int countLikes(String checkInId) {
        Long count = reactionMapper.selectCount(new LambdaQueryWrapper<CheckInReaction>()
                .eq(CheckInReaction::getCheckInId, checkInId));
        return count != null ? count.intValue() : 0;
    }

    private int countComments(String checkInId) {
        Long count = commentMapper.selectCount(new LambdaQueryWrapper<CheckInComment>()
                .eq(CheckInComment::getCheckInId, checkInId));
        return count != null ? count.intValue() : 0;
    }

    private boolean hasLiked(String checkInId, String viewerId) {
        Long count = reactionMapper.selectCount(new LambdaQueryWrapper<CheckInReaction>()
                .eq(CheckInReaction::getCheckInId, checkInId)
                .eq(CheckInReaction::getUserId, viewerId));
        return count != null && count > 0;
    }

    private Double toFrontendRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        return Math.round(rating / 2.0 * 10.0) / 10.0;
    }

    private String toFrontendCardStyle(String value) {
        if (!StringUtils.hasText(value)) {
            return "receipt";
        }
        return switch (value.trim().toUpperCase()) {
            case "WATERCOLOR" -> "watercolor";
            case "FILM_TICKET" -> "film_ticket";
            case "DOODLE_GLOW" -> "doodle_glow";
            case "PASSPORT_STAMP" -> "passport_stamp";
            default -> "receipt";
        };
    }

    private String toFrontendVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return "private";
        }
        return switch (value.trim().toUpperCase()) {
            case "PUBLIC" -> "public";
            case "TONIGHT_ONLY" -> "tonight_only";
            default -> "private";
        };
    }

    private String toFrontendSocialStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "not_social";
        }
        return switch (value.trim().toUpperCase()) {
            case "OPEN_TO_CHAT", "CHAT_OK" -> "open_to_chat";
            case "LOOKING_FOR_BUDDY", "FIND_BUDDY" -> "looking_for_buddy";
            case "FRIENDS_ONLY", "VIEW_ONLY" -> "friends_only";
            default -> "not_social";
        };
    }

    private String formatInstant(java.time.LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }
}
