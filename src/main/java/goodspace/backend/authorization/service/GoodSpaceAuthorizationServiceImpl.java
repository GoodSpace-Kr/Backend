package goodspace.backend.authorization.service;

import goodspace.backend.authorization.dto.request.AccessTokenReissueRequestDto;
import goodspace.backend.authorization.dto.request.SignInRequestDto;
import goodspace.backend.authorization.dto.request.SignUpRequestDto;
import goodspace.backend.authorization.dto.response.AccessTokenResponseDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.domain.user.GoodSpaceUser;
import goodspace.backend.domain.user.User;
import goodspace.backend.email.entity.EmailVerification;
import goodspace.backend.email.repository.EmailVerificationRepository;
import goodspace.backend.repository.UserRepository;
import goodspace.backend.security.TokenProvider;
import goodspace.backend.security.TokenType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class GoodSpaceAuthorizationServiceImpl implements GoodSpaceAuthorizationService {
    private static final Supplier<EntityNotFoundException> VERIFICATION_NOT_FOUND = () -> new EntityNotFoundException("이메일 인증 정보가 없습니다.");
    private static final Supplier<IllegalStateException> NOT_VERIFIED = () -> new IllegalStateException("이메일이 인증되지 않았습니다.");
    private static final Supplier<EntityNotFoundException> USER_NOT_FOUND = () -> new EntityNotFoundException("회원을 찾을 수 없습니다.");
    private static final Supplier<IllegalArgumentException> EXPIRED_TOKEN = () -> new IllegalArgumentException("만료된 토큰입니다.");

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    private final TokenProvider tokenProvider;

    @Override
    @Transactional
    public TokenResponseDto signUp(SignUpRequestDto requestDto) {
        checkEmailVerification(requestDto.email());

        GoodSpaceUser user = requestDto.toEntity();
        userRepository.save(user);

        String accessToken = tokenProvider.createToken(user.getId(), TokenType.ACCESS);
        String refreshToken = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshToken);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto signIn(SignInRequestDto requestDto) {
        GoodSpaceUser user = userRepository.findByEmailAndPassword(requestDto.email(), requestDto.password())
                .orElseThrow(USER_NOT_FOUND);

        String accessToken = tokenProvider.createToken(user.getId(), TokenType.ACCESS);
        String refreshToken = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshToken);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccessTokenResponseDto reissueAccessToken(AccessTokenReissueRequestDto requestDto) {
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

    private void checkEmailVerification(String email) {
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(VERIFICATION_NOT_FOUND);

        if (!emailVerification.isVerified()) {
            throw NOT_VERIFIED.get();
        }

        emailVerificationRepository.delete(emailVerification);
    }
}
