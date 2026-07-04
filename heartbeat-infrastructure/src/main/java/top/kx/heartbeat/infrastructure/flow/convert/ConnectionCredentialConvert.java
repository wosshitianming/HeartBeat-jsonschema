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
    public abstract HbConnectionCredentialDOWithBLOBs toEntity(ConnectionCredential credential);

    public ConnectionCredential toMaskedDomain(HbConnectionCredentialDOWithBLOBs entity) {
        ConnectionCredential credential = toDomain(entity);
        credential.setSecrets(maskSecrets(credential.getSecrets()));
        return credential;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapJson(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("连接配置解析失败", ex);
        }
    }

    @Named("writeConfig")
    protected String writeConfig(Map<String, Object> config) {
        return writeJson(config);
    }

    @Named("writeSecrets")
    protected String writeSecrets(Map<String, Object> secrets) {
        Map<String, Object> encrypted = new LinkedHashMap<>();
        if (secrets != null) {
            for (Map.Entry<String, Object> entry : secrets.entrySet()) {
                encrypted.put(
                        entry.getKey(),
                        secretCryptoService.encryptIfPlain(String.valueOf(entry.getValue()))
                );
            }
        }
        return writeJson(encrypted);
    }

    protected LocalDateTime mapTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    protected Instant mapTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("连接配置序列化失败", ex);
        }
    }

    private Map<String, Object> maskSecrets(Map<String, Object> secrets) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (secrets != null) {
            for (Map.Entry<String, Object> entry : secrets.entrySet()) {
                result.put(entry.getKey(), secretCryptoService.mask(String.valueOf(entry.getValue())));
            }
        }
        return result;
    }
}
