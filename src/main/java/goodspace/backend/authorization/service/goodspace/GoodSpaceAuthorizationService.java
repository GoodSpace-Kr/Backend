package goodspace.backend.authorization.service.goodspace;

import goodspace.backend.authorization.dto.request.SignInRequestDto;
import goodspace.backend.authorization.dto.request.SignUpRequestDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;

public interface GoodSpaceAuthorizationService {
    TokenResponseDto signUp(SignUpRequestDto requestDto);

    TokenResponseDto signIn(SignInRequestDto requestDto);
}
