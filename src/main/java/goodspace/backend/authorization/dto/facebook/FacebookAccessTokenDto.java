package goodspace.backend.authorization.dto.facebook;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class FacebookAccessTokenDto {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("expires_in")
    private Long expiresIn;
}
