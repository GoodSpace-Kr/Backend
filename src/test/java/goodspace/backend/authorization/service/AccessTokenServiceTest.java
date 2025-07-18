package goodspace.backend.authorization.service;

import goodspace.backend.authorization.dto.request.AccessTokenReissueRequestDto;
import goodspace.backend.authorization.dto.response.AccessTokenResponseDto;
import goodspace.backend.domain.user.GoodSpaceUser;
import goodspace.backend.fixture.GoodSpaceUserFixture;
import goodspace.backend.repository.UserRepository;
import goodspace.backend.security.TokenProvider;
import goodspace.backend.security.TokenType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AccessTokenServiceTest {
    private final SecureRandom random = new SecureRandom();

    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private GoodSpaceUser existUser;

    @BeforeEach
    void resetEntity() {
        existUser = createEntityFromFixture(GoodSpaceUserFixture.DEFAULT);
    }

    @Nested
    class reissueAccessToken {
        @Test
        @DisplayName("전달한 토큰이 저장된 리프레쉬 토큰과 일치하면 엑세스 토큰을 발급한다")
        void ifRefreshTokenIsLegalThenIssueAccessToken() {
            // given
            String refreshToken = existUser.getRefreshToken();

            // when
            AccessTokenResponseDto responseDto = accessTokenService.reissue(new AccessTokenReissueRequestDto(refreshToken));

            // then
            String accessToken = responseDto.accessToken();
            boolean isLegalAccessToken = tokenProvider.validateToken(accessToken, TokenType.ACCESS);

            assertThat(isLegalAccessToken).isTrue();
        }

        @Test
        @DisplayName("저장된 리프레쉬 토큰과 일치하지 않으면 예외를 던진다")
        void ifIllegalRefreshTokenThenThrowException() throws InterruptedException {
            String differentRefreshToken = getDifferentRefreshToken(existUser.getId());

            assertThatThrownBy(() -> accessTokenService.reissue(new AccessTokenReissueRequestDto(differentRefreshToken)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private GoodSpaceUser createEntityFromFixture(GoodSpaceUserFixture fixture) {
        GoodSpaceUser user = fixture.getInstance();
        userRepository.save(user);

        String refreshToken = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshToken);

        return (GoodSpaceUser) userRepository.findById(user.getId())
                .orElseThrow();
    }

    private String getDifferentRefreshToken(long userId) throws InterruptedException {
        Thread.sleep(1000);

        return tokenProvider.createToken(userId, TokenType.REFRESH);
    }
}
