package com.alcohol.compat;

import com.alcohol.common.BizException;
import com.alcohol.context.UserContext;
import com.alcohol.entity.CheckIn;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 打卡读权限：私人日记仅本人；单条详情对齐 owner / public 规则。
 */
@Component
public class CheckInAccessHelper {

    public String requireUserId() {
        String userId = UserContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BizException("Missing bearer token", 401, "AUTH_REQUIRED");
        }
        return userId;
    }

    public void assertSelf(String ownerUserId) {
        String currentUserId = requireUserId();
        if (!currentUserId.equals(ownerUserId)) {
            throw new BizException("Forbidden", 403, "CHECKIN_FORBIDDEN");
        }
    }

    public void assertReadable(CheckIn checkIn) {
        if (checkIn == null) {
            throw new BizException("Check-in not found", 404);
        }
        String currentUserId = requireUserId();
        boolean isOwner = currentUserId.equals(checkIn.getUserId());
        boolean isPublic = "PUBLIC".equals(checkIn.getVisibility())
                || "TONIGHT_ONLY".equals(checkIn.getVisibility());
        if (!isOwner && !isPublic) {
            throw new BizException("Forbidden", 403, "CHECKIN_FORBIDDEN");
        }
        if (!isOwner && checkIn.getExpiresAt() != null
                && checkIn.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("Check-in not found", 404);
        }
    }
}
