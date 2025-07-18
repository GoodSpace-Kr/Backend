package goodspace.backend.authorization.dto.kakao;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class KakaoAccessTokenDto {
    @SerializedName("access_token")
    private String accessToken;
}
