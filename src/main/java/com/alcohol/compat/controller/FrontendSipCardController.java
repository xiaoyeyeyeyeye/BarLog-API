package com.alcohol.compat.controller;

import com.alcohol.compat.service.SipCardService;
import com.alcohol.compat.vo.FrontendSipCardDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sip-cards")
@RequiredArgsConstructor
public class FrontendSipCardController {

    private final SipCardService sipCardService;

    @GetMapping("/{checkInId}")
    public FrontendSipCardDetailVO detail(@PathVariable String checkInId) {
        return sipCardService.getDetail(checkInId);
    }
}
