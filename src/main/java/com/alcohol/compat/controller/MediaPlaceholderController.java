package com.alcohol.compat.controller;

import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaPlaceholderController {

    private final CheckInMapper checkInMapper;
    private final UserMapper userMapper;

    @GetMapping(value = "/sip-card/{checkInId}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> sipCardPlaceholder(@PathVariable String checkInId) {
        CheckIn checkIn = checkInMapper.selectById(checkInId);
        String drinkName = checkIn != null && StringUtils.hasText(checkIn.getDrinkName())
                ? checkIn.getDrinkName()
                : "Tonight's Sip";
        String barName = checkIn != null && StringUtils.hasText(checkIn.getLocationName())
                ? checkIn.getLocationName()
                : "BarLog";
        String author = "BarLog";
        if (checkIn != null && StringUtils.hasText(checkIn.getUserId())) {
            User user = userMapper.selectById(checkIn.getUserId());
            if (user != null && StringUtils.hasText(user.getNickname())) {
                author = user.getNickname();
            }
        }
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="480" height="600" viewBox="0 0 480 600">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="#2a120f"/>
                      <stop offset="100%%" stop-color="#120605"/>
                    </linearGradient>
                  </defs>
                  <rect width="480" height="600" fill="url(#bg)"/>
                  <rect x="28" y="28" width="424" height="544" rx="24" fill="none" stroke="#c68334" stroke-width="3" opacity="0.55"/>
                  <text x="240" y="250" fill="#faf6ee" font-size="34" font-family="Georgia, serif" text-anchor="middle">%s</text>
                  <text x="240" y="305" fill="#c68334" font-size="18" font-family="Arial, sans-serif" text-anchor="middle">%s</text>
                  <text x="240" y="360" fill="#a8988c" font-size="16" font-family="Arial, sans-serif" text-anchor="middle">by %s</text>
                  <text x="240" y="520" fill="#7a665d" font-size="14" font-family="Arial, sans-serif" text-anchor="middle">BarLog Sip Card</text>
                </svg>
                """.formatted(escapeXml(drinkName), escapeXml(barName), escapeXml(author));
        return svgResponse(svg);
    }

    @GetMapping(value = "/avatar/{userId}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> avatarPlaceholder(@PathVariable String userId) {
        User user = userMapper.selectById(userId);
        String label = "?";
        if (user != null) {
            if (StringUtils.hasText(user.getNickname())) {
                label = user.getNickname().substring(0, 1).toUpperCase();
            } else if (StringUtils.hasText(user.getEmail())) {
                label = user.getEmail().substring(0, 1).toUpperCase();
            }
        }
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">
                  <rect width="128" height="128" rx="28" fill="#214b34"/>
                  <rect x="4" y="4" width="120" height="120" rx="24" fill="none" stroke="#9fbf8f" stroke-width="3" opacity="0.45"/>
                  <text x="64" y="78" fill="#faf6ee" font-size="48" font-family="Arial, sans-serif" font-weight="700" text-anchor="middle">%s</text>
                </svg>
                """.formatted(escapeXml(label));
        return svgResponse(svg);
    }

    private ResponseEntity<byte[]> svgResponse(String svg) {
        byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(new MediaType("image", "svg+xml", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
