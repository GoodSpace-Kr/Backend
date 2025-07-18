package goodspace.backend.authorization.dto.google;

import com.google.gson.annotations.SerializedName;
import goodspace.backend.domain.user.OAuthUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static goodspace.backend.domain.user.OAuthType.GOOGLE;

@Getter
@AllArgsConstructor
public class GoogleUserInfoDto {
    private String id;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    @SerializedName("picture")
    private String pictureUrl;
    private String locale;

    public OAuthUser toEntity() {
        return OAuthUser.builder()
                .identifier(id)
                .email(email)
                .name(name)
                .oauthType(GOOGLE)
                .build();
    }
}
