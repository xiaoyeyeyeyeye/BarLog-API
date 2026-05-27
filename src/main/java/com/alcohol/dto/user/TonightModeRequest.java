package com.alcohol.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TonightModeRequest {

    @NotNull(message = "请指定是否开启 Tonight Mode")
    private Boolean enabled;

    /** NONE / CHAT_OK / FIND_BUDDY / VIEW_ONLY */
    private String socialStatus;
}
