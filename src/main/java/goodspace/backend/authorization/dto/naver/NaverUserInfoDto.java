package goodspace.backend.authorization.dto.naver;

import goodspace.backend.domain.user.OAuthUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static goodspace.backend.domain.user.OAuthType.NAVER;

/**
 * 참고 문서: <a href="https://developers.naver.com/docs/login/profile/profile.md">...</a>
 */
@Getter
@AllArgsConstructor
public class NaverUserInfoDto {
    private String resultCode;
    private String message;

    private Response response;

    @Getter
    @AllArgsConstructor
    static class Response {
        private String id;
        private String email;
    }

    public String getId() {
        return response.id;
    }

    public String getEmail() {
        return response.email;
    }

    public OAuthUser toEntity() {
            return OAuthUser.builder()
                    .identifier(response.id)
                    .email(response.email)
                    .oauthType(NAVER)
                    .build();
    }
}
