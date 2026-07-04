package top.kx.heartbeat.infrastructure.auth.social;


import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractJsonSocialLoginHandler {

    @Resource(name = "socialRestTemplate")
    protected RestTemplate restTemplate;

    protected Map<String, Object> get(String url) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return body(response);
    }

    protected Map<String, Object> getBearer(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                Map.class
        );
        return body(response);
    }

    protected Map<String, Object> postJson(String url, Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );
        return body(response);
    }

    protected Map<String, Object> postForm(String url, Map<String, String> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        request.forEach(form::add);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                Map.class
        );
        return body(response);
    }

    protected String url(String base, Map<String, ?> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(base);
        query.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    protected String value(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    protected String config(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected void requireValue(String value, String field) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> body(ResponseEntity<Map> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("第三方登录服务返回空响应");
        }
        return new LinkedHashMap<>(response.getBody());
    }
}
