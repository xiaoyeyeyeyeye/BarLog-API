package com.alcohol.compat.controller;

import com.alcohol.compat.service.FrontendCompatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FrontendMiscController {

    private final FrontendCompatService compatService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "alcohol-api-frontend-compat");
    }

    @PostMapping("/api/uploads/image")
    public Map<String, Object> uploadImage() {
        return compatService.uploadImageStub();
    }

    @GetMapping("/api/drinks/collection")
    public List<Map<String, Object>> drinkCollection() {
        return compatService.drinkCollection();
    }

    @GetMapping("/api/drinks/{drinkId}")
    public Map<String, Object> drinkDetail(@PathVariable String drinkId) {
        return compatService.drinkDetail(drinkId);
    }

    @GetMapping("/api/persona/current")
    public Map<String, Object> personaCurrent() {
        return compatService.personaCurrent();
    }

    @PostMapping("/api/ai/recognize-drink")
    public Map<String, Object> recognizeDrink(@RequestBody(required = false) Map<String, Object> body) {
        return compatService.aiRecognizeDrink();
    }

    @PostMapping("/api/ai/generate-card-copy")
    public Map<String, Object> generateCardCopy(@RequestBody(required = false) Map<String, Object> body) {
        return compatService.aiGenerateCardCopy(body);
    }

    @PostMapping("/api/ai/generate-persona")
    public Map<String, Object> generatePersona() {
        return compatService.aiGeneratePersona();
    }

    @PostMapping("/api/ai/match-reason")
    public Map<String, Object> matchReason(@RequestBody(required = false) Map<String, Object> body) {
        return compatService.aiMatchReason();
    }

    @PostMapping("/api/ai/icebreakers")
    public Map<String, Object> icebreakers(@RequestBody(required = false) Map<String, Object> body) {
        return compatService.aiIcebreakers();
    }

    @PostMapping("/api/match/session")
    public Map<String, Object> matchSession() {
        return compatService.matchSession();
    }

    @PostMapping("/api/match/answer")
    public Map<String, Object> matchAnswer() {
        return Map.of();
    }

    @GetMapping("/api/match/candidates")
    public List<Map<String, Object>> matchCandidates() {
        return compatService.matchCandidates();
    }

    @PostMapping("/api/match/request")
    public Map<String, Object> matchRequest() {
        return compatService.matchStatus("sent");
    }

    @PostMapping("/api/match/respond")
    public Map<String, Object> matchRespond() {
        return compatService.matchStatus("accepted");
    }

    @GetMapping("/api/chat/conversations")
    public Object conversations() {
        return compatService.conversations();
    }

    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    public Object messages(@PathVariable String conversationId) {
        return compatService.messages(conversationId);
    }

    @PostMapping("/api/chat/conversations/{conversationId}/messages")
    public Map<String, Object> sendMessage(@PathVariable String conversationId,
                                           @RequestBody Map<String, Object> body) {
        return compatService.sendMessage(conversationId, body);
    }

    @PostMapping("/api/chat/conversations/{conversationId}/read")
    public void markRead(@PathVariable String conversationId) {
        compatService.markRead(conversationId);
    }
}
