package goodspace.backend.authorization.controller;

import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.authorization.service.facebook.FacebookOAuthService;
import goodspace.backend.authorization.service.google.GoogleOAuthService;
import goodspace.backend.authorization.service.kakao.KaKaoOAuthService;
import goodspace.backend.authorization.service.naver.NaverOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.SEE_OTHER;

@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
@Tag(
        name = "OAuth API",
        description = "소셜 회원가입, 로그인 관련 기능"
)
public class OAuthController {
    private final GoogleOAuthService googleOAuthService;
    private final KaKaoOAuthService kaKaoOAuthService;
    private final FacebookOAuthService facebookOAuthService;
    private final NaverOAuthService naverOAuthService;

    @GetMapping("/google")
    @Operation(
            summary = "구글 소셜 로그인/회원가입",
            description = "구글을 통해 사용자를 인증하고 JWT를 발급합니다"
    )
    public ResponseEntity<TokenResponseDto> googleAuthorization(@RequestParam(name = "code") String code) {
        String accessToken = googleOAuthService.getAccessToken(code);
        TokenResponseDto responseDto = googleOAuthService.signUpOrSignIn(accessToken);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/google/redirection")
    @Operation(
            summary = "구글 로그인 화면 리다이렉트",
            description = "구글 로그인 화면으로 리다이렉트합니다"
    )
    public ResponseEntity<Void> googleRedirect() {
        return ResponseEntity.status(SEE_OTHER)
                .header(HttpHeaders.LOCATION, googleOAuthService.getOauthPageRedirectUrl())
                .build();
    }

    @GetMapping("/kakao")
    @Operation(
            summary = "카카오 소셜 로그인",
            description = "카카오를 통해 사용자를 인증하고 JWT를 발급합니다"
    )
    public ResponseEntity<TokenResponseDto> kakaoAuthorization(@RequestParam(name = "code") String code) {
        String accessToken = kaKaoOAuthService.getAccessToken(code);
        TokenResponseDto responseDto = kaKaoOAuthService.signUpOrSignIn(accessToken);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/kakao/redirection")
    @Operation(
            summary = "카카오 로그인 화면 리다이렉트",
            description = "카카오 로그인 화면으로 리다이렉트합니다"
    )
    public ResponseEntity<Void> kakaoRedirect() {
        return ResponseEntity.status(SEE_OTHER)
                .header(HttpHeaders.LOCATION, kaKaoOAuthService.getOauthPageRedirectUrl())
                .build();
    }

    @GetMapping("/facebook")
    @Operation(
            summary = "페이스북 소셜 로그인",
            description = "페이스북을 통해 사용자를 인증하고 JWT를 발급합니다"
    )
    public ResponseEntity<TokenResponseDto> facebookAuthorization(@RequestParam(name = "code") String code) {
        String accessToken = facebookOAuthService.getAccessToken(code);
        TokenResponseDto responseDto = facebookOAuthService.signUpOrSignIn(accessToken);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/facebook/redirection")
    @Operation(
            summary = "페이스북 로그인 화면 리다이렉트",
            description = "페이스북 로그인 화면으로 리다이렉트합니다"
    )
    public ResponseEntity<Void> facebookRedirect() {
        return ResponseEntity.status(SEE_OTHER)
                .header(HttpHeaders.LOCATION, facebookOAuthService.getOauthPageRedirectUrl())
                .build();
    }

    @GetMapping("/naver")
    @Operation(
            summary = "네이버 소셜 로그인",
            description = "네이버를 통해 사용자를 인증하고 JWT를 발급합니다"
    )
    public ResponseEntity<TokenResponseDto> naverAuthorization(@RequestParam(name = "code") String code) {
        String accessToken = naverOAuthService.getAccessToken(code);
        TokenResponseDto responseDto = naverOAuthService.signUpOrSignIn(accessToken);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/naver/redirection")
    @Operation(
            summary = "네이버 로그인 화면 리다이렉트",
            description = "네이버 로그인 화면으로 리다이렉트합니다"
    )
    public ResponseEntity<Void> naverRedirect() {
        return ResponseEntity.status(SEE_OTHER)
                .header(HttpHeaders.LOCATION, naverOAuthService.getOauthPageRedirectUrl())
                .build();
    }
}
