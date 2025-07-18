package goodspace.backend.authorization.service;

import goodspace.backend.authorization.dto.request.AccessTokenReissueRequestDto;
import goodspace.backend.authorization.dto.response.AccessTokenResponseDto;

public interface AccessTokenService {
    AccessTokenResponseDto reissue(AccessTokenReissueRequestDto requestDto);
}
