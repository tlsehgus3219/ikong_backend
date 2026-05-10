package com.ikongserver.controller;

import com.ikongserver.dto.UserDto.MainProfileResponse;
import com.ikongserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    // 메인 화면 이름 및 상태 표시
    @GetMapping("/{userId}/main")
    public ResponseEntity<MainProfileResponse> getUser(@PathVariable Long userId) {

        MainProfileResponse response = userService.getMainProfile(userId);
        return ResponseEntity.ok(response);

    }
}
