package com.alcohol.chat;

import com.alcohol.common.BizException;
import com.alcohol.compat.vo.FrontendItemsResponse;
import com.alcohol.context.UserContext;
import com.alcohol.entity.ChatMessage;
import com.alcohol.entity.Conversation;
import com.alcohol.entity.ConversationMember;
import com.alcohol.entity.User;
import com.alcohol.mapper.ChatMessageMapper;
import com.alcohol.mapper.ConversationMapper;
import com.alcohol.mapper.ConversationMemberMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final DateTimeFormatter CURSOR_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ConversationFactory conversationFactory;
    private final ChatMessagePublisher messagePublisher;

    public FrontendItemsResponse<Map<String, Object>> listConversations() {
        String userId = requireUserId();
        List<ConversationMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getUserId, userId)
                .orderByDesc(ConversationMember::getJoinedAt));

        List<Map<String, Object>> items = new ArrayList<>();
        for (ConversationMember membership : memberships) {
            Conversation conversation = conversationMapper.selectById(membership.getConversationId());
            if (conversation == null) {
                continue;
            }
            items.add(toConversationMap(conversation, membership, userId));
        }
        items.sort(Comparator.comparing(
                (Map<String, Object> m) -> String.valueOf(m.get("updatedAt")),
                Comparator.reverseOrder()));
        return FrontendItemsResponse.of(items);
    }

    public FrontendItemsResponse<Map<String, Object>> listMessages(String conversationId, String cursor, Integer limit) {
        String userId = requireUserId();
        assertMember(conversationId, userId);

        int pageSize = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + (pageSize + 1));

        applyCursor(qw, cursor);
        List<ChatMessage> rows = messageMapper.selectList(qw);

        String nextCursor = null;
        if (rows.size() > pageSize) {
            ChatMessage last = rows.get(pageSize - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, pageSize);
        }

        Collections.reverse(rows);
        List<Map<String, Object>> items = rows.stream().map(this::toMessageMap).collect(Collectors.toList());
        FrontendItemsResponse<Map<String, Object>> response = FrontendItemsResponse.of(items);
        response.setNextCursor(nextCursor);
        return response;
    }

    @Transactional
    public Map<String, Object> sendMessage(String conversationId, String body) {
        String userId = requireUserId();
        assertMember(conversationId, userId);
        if (!StringUtils.hasText(body) || body.trim().length() > 2000) {
            throw new BizException("Message body must be 1-2000 characters", 400);
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setBody(body.trim());
        message.setContentType("text");
        message.setCreatedAt(now);
        messageMapper.insert(message);

        Conversation conversation = conversationMapper.selectById(conversationId);
        conversation.setLastMessagePreview(body.trim());
        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);

        List<ConversationMember> members = memberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId));
        for (ConversationMember member : members) {
            if (!userId.equals(member.getUserId())) {
                member.setUnreadCount(member.getUnreadCount() == null ? 1 : member.getUnreadCount() + 1);
                memberMapper.updateById(member);
            }
        }

        Map<String, Object> payload = toMessageMap(message);
        messagePublisher.publishNewMessage(conversationId, members, payload);
        return payload;
    }

    @Transactional
    public void markRead(String conversationId) {
        String userId = requireUserId();
        ConversationMember membership = assertMember(conversationId, userId);
        membership.setUnreadCount(0);
        membership.setLastReadAt(LocalDateTime.now());
        memberMapper.updateById(membership);
        messagePublisher.publishRead(conversationId, userId);
    }

    private Map<String, Object> toConversationMap(Conversation conversation, ConversationMember membership, String userId) {
        String title = conversation.getTitle();
        if (!StringUtils.hasText(title)) {
            title = resolvePeerName(conversation.getId(), userId);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conversation.getId());
        map.put("title", title);
        map.put("lastMessage", conversation.getLastMessagePreview());
        map.put("unreadCount", membership.getUnreadCount() == null ? 0 : membership.getUnreadCount());
        map.put("updatedAt", formatInstant(conversation.getUpdatedAt()));
        return map;
    }

    private String resolvePeerName(String conversationId, String userId) {
        List<ConversationMember> members = memberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId));
        for (ConversationMember member : members) {
            if (!userId.equals(member.getUserId())) {
                User user = userMapper.selectById(member.getUserId());
                return conversationFactory.displayName(user);
            }
        }
        return "Chat";
    }

    private Map<String, Object> toMessageMap(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("conversationId", message.getConversationId());
        map.put("senderId", message.getSenderId());
        map.put("body", message.getBody());
        map.put("createdAt", formatInstant(message.getCreatedAt()));
        return map;
    }

    private ConversationMember assertMember(String conversationId, String userId) {
        ConversationMember membership = memberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId));
        if (membership == null) {
            throw new BizException("Conversation not found", 404);
        }
        return membership;
    }

    private String requireUserId() {
        String userId = UserContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BizException("Missing bearer token", 401, "AUTH_REQUIRED");
        }
        return userId;
    }

    private String formatInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private void applyCursor(LambdaQueryWrapper<ChatMessage> qw, String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return;
        }
        Cursor decoded = decodeCursor(cursor);
        qw.and(w -> w.lt(ChatMessage::getCreatedAt, decoded.createdAt())
                .or(o -> o.eq(ChatMessage::getCreatedAt, decoded.createdAt()).lt(ChatMessage::getId, decoded.id())));
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

    private record Cursor(LocalDateTime createdAt, String id) {
    }
}
