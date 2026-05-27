package com.alcohol.service.auth;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 校验 Google idToken / Facebook accessToken，提取用户信息。
 */
@Service
@RequiredArgsConstructor
public class OAuthTokenVerifier {

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public OAuthUserInfo verifyGoogle(String idToken) {
        String clientId = authProperties.getOauth().getGoogle().getClientId();
        if (!StringUtils.hasText(clientId)) {
            throw new BizException("Google 登录未配置，请联系管理员");
        }
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            JsonNode node = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            if (node.has("error_description")) {
                throw new BizException("Google 令牌无效");
            }
            String aud = node.path("aud").asText();
            if (!clientId.equals(aud)) {
                throw new BizException("Google 令牌与应用不匹配");
            }
            OAuthUserInfo info = new OAuthUserInfo();
            info.setProviderUserId(node.path("sub").asText());
            info.setEmail(node.path("email").asText(null));
            info.setName(node.path("name").asText(null));
            info.setAvatarUrl(node.path("picture").asText(null));
            info.setRawProfile(node.toString());
            return info;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Google 登录验证失败");
        }
    }

    public OAuthUserInfo verifyFacebook(String accessToken) {
        String appId = authProperties.getOauth().getFacebook().getAppId();
        String appSecret = authProperties.getOauth().getFacebook().getAppSecret();
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BizException("Facebook 登录未配置，请联系管理员");
        }
        try {
            String debugUrl = "https://graph.facebook.com/debug_token?input_token=" + accessToken
                    + "&access_token=" + appId + "|" + appSecret;
            JsonNode debug = objectMapper.readTree(restTemplate.getForObject(debugUrl, String.class));
            if (!debug.path("data").path("is_valid").asBoolean(false)) {
                throw new BizException("Facebook 令牌无效");
            }
            String fbUserId = debug.path("data").path("user_id").asText();
            String meUrl = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)"
                    + "&access_token=" + accessToken;
            JsonNode me = objectMapper.readTree(restTemplate.getForObject(meUrl, String.class));
            OAuthUserInfo info = new OAuthUserInfo();
            info.setProviderUserId(fbUserId != null ? fbUserId : me.path("id").asText());
            info.setEmail(me.path("email").asText(null));
            info.setName(me.path("name").asText(null));
            info.setAvatarUrl(me.path("picture").path("data").path("url").asText(null));
            info.setRawProfile(me.toString());
            return info;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Facebook 登录验证失败");
        }
    }
}
