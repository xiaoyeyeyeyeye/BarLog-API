package com.alcohol.compat.controller;

import com.alcohol.compat.dto.FrontendUpdateProfileRequest;
import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.FrontendUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FrontendUserController {

    private final FrontendCompatService compatService;

    @PatchMapping("/me")
    public FrontendUserVO updateMe(@Valid @RequestBody FrontendUpdateProfileRequest req) {
        return compatService.updateProfile(req);
    }
}
