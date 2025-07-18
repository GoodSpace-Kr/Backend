package goodspace.backend.authorization.dto.naver;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class NaverAccessTokenDto {
    @SerializedName("access_token")
    private String accessToken;
}
