package com.ikongserver.controller;

import com.ikongserver.common.ApiResponse;
import com.ikongserver.dto.GuardianDto;
import com.ikongserver.service.GuardianService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guardians")
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    public ResponseEntity<ApiResponse<GuardianDto.ResponseRegister>> registerGuardian(
        @RequestParam Long userId, // TODO: JWT 인증 후 SecurityContext에서 userId 추출로 변경
        @RequestBody GuardianDto.RequestRegister request
    ) {
        GuardianDto.ResponseRegister response = guardianService.registerGuardian(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuardianDto.ResponseGuardian>>> getGuardians(
        @RequestParam Long userId // TODO: JWT 인증 후 SecurityContext에서 userId 추출로 변경
    ) {
        List<GuardianDto.ResponseGuardian> response = guardianService.getGuardians(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{guardianId}")
    public ResponseEntity<ApiResponse<Void>> deleteGuardian(
        @RequestParam Long userId, // TODO: JWT 인증 후 SecurityContext에서 userId 추출로 변경
        @PathVariable Long guardianId
    ) {
        guardianService.deleteGuardian(userId, guardianId);
        return ResponseEntity.ok(ApiResponse.ok("보호자가 삭제되었습니다.", null));
    }
}
