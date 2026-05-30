package com.alcohol.compat.vo;

import lombok.Data;

@Data
public class MatchConnectResultVO {

    private String conversationId;
    private String status;
    private MatchPeerVO peer;
}
