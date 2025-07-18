package goodspace.backend.authorization.service;

import goodspace.backend.authorization.dto.request.AccessTokenReissueRequestDto;
import goodspace.backend.authorization.dto.response.AccessTokenResponseDto;
import goodspace.backend.domain.user.User;
import goodspace.backend.repository.UserRepository;
import goodspace.backend.security.TokenProvider;
import goodspace.backend.security.TokenType;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@AllArgsConstructor
public class AccessTokenServiceImpl implements AccessTokenService {
    private static final Supplier<EntityNotFoundException> USER_NOT_FOUND = () -> new EntityNotFoundException("회원을 찾을 수 없습니다.");
    private static final Supplier<IllegalArgumentException> EXPIRED_TOKEN = () -> new IllegalArgumentException("만료된 토큰입니다.");

    private final UserRepository userRepository;

    private final TokenProvider tokenProvider;

    @Override
    @Transactional(readOnly = true)
    public AccessTokenResponseDto reissue(AccessTokenReissueRequestDto requestDto) {
        String refreshToken = requestDto.refreshToken();
        long userId = tokenProvider.getIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(USER_NOT_FOUND);

        String existsRefreshToken = user.getRefreshToken();

        if (!refreshToken.equals(existsRefreshToken)) {
            throw EXPIRED_TOKEN.get();
        }

        return AccessTokenResponseDto.builder()
                .accessToken(tokenProvider.createToken(userId, TokenType.ACCESS))
                .build();
    }
}
