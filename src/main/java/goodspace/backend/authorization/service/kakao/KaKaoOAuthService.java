package goodspace.backend.authorization.service.kakao;

import com.google.gson.Gson;
import goodspace.backend.authorization.dto.kakao.KakaoAccessTokenDto;
import goodspace.backend.authorization.dto.kakao.KakaoUserInfoDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.authorization.service.OAuthService;
import goodspace.backend.domain.user.OAuthType;
import goodspace.backend.domain.user.User;
import goodspace.backend.repository.UserRepository;
import goodspace.backend.security.TokenProvider;
import goodspace.backend.security.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KaKaoOAuthService implements OAuthService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private static final String TOKEN_BASE_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_BASE_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String OAUTH_PAGE_REDIRECT_BASE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String SCOPE = "account_email";

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;

    public KaKaoOAuthService(
            UserRepository userRepository,
            TokenProvider tokenProvider,
            @Value("${keys.kakao.rest-api-key}") String clientId,
            @Value("${keys.kakao.client-secret}") String clientSecret,
            @Value("${keys.kakao.redirect-uri}") String redirectUri
    ) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;

        this.CLIENT_ID = clientId;
        this.CLIENT_SECRET = clientSecret;
        this.REDIRECT_URI = redirectUri;
    }

    @Override
    public String getAccessToken(String code) {
        HttpHeaders headers = getTokenHeaders();

        LinkedMultiValueMap<String, String> params = getTokenParams(code);
        ResponseEntity<String> response = sendTokenRequest(params, headers);

        if (isRequestFailed(response)) {
            log.warn(
                    "카카오로부터 엑세스토큰을 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("카카오로부터 엑세스토큰을 획득하지 못했습니다.");
        }

        return getAccessTokenFromResponse(response);
    }

    @Override
    @Transactional
    public TokenResponseDto signUpOrSignIn(String kakaoAccessToken) {
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        if (!kakaoUserInfo.isValidatedEmail()) {
            throw new IllegalStateException("이메일 인증이 되지 않은 유저입니다.");
        }

        User user = userRepository.findByIdentifierAndOAuthType(kakaoUserInfo.getId(), OAuthType.KAKAO)
                .orElseGet(() -> userRepository.save(kakaoUserInfo.toEntity()));

        String accessTokenValue = tokenProvider.createToken(user.getId(), TokenType.ACCESS);
        String refreshTokenValue = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshTokenValue);

        return new TokenResponseDto(accessTokenValue, refreshTokenValue);
    }

    @Override
    public String getOauthPageRedirectUrl() {
        return OAUTH_PAGE_REDIRECT_BASE_URL +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=code" +
                "&scope=" + SCOPE;
    }

    private HttpHeaders getTokenHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        return headers;
    }

    private LinkedMultiValueMap<String, String> getTokenParams(String code) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("client_id", CLIENT_ID);
        params.add("redirect_uri", REDIRECT_URI);
        params.add("client_secret", CLIENT_SECRET);

        return params;
    }

    private ResponseEntity<String> sendTokenRequest(LinkedMultiValueMap<String, String> params, HttpHeaders headers) {
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        return new RestTemplate().exchange(TOKEN_BASE_URL, HttpMethod.POST, httpEntity, String.class);
    }

    private String getAccessTokenFromResponse(ResponseEntity<String> responseEntity) {
        String json = responseEntity.getBody();

        return new Gson().fromJson(json, KakaoAccessTokenDto.class)
                .getAccessToken();
    }

    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = getUserInfoHeaders(accessToken);

        ResponseEntity<String> response = sendUserInfoRequest(headers);

        if (isRequestFailed(response)) {
            log.warn(
                    "카카오로부터 유저 정보를 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("카카오로부터 유저 정보를 획득하지 못했습니다.");
        }

        return getUserInfoFromResponse(response);
    }

    private HttpHeaders getUserInfoHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        return headers;
    }

    private ResponseEntity<String> sendUserInfoRequest(HttpHeaders headers) {
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        return new RestTemplate().exchange(USER_INFO_BASE_URL, HttpMethod.GET, httpEntity, String.class);
    }

    private boolean isRequestFailed(ResponseEntity<String> responseEntity) {
        return !responseEntity.getStatusCode().is2xxSuccessful();
    }

    private KakaoUserInfoDto getUserInfoFromResponse(ResponseEntity<String> responseEntity) {
        String json = responseEntity.getBody();
        Gson gson = new Gson();
        return gson.fromJson(json, KakaoUserInfoDto.class);
    }
}
