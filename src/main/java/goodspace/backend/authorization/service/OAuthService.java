package goodspace.backend.authorization.service;

import goodspace.backend.authorization.dto.response.TokenResponseDto;

public interface OAuthService {
    String getAccessToken(String code);

    TokenResponseDto signUpOrSignIn(String accessToken);

    String getOauthPageRedirectUrl();
}
