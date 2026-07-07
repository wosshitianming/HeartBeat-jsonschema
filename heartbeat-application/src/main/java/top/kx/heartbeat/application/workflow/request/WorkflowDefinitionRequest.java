package top.kx.heartbeat.application.workflow.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class WorkflowDefinitionRequest {

    private String name;
    @JsonAlias({"definition_key", "key"})
    private String definitionKey;
    private Integer versionNo;
    private Object formSchema;
    private String bpmnXml;
}
