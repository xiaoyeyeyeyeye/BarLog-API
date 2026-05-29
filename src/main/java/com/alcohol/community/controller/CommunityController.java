package com.alcohol.community.controller;

import com.alcohol.community.CommunityFeedService;
import com.alcohol.community.CommunityInteractionService;
import com.alcohol.community.vo.*;
import com.alcohol.compat.vo.FrontendItemsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityFeedService feedService;
    private final CommunityInteractionService interactionService;

    @GetMapping("/eligibility")
    public CommunityEligibilityVO eligibility(@RequestParam(required = false) String city,
                                                @RequestParam(required = false) String barId) {
        return feedService.eligibility(city, barId);
    }

    @GetMapping("/feed")
    public FrontendItemsResponse<CommunityPostVO> feed(@RequestParam(required = false, defaultValue = "global") String scope,
                                                       @RequestParam(required = false) String city,
                                                       @RequestParam(required = false) String barId,
                                                       @RequestParam(required = false) String range,
                                                       @RequestParam(required = false) String cursor,
                                                       @RequestParam(required = false) Integer limit) {
        return feedService.communityFeed(scope, city, barId, range, cursor, limit);
    }

    @PostMapping("/posts/{checkInId}/like")
    public CommunityLikeResultVO like(@PathVariable String checkInId) {
        return interactionService.toggleLike(checkInId);
    }

    @GetMapping("/posts/{checkInId}/comments")
    public FrontendItemsResponse<CommunityCommentVO> comments(@PathVariable String checkInId) {
        return interactionService.listComments(checkInId);
    }

    @PostMapping("/posts/{checkInId}/comments")
    public CommunityCommentVO addComment(@PathVariable String checkInId,
                                         @RequestBody Map<String, String> body) {
        return interactionService.addComment(checkInId, body != null ? body.get("body") : null);
    }

    @PostMapping("/users/{userId}/wave")
    public CommunityWaveResultVO wave(@PathVariable String userId,
                                      @RequestBody(required = false) Map<String, String> body) {
        String checkInId = body != null ? body.get("checkInId") : null;
        return interactionService.wave(userId, checkInId);
    }
}
