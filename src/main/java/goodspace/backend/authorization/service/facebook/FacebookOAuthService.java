package goodspace.backend.authorization.service.facebook;

import com.google.gson.Gson;
import goodspace.backend.authorization.dto.facebook.FacebookAccessTokenDto;
import goodspace.backend.authorization.dto.facebook.FacebookUserInfoDto;
import goodspace.backend.authorization.dto.response.TokenResponseDto;
import goodspace.backend.authorization.service.OAuthService;
import goodspace.backend.domain.user.User;
import goodspace.backend.repository.UserRepository;
import goodspace.backend.security.TokenProvider;
import goodspace.backend.security.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import static goodspace.backend.domain.user.OAuthType.FACEBOOK;

@Service
@Slf4j
public class FacebookOAuthService implements OAuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private static final String TOKEN_BASE_URL = "https://graph.facebook.com/v14.0/oauth/access_token";
    private static final String USER_INFO_BASE_URL = "https://graph.facebook.com/me?fields=id,email";
    private static final String OAUTH_PAGE_REDIRECT_BASE_URL = "https://www.facebook.com/v14.0/dialog/oauth";
    private static final String SCOPE = "email";

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;

    public FacebookOAuthService(
            UserRepository userRepository,
            TokenProvider tokenProvider,
            @Value("${keys.facebook.client-id}") String clientId,
            @Value("${keys.facebook.client-secret}") String clientSecret,
            @Value("${keys.facebook.redirect-uri}") String redirectUri
    ) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.CLIENT_ID = clientId;
        this.CLIENT_SECRET = clientSecret;
        this.REDIRECT_URI = redirectUri;
    }

    @Override
    public String getAccessToken(String code) {
        String url = getTokenUrl(code);
        ResponseEntity<String> response = sendTokenRequest(url);

        if (isRequestFailed(response)) {
            log.warn(
                    "페이스북으로부터 엑세스 토큰을 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("페이스북 엑세스 토큰 획득 실패");
        }

        return getAccessTokenFromResponse(response);
    }

    @Override
    @Transactional
    public TokenResponseDto signUpOrSignIn(String facebookAccessToken) {
        FacebookUserInfoDto facebookUserInfo = getFacebookUserInfo(facebookAccessToken);

        User user = userRepository.findByIdentifierAndOAuthType(facebookUserInfo.getId(), FACEBOOK)
                .orElseGet(() -> userRepository.save(facebookUserInfo.toEntity()));

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

    private String getTokenUrl(String code) {
        return TOKEN_BASE_URL +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&client_secret=" + CLIENT_SECRET +
                "&code=" + code;
    }

    private ResponseEntity<String> sendTokenRequest(String url) {
        return new RestTemplate().getForEntity(url, String.class);
    }

    private String getAccessTokenFromResponse(ResponseEntity<String> response) {
        return new Gson().fromJson(response.getBody(), FacebookAccessTokenDto.class)
                .getAccessToken();
    }

    private FacebookUserInfoDto getFacebookUserInfo(String accessToken) {
        HttpHeaders headers = getUserInfoHeaders(accessToken);
        ResponseEntity<String> response = sendUserInfoRequest(headers);

        if (isRequestFailed(response)) {
            log.warn(
                    "페이스북으로부터 유저 정보를 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("페이스북 유저 정보 획득 실패");
        }

        return getUserInfoFromResponse(response);
    }

    private HttpHeaders getUserInfoHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private ResponseEntity<String> sendUserInfoRequest(HttpHeaders headers) {
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        return new RestTemplate().exchange(USER_INFO_BASE_URL, HttpMethod.GET, httpEntity, String.class);
    }

    private boolean isRequestFailed(ResponseEntity<String> responseEntity) {
        return !responseEntity.getStatusCode().is2xxSuccessful();
    }

    private FacebookUserInfoDto getUserInfoFromResponse(ResponseEntity<String> response) {
        return new Gson().fromJson(response.getBody(), FacebookUserInfoDto.class);
    }
}
