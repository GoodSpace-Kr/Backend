package goodspace.backend.authorization.controller;

import goodspace.backend.authorization.dto.request.SignInRequestDto;
import goodspace.backend.authorization.dto.request.SignUpRequestDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.authorization.service.goodspace.GoodSpaceAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/authorization")
@Tag(
        name = "자체 인증/인가 API",
        description = "자체 회원가입, 로그인 관련 기능"
)
public class GoodSpaceAuthorizationController {
    private final GoodSpaceAuthorizationService goodSpaceAuthorizationService;

    @PostMapping("/sign-up")
    @Operation(
            summary = "회원가입",
            description = "전달받은 정보를 통해 새로운 회원을 생성합니다"
    )
    public ResponseEntity<TokenResponseDto> signUp(SignUpRequestDto requestDto) {
        TokenResponseDto responseDto = goodSpaceAuthorizationService.signUp(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/sign-in")
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호를 통해 사용자를 인증하고 JWT 토큰을 발급합니다"
    )
    public ResponseEntity<TokenResponseDto> signIn(SignInRequestDto requestDto) {
        TokenResponseDto responseDto = goodSpaceAuthorizationService.signIn(requestDto);

        return ResponseEntity.ok(responseDto);
    }
}
