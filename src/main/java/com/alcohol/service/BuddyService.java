package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.context.UserContext;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import com.alcohol.vo.buddy.BuddyMatchVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 摇一摇找搭子 — 优先匹配已开启 Tonight 的用户。
 */
@Service
@RequiredArgsConstructor
public class BuddyService {

    private static final List<String> ICEBREAKERS = List.of(
            "你也喜欢这家的氛围？要不要一起碰一杯？",
            "看到你也在附近，今晚想喝点什么风格的？",
            "同城酒友你好！最近有发现什么好酒吧吗？",
            "摇到你了——今晚的推荐酒款是什么？"
    );

    private final UserMapper userMapper;
    private final CheckInMapper checkInMapper;

    public BuddyMatchVO shake() {
        String userId = UserContext.getUserId();

        List<User> candidates = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getTonightEnabled, 1)
                .ne(User::getId, userId)
                .last("LIMIT 20"));

        if (candidates.isEmpty()) {
            candidates = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .ne(User::getId, userId)
                    .last("LIMIT 20"));
        }
        if (candidates.isEmpty()) {
            throw new BizException("暂时没有可匹配的酒友，稍后再试");
        }

        User match = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        CheckIn recent = checkInMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, match.getId())
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 1"));

        BuddyMatchVO vo = new BuddyMatchVO();
        vo.setUserId(match.getId());
        vo.setNickname(match.getNickname());
        vo.setAvatarEmoji(StringUtils.hasText(match.getAvatarEmoji()) ? match.getAvatarEmoji() : "🍸");
        if (recent != null) {
            vo.setDrinkName(recent.getDrinkName());
            vo.setVibeMumbling(StringUtils.hasText(recent.getDiaryText()) ? recent.getDiaryText() : recent.getVibeMumbling());
        }
        vo.setIcebreaker(ICEBREAKERS.get(ThreadLocalRandom.current().nextInt(ICEBREAKERS.size())));
        vo.setReason("你们都开启了 Tonight，且口味偏好相近");
        return vo;
    }
}
