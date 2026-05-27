package com.alcohol.community;

import com.alcohol.chat.ConversationFactory;
import com.alcohol.common.BizException;
import com.alcohol.community.vo.*;
import com.alcohol.compat.vo.FrontendItemsResponse;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.CheckInComment;
import com.alcohol.entity.CheckInReaction;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInCommentMapper;
import com.alcohol.mapper.CheckInReactionMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityInteractionService {

    private static final Set<String> WAVEABLE = Set.of("CHAT_OK", "OPEN_TO_CHAT", "FIND_BUDDY", "LOOKING_FOR_BUDDY");

    private final CommunityAccessHelper accessHelper;
    private final CheckInReactionMapper reactionMapper;
    private final CheckInCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final ConversationFactory conversationFactory;

    @Transactional
    public CommunityLikeResultVO toggleLike(String checkInId) {
        accessHelper.requireVisiblePost(checkInId);
        String userId = accessHelper.requireUserId();

        CheckInReaction existing = reactionMapper.selectOne(new LambdaQueryWrapper<CheckInReaction>()
                .eq(CheckInReaction::getCheckInId, checkInId)
                .eq(CheckInReaction::getUserId, userId));

        boolean liked;
        if (existing != null) {
            reactionMapper.deleteById(existing.getId());
            liked = false;
        } else {
            CheckInReaction reaction = new CheckInReaction();
            reaction.setCheckInId(checkInId);
            reaction.setUserId(userId);
            reaction.setCreatedAt(LocalDateTime.now());
            reactionMapper.insert(reaction);
            liked = true;
        }

        Long count = reactionMapper.selectCount(new LambdaQueryWrapper<CheckInReaction>()
                .eq(CheckInReaction::getCheckInId, checkInId));

        CommunityLikeResultVO result = new CommunityLikeResultVO();
        result.setLiked(liked);
        result.setLikedCount(count != null ? count.intValue() : 0);
        return result;
    }

    public FrontendItemsResponse<CommunityCommentVO> listComments(String checkInId) {
        accessHelper.requireVisiblePost(checkInId);
        List<CheckInComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CheckInComment>()
                .eq(CheckInComment::getCheckInId, checkInId)
                .orderByAsc(CheckInComment::getCreatedAt));

        List<CommunityCommentVO> items = comments.stream().map(this::toComment).collect(Collectors.toList());
        return FrontendItemsResponse.of(items);
    }

    @Transactional
    public CommunityCommentVO addComment(String checkInId, String body) {
        accessHelper.requireVisiblePost(checkInId);
        if (!StringUtils.hasText(body) || body.trim().length() > 500) {
            throw new BizException("Comment body must be 1-500 characters", 400);
        }
        String userId = accessHelper.requireUserId();

        CheckInComment comment = new CheckInComment();
        comment.setCheckInId(checkInId);
        comment.setUserId(userId);
        comment.setBody(body.trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return toComment(comment);
    }

    @Transactional
    public CommunityWaveResultVO wave(String targetUserId, String checkInId) {
        if (!StringUtils.hasText(targetUserId)) {
            throw new BizException("targetUserId is required", 400);
        }
        String userId = accessHelper.requireUserId();
        if (userId.equals(targetUserId)) {
            throw new BizException("Cannot wave at yourself", 400);
        }

        CheckIn targetPost = resolveWaveTargetPost(targetUserId, checkInId);
        assertWaveable(targetPost);

        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException("User not found", 404);
        }

        String conversationId = conversationFactory.getOrCreateDirectConversation(
                userId, targetUserId, "bar_wave", displayName(target));

        CommunityWaveResultVO vo = new CommunityWaveResultVO();
        vo.setConversationId(conversationId);
        vo.setStatus("ready");
        return vo;
    }

    private CheckIn resolveWaveTargetPost(String targetUserId, String checkInId) {
        if (StringUtils.hasText(checkInId)) {
            CheckIn checkIn = accessHelper.requireVisiblePost(checkInId);
            if (!targetUserId.equals(checkIn.getUserId())) {
                throw new BizException("Post does not belong to target user", 400);
            }
            return checkIn;
        }
        List<CheckIn> open = accessHelper.latestOpenCheckInsForUser(targetUserId, 1);
        if (open.isEmpty()) {
            throw new BizException("User has no open check-in to wave at", 404);
        }
        return open.get(0);
    }

    private void assertWaveable(CheckIn checkIn) {
        String status = checkIn.getSocialStatus();
        if (!StringUtils.hasText(status)) {
            throw new BizException("User is not open to chat right now", 403);
        }
        if (!WAVEABLE.contains(status.trim().toUpperCase(Locale.ROOT))) {
            throw new BizException("User is not open to chat right now", 403);
        }
    }

    private CommunityCommentVO toComment(CheckInComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        CommunityCommentVO vo = new CommunityCommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setAuthorName(displayName(user));
        vo.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
        vo.setBody(comment.getBody());
        vo.setCreatedAt(comment.getCreatedAt() == null ? null
                : comment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString());
        return vo;
    }

    private String displayName(User user) {
        if (user == null) {
            return "BarLog";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "BarLog";
    }
}
