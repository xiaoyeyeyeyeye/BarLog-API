package com.alcohol.service;

import com.alcohol.context.UserContext;
import com.alcohol.entity.Badge;
import com.alcohol.entity.UserBadge;
import com.alcohol.mapper.BadgeMapper;
import com.alcohol.mapper.UserBadgeMapper;
import com.alcohol.vo.badge.BadgeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;

    public List<BadgeVO> listAll() {
        return badgeMapper.selectList(new LambdaQueryWrapper<Badge>().orderByAsc(Badge::getName))
                .stream().map(b -> toVO(b, false, null)).toList();
    }

    public List<BadgeVO> listMine() {
        String userId = UserContext.getUserId();
        List<Badge> all = badgeMapper.selectList(null);
        Map<String, UserBadge> unlocked = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getUserId, userId)
        ).stream().collect(Collectors.toMap(UserBadge::getBadgeId, ub -> ub));

        return all.stream().map(b -> {
            UserBadge ub = unlocked.get(b.getId());
            return toVO(b, ub != null, ub != null ? ub.getUnlockedAt() : null);
        }).toList();
    }

    private BadgeVO toVO(Badge b, boolean unlocked, java.time.LocalDateTime unlockedAt) {
        BadgeVO vo = new BadgeVO();
        vo.setId(b.getId());
        vo.setCode(b.getCode());
        vo.setName(b.getName());
        vo.setDescription(b.getDescription());
        vo.setIconUrl(b.getIconUrl());
        vo.setUnlocked(unlocked);
        vo.setUnlockedAt(unlockedAt);
        return vo;
    }
}
