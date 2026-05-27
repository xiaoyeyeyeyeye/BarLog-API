package com.alcohol.chat;

import com.alcohol.entity.ConversationMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final ChatSessionRegistry sessionRegistry;
    private final ChatWebSocketHandler webSocketHandler;

    public void publishNewMessage(String conversationId, List<ConversationMember> members, Map<String, Object> message) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "message.new");
        envelope.put("conversationId", conversationId);
        envelope.put("message", message);
        pushToMembers(members, envelope);
    }

    public void publishRead(String conversationId, String readerId) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "message.read");
        envelope.put("conversationId", conversationId);
        envelope.put("readerId", readerId);
        sessionRegistry.sessionsForUser(readerId).forEach(session -> webSocketHandler.sendJson(session, envelope));
    }

    private void pushToMembers(List<ConversationMember> members, Map<String, Object> envelope) {
        for (ConversationMember member : members) {
            sessionRegistry.sessionsForUser(member.getUserId())
                    .forEach(session -> webSocketHandler.sendJson(session, envelope));
        }
    }
}
