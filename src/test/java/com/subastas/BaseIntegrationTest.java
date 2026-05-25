package com.subastas;

import com.subastas.model.dto.request.LoginRequest;
import com.subastas.model.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Autowired
    protected TestRestTemplate rest;

    @LocalServerPort
    protected int port;

    /**
     * RestTemplate que soporta PATCH y no lanza excepciones en 4xx/5xx.
     * Usa JDK HttpClient (soporta PATCH, a diferencia de HttpURLConnection).
     */
    private RestTemplate rawRest() {
        RestTemplate rt = new RestTemplate(new JdkClientHttpRequestFactory());
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });
        return rt;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    protected String loginAndGetToken(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        ResponseEntity<LoginResponse> res = rest.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(req, jsonHeaders()), LoginResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody().getTokenAcceso();
    }

    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected HttpHeaders authHeaders(String jwt) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(jwt);
        return headers;
    }

    protected <T> ResponseEntity<T> getWithAuth(String url, String jwt, Class<T> type) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> postWithAuth(String url, String jwt, Object body, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> patchWithAuth(String url, String jwt, Object body, Class<T> type) {
        return rawRest().exchange(baseUrl(url), HttpMethod.PATCH, new HttpEntity<>(body, authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> deleteWithAuth(String url, String jwt, Class<T> type) {
        return rest.exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> postNoAuth(String url, Object body, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), type);
    }

    /**
     * POST sin auth usando JDK HttpClient (evita problemas de HttpURLConnection con 401).
     */
    protected <T> ResponseEntity<T> postNoAuthRaw(String url, Object body, Class<T> type) {
        return rawRest().exchange(baseUrl(url), HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), type);
    }

    protected <T> ResponseEntity<T> getNoAuth(String url, Class<T> type) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), type);
    }

    protected <T> ResponseEntity<T> getWithAuth(String url, String jwt, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> postWithAuth(String url, String jwt, Object body, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> patchWithAuth(String url, String jwt, Object body, ParameterizedTypeReference<T> type) {
        return rawRest().exchange(baseUrl(url), HttpMethod.PATCH, new HttpEntity<>(body, authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> deleteWithAuth(String url, String jwt, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders(jwt)), type);
    }

    protected <T> ResponseEntity<T> postNoAuth(String url, Object body, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), type);
    }

    protected <T> ResponseEntity<T> postNoAuthRaw(String url, Object body, ParameterizedTypeReference<T> type) {
        return rawRest().exchange(baseUrl(url), HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), type);
    }

    protected <T> ResponseEntity<T> getNoAuth(String url, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), type);
    }
}
