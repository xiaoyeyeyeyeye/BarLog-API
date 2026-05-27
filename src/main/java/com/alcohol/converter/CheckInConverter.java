package com.alcohol.converter;

import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.util.JsonUtil;
import com.alcohol.util.TimeLabelUtil;
import com.alcohol.vo.checkin.CheckInVO;
import org.springframework.stereotype.Component;

/**
 * 打卡实体与 API 视图对象之间的转换。
 * <p>仅做字段映射，不访问数据库。</p>
 */
@Component
public class CheckInConverter {

    /** 完整打卡详情（含 P1 扩展字段与相对时间文案） */
    public CheckInVO toVO(CheckIn entity, User user) {
        if (entity == null) {
            return null;
        }
        CheckInVO vo = new CheckInVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatarUrl(user.getAvatarUrl());
        }
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setDrinkName(entity.getDrinkName());
        vo.setDrinkId(entity.getDrinkId());
        vo.setDrinkCategory(entity.getDrinkCategory());
        vo.setBarId(entity.getBarId());
        vo.setLocationName(entity.getLocationName());
        vo.setCity(entity.getCity());
        vo.setArea(entity.getArea());
        vo.setMoodTags(JsonUtil.parseStringList(entity.getMoodTags()));
        vo.setFlavorTags(JsonUtil.parseStringList(entity.getFlavorTags()));
        vo.setVibeMumbling(entity.getVibeMumbling());
        vo.setDiaryText(entity.getDiaryText());
        vo.setRating(entity.getRating());
        vo.setVoiceNoteUrl(entity.getVoiceNoteUrl());
        vo.setAiCardQuote(entity.getAiCardQuote());
        vo.setAiCardQuoteSource(entity.getAiCardQuoteSource());
        vo.setCardStyle(entity.getCardStyle());
        vo.setCardImageUrl(entity.getCardImageUrl());
        vo.setVisibility(entity.getVisibility());
        vo.setSocialStatus(entity.getSocialStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setTimeLabel(TimeLabelUtil.relativeLabel(entity.getCreatedAt()));
        return vo;
    }

    /** 日记列表精简视图（仅展示列表所需字段） */
    public CheckInVO toRecentVO(CheckIn entity, User user) {
        CheckInVO vo = new CheckInVO();
        vo.setId(entity.getId());
        vo.setDrinkName(entity.getDrinkName());
        vo.setLocationName(entity.getLocationName());
        vo.setFlavorTags(JsonUtil.parseStringList(entity.getFlavorTags()));
        vo.setRating(entity.getRating());
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setCardImageUrl(entity.getCardImageUrl());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setTimeLabel(TimeLabelUtil.relativeLabel(entity.getCreatedAt()));
        if (user != null) {
            vo.setUserNickname(user.getNickname());
        }
        return vo;
    }
}
