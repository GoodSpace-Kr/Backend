package goodspace.backend.authorization.service.naver;

import com.google.gson.Gson;
import goodspace.backend.authorization.dto.naver.NaverAccessTokenDto;
import goodspace.backend.authorization.dto.naver.NaverUserInfoDto;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static goodspace.backend.domain.user.OAuthType.NAVER;

@Service
@Slf4j
public class NaverOAuthService implements OAuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private static final String TOKEN_BASE_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_BASE_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String OAUTH_PAGE_REDIRECTION_BASE_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String SCOPE = "email";

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;

    public NaverOAuthService(
            UserRepository userRepository,
            TokenProvider tokenProvider,
            @Value("${keys.naver.client-id}") String clientId,
            @Value("${keys.naver.client-secret}") String clientSecret,
            @Value("${keys.naver.redirect-uri}") String redirectUri
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
        MultiValueMap<String, String> params = getTokenParams(code);

        ResponseEntity<String> response = sendTokenRequest(params, headers);

        if (isRequestFailed(response)) {
            log.warn(
                    "네이버로부터 엑세스토큰을 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("네이버로부터 엑세스토큰을 획득하지 못했습니다.");
        }

        return getTokenFromResponse(response);
    }

    @Override
    @Transactional
    public TokenResponseDto signUpOrSignIn(String naverAccessToken) {
        NaverUserInfoDto naverUserInfo = getNaverUserInfo(naverAccessToken);

        User user = userRepository.findByIdentifierAndOAuthType(naverUserInfo.getId(), NAVER)
                .orElseGet(() -> userRepository.save(naverUserInfo.toEntity()));

        String accessTokenValue = tokenProvider.createToken(user.getId(), TokenType.ACCESS);
        String refreshTokenValue = tokenProvider.createToken(user.getId(), TokenType.REFRESH);
        user.updateRefreshToken(refreshTokenValue);

        return new TokenResponseDto(accessTokenValue, refreshTokenValue);
    }

    @Override
    public String getOauthPageRedirectUrl() {
        return OAUTH_PAGE_REDIRECTION_BASE_URL +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=code" +
                "&scope=" + SCOPE;
    }

    private HttpHeaders getTokenHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> getTokenParams(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", CLIENT_ID);
        params.add("client_secret", CLIENT_SECRET);
        params.add("code", code);
        params.add("redirect_uri", REDIRECT_URI);

        return params;
    }

    private ResponseEntity<String> sendTokenRequest(MultiValueMap<String, String> params, HttpHeaders headers) {
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return new RestTemplate().exchange(TOKEN_BASE_URL, HttpMethod.POST, request, String.class);
    }

    private String getTokenFromResponse(ResponseEntity<String> response) {
        return new Gson().fromJson(response.getBody(), NaverAccessTokenDto.class)
                .getAccessToken();
    }

    private NaverUserInfoDto getNaverUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(USER_INFO_BASE_URL, HttpMethod.GET, entity, String.class);

        if (isRequestFailed(response)) {
            log.warn(
                    "네이버로부터 회원 정보를 획득하지 못했습니다. 상태코드 {}: {}",
                    response.getStatusCode(),
                    response.getBody()
            );
            throw new RuntimeException("네이버로부터 회원 정보를 획득하지 못했습니다.");
        }

        return getUserInfoFromResponse(response);
    }

    private NaverUserInfoDto getUserInfoFromResponse(ResponseEntity<String> response) {
        return new Gson().fromJson(response.getBody(), NaverUserInfoDto.class);
    }

    private boolean isRequestFailed(ResponseEntity<String> response) {
        return !response.getStatusCode().is2xxSuccessful();
    }
}
