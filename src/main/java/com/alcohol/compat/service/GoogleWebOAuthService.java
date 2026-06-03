package com.alcohol.compat.service;

import com.alcohol.common.BizException;
import com.alcohol.compat.CompatAuthSupport;
import com.alcohol.compat.FrontendMapper;
import com.alcohol.compat.vo.FrontendAuthResponse;
import com.alcohol.compat.vo.FrontendGoogleAuthStartResponse;
import com.alcohol.config.AuthProperties;
import com.alcohol.entity.User;
import com.alcohol.enums.AuthProvider;
import com.alcohol.mapper.UserMapper;
import com.alcohol.service.auth.OAuthTokenVerifier;
import com.alcohol.service.auth.OAuthUserInfo;
import com.alcohol.service.auth.UserAccountService;
import com.alcohol.vo.persona.PersonaVO;
import com.alcohol.context.UserContext;
import com.alcohol.service.PersonaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleWebOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final AuthProperties authProperties;
    private final OAuthTokenVerifier oauthTokenVerifier;
    private final UserAccountService userAccountService;
    private final CompatAuthSupport compatAuthSupport;
    private final FrontendMapper mapper;
    private final PersonaService personaService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public FrontendGoogleAuthStartResponse start(String redirectUri, String mode) {
        assertGoogleConfigured();
        validateRedirectUri(redirectUri);
        validateMode(mode);

        String state = encodeState(redirectUri, mode);
        String callbackUrl = requireCallbackUrl();
        String authUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", authProperties.getOauth().getGoogle().getClientId())
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "online")
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new FrontendGoogleAuthStartResponse(authUrl);
    }

    public void handleCallback(String code, String state, HttpServletResponse response) throws IOException {
        assertGoogleConfigured();
        if (!StringUtils.hasText(code)) {
            redirectError(response, state, "Missing Google authorization code");
            return;
        }

        OAuthState oauthState;
        try {
            oauthState = decodeState(state);
        } catch (Exception e) {
            redirectError(response, null, "Invalid OAuth state");
            return;
        }

        try {
            String idToken = exchangeCodeForIdToken(code);
            OAuthUserInfo info = oauthTokenVerifier.verifyGoogle(idToken);
            User user = resolveUser(info, oauthState.mode());
            FrontendAuthResponse auth = buildAuthResponse(user);
            String target = oauthState.redirectUri()
                    + "?accessToken=" + urlEncode(auth.getAccessToken())
                    + (StringUtils.hasText(auth.getRefreshToken())
                    ? "&refreshToken=" + urlEncode(auth.getRefreshToken())
                    : "");
            response.sendRedirect(target);
        } catch (BizException e) {
            redirectError(response, oauthState.redirectUri(), e.getMessage());
        } catch (Exception e) {
            log.warn("Google OAuth callback failed", e);
            redirectError(response, oauthState.redirectUri(), "Google sign-in failed");
        }
    }

    public FrontendAuthResponse complete(String accessToken, String refreshToken) {
        if (!compatAuthSupport.validateAccessToken(accessToken)) {
            throw new BizException("Invalid access token", 401);
        }
        String userId = compatAuthSupport.userIdFromToken(accessToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("User not found", 401);
        }
        userAccountService.assertActive(user);
        return buildAuthResponse(user);
    }

    private User resolveUser(OAuthUserInfo info, String mode) {
        if ("login".equalsIgnoreCase(mode)) {
            return userAccountService.loginOAuthUser(info, AuthProvider.GOOGLE);
        }
        return userAccountService.findOrCreateOAuthUser(info, AuthProvider.GOOGLE, null).user();
    }

    private String exchangeCodeForIdToken(String code) {
        String callbackUrl = requireCallbackUrl();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", authProperties.getOauth().getGoogle().getClientId());
        form.add("client_secret", authProperties.getOauth().getGoogle().getClientSecret());
        form.add("redirect_uri", callbackUrl);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                Map.class);
        Map<?, ?> body = response.getBody();
        if (body == null || !body.containsKey("id_token")) {
            throw new BizException("Google token exchange failed");
        }
        return String.valueOf(body.get("id_token"));
    }

    private FrontendAuthResponse buildAuthResponse(User user) {
        FrontendAuthResponse resp = new FrontendAuthResponse();
        resp.setUser(mapper.toUser(user, resolvePersonaStatement(user.getId())));
        resp.setAccessToken(compatAuthSupport.issueAccessToken(user));
        resp.setRefreshToken(compatAuthSupport.issueRefreshToken(user));
        return resp;
    }

    private String resolvePersonaStatement(String userId) {
        String previous = UserContext.getUserId();
        try {
            UserContext.setUserId(userId);
            PersonaVO persona = personaService.getMyPersona();
            return persona != null && StringUtils.hasText(persona.getGeneratedText())
                    ? persona.getGeneratedText()
                    : null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (StringUtils.hasText(previous)) {
                UserContext.setUserId(previous);
            } else {
                UserContext.clear();
            }
        }
    }

    private void redirectError(HttpServletResponse response, String redirectUri, String message) throws IOException {
        if (!StringUtils.hasText(redirectUri)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(message);
            return;
        }
        response.sendRedirect(redirectUri + "?error=" + urlEncode(message));
    }

    private void assertGoogleConfigured() {
        AuthProperties.OAuth.Google google = authProperties.getOauth().getGoogle();
        if (!StringUtils.hasText(google.getClientId()) || !StringUtils.hasText(google.getClientSecret())) {
            throw new BizException("Google sign-in is not configured", 503);
        }
    }

    private String requireCallbackUrl() {
        String callbackUrl = authProperties.getOauth().getGoogle().getCallbackUrl();
        if (!StringUtils.hasText(callbackUrl)) {
            throw new BizException("Google OAuth callback URL is not configured", 503);
        }
        return callbackUrl.trim();
    }

    private void validateMode(String mode) {
        if (!"login".equalsIgnoreCase(mode) && !"register".equalsIgnoreCase(mode)) {
            throw new BizException("mode must be login or register", 400);
        }
    }

    private void validateRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            throw new BizException("redirectUri is required", 400);
        }
        String trimmed = redirectUri.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://127.0.0.1")
                && !trimmed.startsWith("http://localhost")) {
            throw new BizException("redirectUri must use https in production", 400);
        }
        List<String> allowed = allowedRedirectOrigins();
        if (allowed.isEmpty()) {
            return;
        }
        boolean ok = allowed.stream().anyMatch(trimmed::startsWith);
        if (!ok) {
            throw new BizException("redirectUri is not allowed", 400);
        }
    }

    private List<String> allowedRedirectOrigins() {
        String raw = authProperties.getOauth().getGoogle().getAllowedRedirectOrigins();
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String encodeState(String redirectUri, String mode) {
        try {
            Map<String, String> payload = Map.of(
                    "redirectUri", redirectUri.trim(),
                    "mode", mode.trim().toLowerCase(Locale.ROOT),
                    "nonce", UUID.randomUUID().toString());
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new BizException("Unable to start Google sign-in", 500);
        }
    }

    private OAuthState decodeState(String state) throws IOException {
        byte[] bytes = Base64.getUrlDecoder().decode(state);
        Map<?, ?> payload = objectMapper.readValue(bytes, Map.class);
        return new OAuthState(
                String.valueOf(payload.get("redirectUri")),
                String.valueOf(payload.get("mode")));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record OAuthState(String redirectUri, String mode) {
    }
}
