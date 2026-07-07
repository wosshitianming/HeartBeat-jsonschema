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
        // 创建当前流程需要的临时对象，承载后续处理数据。
        HttpHeaders headers = new HttpHeaders();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        request.forEach(form::add);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ResponseEntity<Map> response = restTemplate.exchange(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                url,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                HttpMethod.POST,
                // 创建当前流程需要的临时对象，承载后续处理数据。
                new HttpEntity<>(form, headers),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                Map.class
        );
        // 返回已经完成封装的业务结果。
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
