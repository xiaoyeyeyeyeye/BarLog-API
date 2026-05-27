package com.alcohol.chat;

import com.alcohol.entity.Conversation;
import com.alcohol.entity.ConversationMember;
import com.alcohol.entity.User;
import com.alcohol.mapper.ConversationMapper;
import com.alcohol.mapper.ConversationMemberMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationFactory {

    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper memberMapper;
    private final UserMapper userMapper;

    @Transactional
    public String getOrCreateDirectConversation(String userA, String userB, String type, String title) {
        List<ConversationMember> membershipsA = memberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getUserId, userA));
        for (ConversationMember membership : membershipsA) {
            Conversation conversation = conversationMapper.selectById(membership.getConversationId());
            if (conversation == null || !isDirectType(conversation.getType())) {
                continue;
            }
            ConversationMember other = memberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                    .eq(ConversationMember::getConversationId, conversation.getId())
                    .eq(ConversationMember::getUserId, userB));
            if (other != null) {
                return conversation.getId();
            }
        }
        return createConversation(userA, userB, type, title);
    }

    @Transactional
    public String createConversation(String userA, String userB, String type, String title) {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setType(StringUtils.hasText(type) ? type : "direct");
        conversation.setTitle(title);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);

        addMember(conversation.getId(), userA, now);
        addMember(conversation.getId(), userB, now);
        return conversation.getId();
    }

    private void addMember(String conversationId, String userId, LocalDateTime now) {
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setUnreadCount(0);
        member.setJoinedAt(now);
        memberMapper.insert(member);
    }

    private boolean isDirectType(String type) {
        if (!StringUtils.hasText(type)) {
            return true;
        }
        return "direct".equalsIgnoreCase(type) || "bar_wave".equalsIgnoreCase(type) || "match".equalsIgnoreCase(type);
    }

    public String displayName(User user) {
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
