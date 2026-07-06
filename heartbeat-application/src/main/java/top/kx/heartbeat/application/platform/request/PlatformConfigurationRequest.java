package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PlatformConfigurationRequest {

    @JsonAlias("key")
    private String configKey;
    @JsonAlias("name")
    private String configName;
    @JsonAlias("value")
    private String configValue;
    private String valueType;
    private Boolean encrypted;
    private String configGroup;
    private String description;
    private String status;
}
