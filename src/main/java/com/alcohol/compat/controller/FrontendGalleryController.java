package com.alcohol.compat.controller;

import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.FrontendGalleryPostVO;
import com.alcohol.compat.vo.FrontendItemsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class FrontendGalleryController {

    private final FrontendCompatService compatService;

    @GetMapping("/feed")
    public FrontendItemsResponse<FrontendGalleryPostVO> feed(@RequestParam(required = false) String city) {
        return compatService.galleryFeed(city);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public FrontendGalleryPostVO createPost(@RequestBody Map<String, Object> body) {
        return stubPost(body);
    }

    @GetMapping("/posts/{postId}")
    public FrontendGalleryPostVO getPost(@PathVariable String postId) {
        return compatService.galleryFeed(null).getItems().stream()
                .filter(p -> postId.equals(p.getId()))
                .findFirst()
                .orElse(stubPost(Map.of("imageUrl", "https://images.barlog.local/gallery/post.jpg")));
    }

    @PostMapping("/posts/{postId}/like")
    public FrontendGalleryPostVO like(@PathVariable String postId) {
        FrontendGalleryPostVO post = getPost(postId);
        post.setLikedCount(post.getLikedCount() + 1);
        return post;
    }

    private FrontendGalleryPostVO stubPost(Map<String, Object> body) {
        FrontendGalleryPostVO post = new FrontendGalleryPostVO();
        post.setId("post_" + java.util.UUID.randomUUID());
        post.setUserId("demo");
        post.setAuthorName("BarLog");
        post.setImageUrl(String.valueOf(body.getOrDefault("imageUrl", "https://images.barlog.local/gallery/post.jpg")));
        post.setLikedCount(0);
        post.setCreatedAt(java.time.Instant.now().toString());
        return post;
    }
}
