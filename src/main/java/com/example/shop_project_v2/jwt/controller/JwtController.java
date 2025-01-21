package com.example.shop_project_v2.jwt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.shop_project_v2.jwt.JwtProvider;
import com.example.shop_project_v2.jwt.dto.JwtTokenDto;
import com.example.shop_project_v2.jwt.dto.JwtTokenLoginRequest;
import com.example.shop_project_v2.jwt.dto.JwtTokenResponse;
import com.example.shop_project_v2.member.service.MemberService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class JwtController {
	private final MemberService memberService;
	private final JwtProvider jwtProvider;
	
	@PostMapping("/jwt-login")
	public ResponseEntity<JwtTokenResponse> jwtLogin(@RequestBody JwtTokenLoginRequest request,
													HttpServletResponse response) {
		JwtTokenDto jwtTokenDto = memberService.login(request);
		
		// 1) Access Token 쿠키로 저장
        Cookie accessTokenCookie = new Cookie("accessToken", jwtTokenDto.getAccessToken());
        accessTokenCookie.setHttpOnly(true);   // 자바스크립트 접근 방지
        accessTokenCookie.setSecure(false);     // HTTPS에서만 전송
        accessTokenCookie.setPath("/");        // 모든 경로에서 쿠키 전송
        accessTokenCookie.setMaxAge(10 * 60);  // 예: 만료시간 10분(초 단위)

        // 2) Refresh Token 쿠키로 저장 (이미 구현된 부분과 동일)
        Cookie refreshTokenCookie = new Cookie("refreshToken", jwtTokenDto.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(14 * 24 * 60 * 60); // 예: 2주

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        // csrf 공격을 막기 위한 samesite 설정
        response.setHeader("Set-Cookie", String.format("accessToken=%s; Path=/; HttpOnly; Secure; SameSite=Lax", jwtTokenDto.getAccessToken()));
        response.addHeader("Set-Cookie", String.format("refreshToken=%s; Path=/; HttpOnly; Secure; SameSite=Lax", jwtTokenDto.getRefreshToken()));
        
        // 3) 추가로 클라이언트에 AccessToken을 JSON으로 줄 필요가 없다면, 
        //    JwtTokenResponse 대신 간단한 성공 메시지를 내려줘도 됨.
        return ResponseEntity.ok(
            JwtTokenResponse.builder()
            .accessToken(jwtTokenDto.getAccessToken())
            .build()
        );
	}

}
