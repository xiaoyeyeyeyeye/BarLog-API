package com.alcohol.vo.buddy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "摇一摇匹配结果")
public class BuddyMatchVO {

    private String userId;
    private String nickname;
    private String avatarEmoji;
    private String drinkName;
    private String vibeMumbling;
    private String icebreaker;
    private String reason;
}
