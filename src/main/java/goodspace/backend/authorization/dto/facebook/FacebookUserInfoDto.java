package goodspace.backend.authorization.dto.facebook;

import com.google.gson.annotations.SerializedName;
import goodspace.backend.domain.user.OAuthUser;
import lombok.Getter;

import static goodspace.backend.domain.user.OAuthType.FACEBOOK;

@Getter
public class FacebookUserInfoDto {
    @SerializedName("id")
    private String id;
    @SerializedName("email")
    private String email;

    public OAuthUser toEntity() {
        return OAuthUser.builder()
                .identifier(id)
                .email(email)
                .oauthType(FACEBOOK)
                .build();
    }
}
