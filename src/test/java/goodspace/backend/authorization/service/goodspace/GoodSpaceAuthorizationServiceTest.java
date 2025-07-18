package goodspace.backend.authorization.service.goodspace;

import goodspace.backend.authorization.dto.request.SignInRequestDto;
import goodspace.backend.authorization.dto.request.SignUpRequestDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.domain.user.GoodSpaceUser;
import goodspace.backend.email.entity.EmailVerification;
import goodspace.backend.email.repository.EmailVerificationRepository;
import goodspace.backend.fixture.EmailVerificationFixture;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GoodSpaceAuthorizationServiceTest {
    private static final int SLEEP_FOR_GET_DIFFERENT_TOKEN = 1000;
    private static final String DEFAULT_PASSWORD = "defaultPassword!";

    private final GoodSpaceAuthorizationService authorizationService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private EmailVerification verifiedEmail;
    private EmailVerification notVerifiedEmail;
    private GoodSpaceUser existUser;

    @BeforeEach
    void resetEntities() {
        verifiedEmail = createEntityFromFixture(EmailVerificationFixture.VERIFIED);
        notVerifiedEmail = createEntityFromFixture(EmailVerificationFixture.NOT_VERIFIED);
        existUser = createEntityFromFixture(GoodSpaceUserFixture.DEFAULT);
    }

    @Nested
    class signUp {
        @Test
        @DisplayName("인증된 이메일로 새로운 회원을 생성한다")
        void createUserWithVerifiedEmail() {
            // given
            String email = verifiedEmail.getEmail();

            // when
            authorizationService.signUp(new SignUpRequestDto(email, DEFAULT_PASSWORD));

            // then
            boolean isPresent = userRepository.findByEmailAndPassword(email, DEFAULT_PASSWORD)
                    .isPresent();
            assertThat(isPresent).isTrue();
        }

        @Test
        @DisplayName("인증되지 않은 이메일을 사용하면 예외가 발생한다")
        void ifNotVerifiedEmailThenThrowException() {
            // given
            String email = notVerifiedEmail.getEmail();

            // then
            assertThatThrownBy(() -> authorizationService.signUp(new SignUpRequestDto(email, DEFAULT_PASSWORD)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("유저 생성 후 인증 정보는 제거한다")
        void deleteVerificationInformationAfterCreateUser() {
            // given
            String email = verifiedEmail.getEmail();

            // when
            authorizationService.signUp(new SignUpRequestDto(email, DEFAULT_PASSWORD));

            // then
            boolean isEmpty = emailVerificationRepository.findByEmail(email)
                    .isEmpty();
            assertThat(isEmpty).isTrue();
        }

        @Test
        @DisplayName("회원 생성에 성공하면 JWT 토큰을 발급한다")
        void issueJwtIfUserCreateSucceeded() {
            // given
            String email = verifiedEmail.getEmail();

            // when
            TokenResponseDto jwt = authorizationService.signUp(new SignUpRequestDto(email, DEFAULT_PASSWORD));

            // then
            boolean isLegalAccessToken = tokenProvider.validateToken(jwt.accessToken(), TokenType.ACCESS);
            boolean isLegalRefreshToken = tokenProvider.validateToken(jwt.refreshToken(), TokenType.REFRESH);

            assertThat(isLegalAccessToken).isTrue();
            assertThat(isLegalRefreshToken).isTrue();
        }
    }

    @Nested
    class signIn {
        @Test
        @DisplayName("이메일과 비밀번호가 올바르면 JWT 토큰을 발급한다")
        void issueJwtIfEmailAndPasswordAreCorrect() {
            // given
            String email = existUser.getEmail();
            String password = existUser.getPassword();

            // when
            TokenResponseDto jwt = authorizationService.signIn(new SignInRequestDto(email, password));

            // then
            boolean isLegalAccessToken = tokenProvider.validateToken(jwt.accessToken(), TokenType.ACCESS);
            boolean isLegalRefreshToken = tokenProvider.validateToken(jwt.refreshToken(), TokenType.REFRESH);

            assertThat(isLegalAccessToken).isTrue();
            assertThat(isLegalRefreshToken).isTrue();
        }

        @Test
        @DisplayName("리프레쉬 토큰을 새로 갱신한다")
        void updateRefreshToken() throws InterruptedException {
            // given
            String existRefreshToken = existUser.getRefreshToken();
            String email = existUser.getEmail();
            String password = existUser.getPassword();

            Thread.sleep(SLEEP_FOR_GET_DIFFERENT_TOKEN);

            // when
            authorizationService.signIn(new SignInRequestDto(email, password));

            // then
            String newRefreshToken = existUser.getRefreshToken();
            assertThat(newRefreshToken).isNotEqualTo(existRefreshToken);
        }
    }

    private EmailVerification createEntityFromFixture(EmailVerificationFixture fixture) {
        EmailVerification instance = fixture.getInstance();
        emailVerificationRepository.save(instance);

        return emailVerificationRepository.findByEmail(instance.getEmail())
                .orElseThrow();
    }

    private GoodSpaceUser createEntityFromFixture(GoodSpaceUserFixture fixture) {
        GoodSpaceUser user = fixture.getInstance();
        userRepository.save(user);

        String refreshToken = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshToken);

        return (GoodSpaceUser) userRepository.findById(user.getId())
                .orElseThrow();
    }
}
