package com.alcohol.compat.service;

import com.alcohol.chat.ConversationFactory;
import com.alcohol.common.BizException;
import com.alcohol.compat.CheckInAccessHelper;
import com.alcohol.compat.MediaUrlResolver;
import com.alcohol.compat.vo.MatchCandidateVO;
import com.alcohol.compat.vo.MatchConnectResultVO;
import com.alcohol.compat.vo.MatchPeerVO;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final UserMapper userMapper;
    private final CheckInMapper checkInMapper;
    private final CheckInAccessHelper checkInAccessHelper;
    private final ConversationFactory conversationFactory;
    private final MediaUrlResolver mediaUrlResolver;

    public List<MatchCandidateVO> listCandidates() {
        String viewerId = checkInAccessHelper.requireUserId();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ne(User::getId, viewerId)
                .and(w -> w.isNull(User::getStatus).or().eq(User::getStatus, 1))
                .orderByDesc(User::getUpdatedAt)
                .last("LIMIT 50"));

        List<MatchCandidateVO> items = new ArrayList<>();
        for (User user : users) {
            MatchCandidateVO vo = new MatchCandidateVO();
            vo.setId(user.getId());
            vo.setDisplayName(conversationFactory.displayName(user));
            vo.setAvatarUrl(mediaUrlResolver.resolveAvatarUrl(user.getAvatarUrl(), user.getId()));

            CheckIn latestPublic = findLatestVisibleCheckIn(user.getId());
            CheckIn todayCheckIn = findTodayCheckIn(user.getId(), todayStart);
            vo.setHasTodayCheckIn(todayCheckIn != null);

            if (latestPublic != null && StringUtils.hasText(latestPublic.getVibeMumbling())) {
                vo.setReason(latestPublic.getVibeMumbling());
            } else if (StringUtils.hasText(user.getBio())) {
                vo.setReason(user.getBio());
            } else if (todayCheckIn != null && StringUtils.hasText(todayCheckIn.getDrinkName())) {
                vo.setReason("Checked in with " + todayCheckIn.getDrinkName() + " tonight.");
            } else {
                vo.setReason("Open to a low-pressure bar chat tonight.");
            }

            items.add(vo);
        }
        return items;
    }

    @Transactional
    public MatchConnectResultVO connect(String targetUserId) {
        if (!StringUtils.hasText(targetUserId)) {
            throw new BizException("userId is required", 400);
        }
        String viewerId = checkInAccessHelper.requireUserId();
        if (viewerId.equals(targetUserId)) {
            throw new BizException("Cannot chat with yourself", 400);
        }

        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException("User not found", 404);
        }

        String conversationId = conversationFactory.getOrCreateDirectConversation(
                viewerId, targetUserId, "match", conversationFactory.displayName(target));

        MatchPeerVO peer = new MatchPeerVO();
        peer.setId(target.getId());
        peer.setDisplayName(conversationFactory.displayName(target));
        peer.setAvatarUrl(mediaUrlResolver.resolveAvatarUrl(target.getAvatarUrl(), target.getId()));

        MatchConnectResultVO result = new MatchConnectResultVO();
        result.setConversationId(conversationId);
        result.setStatus("ready");
        result.setPeer(peer);
        return result;
    }

    private CheckIn findLatestVisibleCheckIn(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return checkInMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .in(CheckIn::getVisibility, "PUBLIC", "TONIGHT_ONLY")
                .gt(CheckIn::getExpiresAt, now)
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 1"));
    }

    private CheckIn findTodayCheckIn(String userId, LocalDateTime todayStart) {
        return checkInMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, todayStart)
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 1"));
    }
}
