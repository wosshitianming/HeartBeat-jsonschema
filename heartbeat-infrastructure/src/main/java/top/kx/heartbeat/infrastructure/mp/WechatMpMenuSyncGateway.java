package top.kx.heartbeat.infrastructure.mp;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.common.response.RecordResponse;
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
    public RecordResponse syncMenus(Map<String, Object> account, List<Map<String, Object>> menus) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("accountId", account.get("id"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("syncedAt", Instant.now().toString());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("menus", menus);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = value(account.get("token"));
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(accessToken)) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("status", "WAITING_TOKEN");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("message", "公众号菜单已完成本地校验，请配置有效 access token 后执行真实同步");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("payload", buildWechatMenuPayload(menus));
            // 返回已经完成封装的业务结果。
            return RecordResponse.from(result);
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String response = postJson(WECHAT_MENU_CREATE_URL + accessToken, buildWechatMenuPayload(menus));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("status", "SYNCED");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("message", "微信菜单同步请求已提交");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("providerResponse", response);
            // 返回已经完成封装的业务结果。
            return RecordResponse.from(result);
        } catch (Exception ex) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("status", "FAILED");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("message", ex.getMessage());
            // 返回已经完成封装的业务结果。
            return RecordResponse.from(result);
        }
    }

    private Map<String, Object> buildWechatMenuPayload(List<Map<String, Object>> menus) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> roots = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map<String, Object> menu : menus) {
            // 校验关键文本参数，防止无效输入继续向后流转。
            if (StringUtils.isEmpty(value(menu.get("parentId")))) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                roots.add(toWechatButton(menu, childrenOf(menus, value(menu.get("id")))));
            }
        }
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> payload = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        payload.put("button", roots);
        // 返回已经完成封装的业务结果。
        return payload;
    }

    private List<Map<String, Object>> childrenOf(List<Map<String, Object>> menus, String parentId) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> result = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map<String, Object> menu : menus) {
            // 比对当前业务状态，决定是否进入该处理分支。
            if (parentId.equals(value(menu.get("parentId")))) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                result.add(menu);
            }
        }
        // 返回已经完成封装的业务结果。
        return result;
    }

    private Map<String, Object> toWechatButton(Map<String, Object> menu, List<Map<String, Object>> children) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> button = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        button.put("name", value(menu.get("name")));
        // 根据当前业务条件选择对应处理路径。
        if (CollectionUtils.isNotEmpty(children)) {
            // 创建结果集合，承接后续逐项组装的数据。
            List<Map<String, Object>> subButtons = new ArrayList<>();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map<String, Object> child : children) {
                // 创建结果集合，承接后续逐项组装的数据。
                subButtons.add(toWechatButton(child, new ArrayList<>()));
            }
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            button.put("sub_button", subButtons);
            // 返回已经完成封装的业务结果。
            return button;
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String type = value(menu.get("menuType"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        button.put("type", StringUtils.isEmpty(type) ? "view" : type);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String url = value(menu.get("url"));
        // 根据当前业务条件选择对应处理路径。
        if (StringUtils.isNotEmpty(url)) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            button.put("url", url);
        }
        // 返回已经完成封装的业务结果。
        return button;
    }

    private String postJson(String url, Map<String, Object> payload) throws Exception {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        connection.setRequestMethod("POST");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        connection.setConnectTimeout(5000);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        connection.setReadTimeout(10000);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        connection.setDoOutput(true);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        // 读取扩展参数载体，为后续动态处理准备数据。
        byte[] body = objectMapper.writeValueAsBytes(payload);
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try (OutputStream outputStream = connection.getOutputStream()) {
            // 写入 ZIP 条目，保证下载包包含当前生成文件。
            outputStream.write(body);
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
        java.io.InputStream inputStream = connection.getResponseCode() >= 400
                // 条件成立时使用前一个分支计算出的业务值。
                ? connection.getErrorStream()
                // 条件不成立时使用兜底业务值。
                : connection.getInputStream();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        byte[] bytes = readAll(inputStream);
        // 返回已经完成封装的业务结果。
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readAll(java.io.InputStream inputStream) throws java.io.IOException {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (inputStream == null) {
            // 返回已经完成封装的业务结果。
            return new byte[0];
        }
        // 创建当前流程需要的临时对象，承载后续处理数据。
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        // 创建当前流程需要的临时对象，承载后续处理数据。
        byte[] buffer = new byte[1024];
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        int len;
        // 持续读取可用数据，直到当前数据源处理完成。
        while ((len = inputStream.read(buffer)) != -1) {
            // 写入 ZIP 条目，保证下载包包含当前生成文件。
            outputStream.write(buffer, 0, len);
        }
        // 返回已经完成封装的业务结果。
        return outputStream.toByteArray();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
