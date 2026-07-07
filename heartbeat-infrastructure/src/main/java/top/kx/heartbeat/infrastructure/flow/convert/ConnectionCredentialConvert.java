package top.kx.heartbeat.infrastructure.flow.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.flow.model.ConnectionCredential;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbConnectionCredentialDOWithBLOBs;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ConnectionCredentialConvert {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private SecretCryptoService secretCryptoService;

    @Mapping(target = "config", source = "configJson")
    @Mapping(target = "secrets", source = "secretJson")
    public abstract ConnectionCredential toDomain(HbConnectionCredentialDOWithBLOBs entity);

    @Mapping(target = "configJson", source = "config", qualifiedByName = "writeConfig")
    @Mapping(target = "secretJson", source = "secrets", qualifiedByName = "writeSecrets")
    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract HbConnectionCredentialDOWithBLOBs toEntity(ConnectionCredential credential);

    public ConnectionCredential toMaskedDomain(HbConnectionCredentialDOWithBLOBs entity) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ConnectionCredential credential = toDomain(entity);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        credential.setSecrets(maskSecrets(credential.getSecrets()));
        // 返回已经完成封装的业务结果。
        return credential;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapJson(String json) {
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isBlank(json)) {
            // 返回已经完成封装的业务结果。
            return new LinkedHashMap<>();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("连接配置解析失败", ex);
        }
    }

    @Named("writeConfig")
    protected String writeConfig(Map<String, Object> config) {
        return writeJson(config);
    }

    @Named("writeSecrets")
    protected String writeSecrets(Map<String, Object> secrets) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> encrypted = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (secrets != null) {
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map.Entry<String, Object> entry : secrets.entrySet()) {
                // 写入对外字段，保持调用方依赖的响应结构稳定。
                encrypted.put(
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        entry.getKey(),
                        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                        secretCryptoService.encryptIfPlain(String.valueOf(entry.getValue()))
                );
            }
        }
        // 返回已经完成封装的业务结果。
        return writeJson(encrypted);
    }

    protected LocalDateTime mapTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    protected Instant mapTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String writeJson(Map<String, Object> value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("连接配置序列化失败", ex);
        }
    }

    private Map<String, Object> maskSecrets(Map<String, Object> secrets) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (secrets != null) {
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map.Entry<String, Object> entry : secrets.entrySet()) {
                // 写入对外字段，保持调用方依赖的响应结构稳定。
                result.put(entry.getKey(), secretCryptoService.mask(String.valueOf(entry.getValue())));
            }
        }
        // 返回已经完成封装的业务结果。
        return result;
    }
}
