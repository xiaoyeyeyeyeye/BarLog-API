package com.alcohol.util;

import com.alcohol.common.BizException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

/**
 * 国际手机号 / 邮箱规范化（默认面向新加坡 +65）。
 */
public final class PhoneEmailUtil {

    private static final Set<String> SUPPORTED_COUNTRY_CODES = Set.of("+65", "+60", "+62", "+66", "+84", "+86", "+1");

    private PhoneEmailUtil() {
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        String e = email.trim().toLowerCase();
        if (!e.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BizException("邮箱格式不正确");
        }
        return e;
    }

    /** 规范为 E.164，如 +6591234567 */
    public static String normalizePhoneE164(String countryCode, String phone) {
        String cc = normalizeCountryCode(countryCode);
        String digits = phone.replaceAll("\\D", "");
        if (cc.equals("+65")) {
            if (digits.startsWith("65") && digits.length() == 10) {
                digits = digits.substring(2);
            }
            if (digits.length() != 8) {
                throw new BizException("新加坡手机号应为 8 位数字");
            }
            if (!digits.matches("^[689]\\d{7}$")) {
                throw new BizException("新加坡手机号格式不正确（应以 6/8/9 开头）");
            }
            return cc + digits;
        }
        if (digits.startsWith(cc.substring(1))) {
            return cc + digits.substring(cc.length() - 1);
        }
        return cc + digits;
    }

    public static String normalizeCountryCode(String countryCode) {
        String cc = StringUtils.hasText(countryCode) ? countryCode.trim() : "+65";
        if (!cc.startsWith("+")) {
            cc = "+" + cc;
        }
        if (!SUPPORTED_COUNTRY_CODES.contains(cc)) {
            throw new BizException("暂不支持该区号: " + cc);
        }
        return cc;
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BizException("哈希计算失败");
        }
    }

    public static String randomDigits(int length) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }
}
