package com.alcohol.compat.controller;

import com.alcohol.compat.dto.MatchConnectRequest;
import com.alcohol.compat.service.MatchService;
import com.alcohol.compat.vo.MatchCandidateVO;
import com.alcohol.compat.vo.MatchConnectResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class FrontendMatchController {

    private final MatchService matchService;

    @GetMapping("/candidates")
    public List<MatchCandidateVO> candidates() {
        return matchService.listCandidates();
    }

    @PostMapping("/connect")
    public MatchConnectResultVO connect(@Valid @RequestBody MatchConnectRequest request) {
        return matchService.connect(request.getUserId());
    }
}
