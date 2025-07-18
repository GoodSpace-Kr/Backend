package goodspace.backend.authorization.controller;

import goodspace.backend.authorization.dto.request.AccessTokenReissueRequestDto;
import goodspace.backend.authorization.dto.response.AccessTokenResponseDto;
import goodspace.backend.authorization.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
@Tag(
        name = "토큰 재발급 API",
        description = "리프레쉬 토큰을 통해 엑세스 토큰을 재발급"
)
public class AccessTokenReissueController {
    private final AccessTokenService accessTokenService;

    @PostMapping("/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = "리프레쉬 토큰을 통해 엑세스 토큰을 재발급합니다"
    )
    public ResponseEntity<AccessTokenResponseDto> reissueAccessToken(AccessTokenReissueRequestDto requestDto) {
        AccessTokenResponseDto responseDto = accessTokenService.reissue(requestDto);

        return ResponseEntity.ok(responseDto);
    }
}
