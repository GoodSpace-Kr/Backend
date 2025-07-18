package goodspace.backend.authorization.service.google;

import com.google.gson.Gson;
import goodspace.backend.authorization.dto.google.GoogleAccessTokenDto;
import goodspace.backend.authorization.dto.google.GoogleUserInfoDto;
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

import java.net.URI;
import java.util.Map;

import static goodspace.backend.domain.user.OAuthType.*;

@Service
@Slf4j
public class GoogleOAuthService implements OAuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private final String TOKEN_BASE_URL = "https://oauth2.googleapis.com/token";
    private final String USER_INFO_BASE_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private final String OAUTH_PAGE_REDIRECT_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private final String SCOPE = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;

    public GoogleOAuthService(
            UserRepository userRepository,
            TokenProvider tokenProvider,
            @Value("${keys.google.client-id}") String clientId,
            @Value("${keys.google.client-secret}") String clientSecret,
            @Value("${keys.google.redirect-uri}") String redirectUri
    ) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;

        this.CLIENT_ID = clientId;
        this.CLIENT_SECRET = clientSecret;
        this.REDIRECT_URI = redirectUri;
    }

    @Override
    public String getAccessToken(String code) {
        Map<String, String> params = getTokenParams(code);
        ResponseEntity<String> response = sendTokenRequest(params);

        if (isRequestFailed(response)) {
            log.warn(
                    "구글로부터 엑세스 토큰을 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("구글 엑세스 토큰 획득 실패");
        }

        return getAccessTokenFromResponse(response);
    }

    @Override
    @Transactional
    public TokenResponseDto signUpOrSignIn(String googleAccessToken) {
        GoogleUserInfoDto googleUserInfo = getGoogleUserInfo(googleAccessToken);

        User user = userRepository.findByIdentifierAndOAuthType(googleUserInfo.getId(), GOOGLE)
                .orElseGet(() -> userRepository.save(googleUserInfo.toEntity()));

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

    private Map<String, String> getTokenParams(String code) {
        return Map.of(
                "code", code,
                "scope", SCOPE,
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET,
                "redirect_uri", REDIRECT_URI,
                "grant_type", "authorization_code"
        );
    }

    private ResponseEntity<String> sendTokenRequest(Map<String, String> params) {
        return new RestTemplate()
                .postForEntity(TOKEN_BASE_URL, params, String.class);
    }

    private String getAccessTokenFromResponse(ResponseEntity<String> responseEntity) {
        String json = responseEntity.getBody();

        return new Gson().fromJson(json, GoogleAccessTokenDto.class)
                .getAccessToken();
    }

    private GoogleUserInfoDto getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = getUserInfoHeaders(accessToken);
        String url = getUserInfoUrl(accessToken);

        ResponseEntity<String> response = sendUserInfoRequest(headers, url);

        if (isRequestFailed(response)) {
            log.warn(
                    "구글로부터 회원 정보를 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("구글 회원 정보 획득 실패");
        }

        return getUserInfoFromResponse(response);
    }

    private static ResponseEntity<String> sendUserInfoRequest(HttpHeaders headers, String url) {
        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(url));

        return new RestTemplate().exchange(requestEntity, String.class);
    }

    private HttpHeaders getUserInfoHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String getUserInfoUrl(String accessToken) {
        return USER_INFO_BASE_URL +
                "?access_token=" + accessToken;
    }

    private GoogleUserInfoDto getUserInfoFromResponse(ResponseEntity<String> responseEntity) {
        String json = responseEntity.getBody();

        return new Gson().fromJson(json, GoogleUserInfoDto.class);
    }

    private boolean isRequestFailed(ResponseEntity<String> responseEntity) {
        return !responseEntity.getStatusCode().is2xxSuccessful();
    }
}
