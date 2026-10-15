package com.example.sitp.access.controller;

import com.example.sitp.access.dto.CodeResponse;
import com.example.sitp.access.dto.RedeemResponse;
import com.example.sitp.access.service.RedeemableCodeService;
import com.example.sitp.common.NoAccessGrantedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
public class RedeemableCodeController {

    private final RedeemableCodeService codeService;

    public RedeemableCodeController(RedeemableCodeService codeService) {
        this.codeService = codeService;
    }

    private boolean isCodeManager(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BOSS")
                        || a.getAuthority().equals("ROLE_ADMIN_ASSISTANT"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public List<CodeResponse> generate(@RequestParam Long courseId, @RequestParam int count,
                                       @RequestParam(required = false) Integer validityDays) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isCodeManager(auth)) {
            throw new NoAccessGrantedException();
        }

        return codeService.generateCodes(courseId, count, validityDays).stream()
                .map(CodeResponse::fromEntity)
                .toList();
    }

    @GetMapping
    public List<CodeResponse> listForCourse(@RequestParam Long courseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isCodeManager(auth)) {
            throw new NoAccessGrantedException();
        }

        return codeService.listCodesForCourse(courseId).stream()
                .map(CodeResponse::fromEntity)
                .toList();
    }

    @PostMapping("/{code}/redeem")
    public RedeemResponse redeem(@PathVariable String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        Long userId = (Long) auth.getPrincipal();
        return codeService.redeem(code, userId);
    }

    @PostMapping("/{code}/revoke")
    public CodeResponse revoke(@PathVariable String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isCodeManager(auth)) {
            throw new NoAccessGrantedException();
        }

        return codeService.revoke(code);
    }
}
