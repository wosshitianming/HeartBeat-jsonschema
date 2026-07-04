package top.kx.heartbeat.infrastructure.mp;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.mp.port.MpMenuSyncGateway;

import javax.annotation.Resource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WechatMpMenuSyncGateway implements MpMenuSyncGateway {

    private static final String WECHAT_MENU_CREATE_URL =
            "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=";

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> syncMenus(Map<String, Object> account, List<Map<String, Object>> menus) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", account.get("id"));
        result.put("syncedAt", Instant.now().toString());
        result.put("menus", menus);
        String accessToken = value(account.get("token"));
        if (StringUtils.isEmpty(accessToken)) {
            result.put("status", "WAITING_TOKEN");
            result.put("message", "公众号菜单已完成本地校验，请配置有效 access token 后执行真实同步");
            result.put("payload", buildWechatMenuPayload(menus));
            return result;
        }
        try {
            String response = postJson(WECHAT_MENU_CREATE_URL + accessToken, buildWechatMenuPayload(menus));
            result.put("status", "SYNCED");
            result.put("message", "微信菜单同步请求已提交");
            result.put("providerResponse", response);
            return result;
        } catch (Exception ex) {
            result.put("status", "FAILED");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private Map<String, Object> buildWechatMenuPayload(List<Map<String, Object>> menus) {
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> menu : menus) {
            if (StringUtils.isEmpty(value(menu.get("parentId")))) {
                roots.add(toWechatButton(menu, childrenOf(menus, value(menu.get("id")))));
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("button", roots);
        return payload;
    }

    private List<Map<String, Object>> childrenOf(List<Map<String, Object>> menus, String parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> menu : menus) {
            if (parentId.equals(value(menu.get("parentId")))) {
                result.add(menu);
            }
        }
        return result;
    }

    private Map<String, Object> toWechatButton(Map<String, Object> menu, List<Map<String, Object>> children) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("name", value(menu.get("name")));
        if (CollectionUtils.isNotEmpty(children)) {
            List<Map<String, Object>> subButtons = new ArrayList<>();
            for (Map<String, Object> child : children) {
                subButtons.add(toWechatButton(child, new ArrayList<>()));
            }
            button.put("sub_button", subButtons);
            return button;
        }
        String type = value(menu.get("menuType"));
        button.put("type", StringUtils.isEmpty(type) ? "view" : type);
        String url = value(menu.get("url"));
        if (StringUtils.isNotEmpty(url)) {
            button.put("url", url);
        }
        return button;
    }

    private String postJson(String url, Map<String, Object> payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        byte[] body = objectMapper.writeValueAsBytes(payload);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }
        java.io.InputStream inputStream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        byte[] bytes = readAll(inputStream);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readAll(java.io.InputStream inputStream) throws java.io.IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toByteArray();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
