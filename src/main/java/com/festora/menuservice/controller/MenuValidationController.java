package com.festora.menuservice.controller;

import com.festora.menuservice.dto.MenuValidationRequest;
import com.festora.menuservice.dto.MenuValidationResponse;
import com.festora.menuservice.service.MenuValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuValidationController {

    private final MenuValidationService validationService;

    @PostMapping("/validate")
    public MenuValidationResponse validateMenu(
            @RequestBody MenuValidationRequest request
    ) {
        return validationService.validate(request);
    }
}