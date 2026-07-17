package top.kx.heartbeat.infrastructure.persistence.mapper.flow;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

public interface FlowDefinitionRuntimeMapper {

    @Update("UPDATE hb_flow_version SET runtime_engine = #{runtimeEngine}, bpmn_xml = #{bpmnXml}, "
            + "bpmn_sha256 = #{bpmnSha256}, deployment_id = #{deploymentId}, "
            + "process_definition_id = #{processDefinitionId}, process_definition_key = #{processDefinitionKey}, "
            + "compile_status = #{compileStatus}, compile_error = #{compileError}, deployed_at = #{deployedAt}, "
            + "update_time = NOW() WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updateVersionRuntime(@Param("id") Long id,
                             @Param("tenantId") long tenantId,
                             @Param("runtimeEngine") String runtimeEngine,
                             @Param("bpmnXml") String bpmnXml,
                             @Param("bpmnSha256") String bpmnSha256,
                             @Param("deploymentId") String deploymentId,
                             @Param("processDefinitionId") String processDefinitionId,
                             @Param("processDefinitionKey") String processDefinitionKey,
                             @Param("compileStatus") String compileStatus,
                             @Param("compileError") String compileError,
                             @Param("deployedAt") Date deployedAt);

    @Update("UPDATE hb_flow_definition SET runtime_engine = #{runtimeEngine}, "
            + "active_deployment_id = #{activeDeploymentId}, "
            + "active_process_definition_id = #{activeProcessDefinitionId}, update_time = NOW() "
            + "WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updateActiveDeployment(@Param("id") Long id,
                               @Param("tenantId") long tenantId,
                               @Param("runtimeEngine") String runtimeEngine,
                               @Param("activeDeploymentId") String activeDeploymentId,
                               @Param("activeProcessDefinitionId") String activeProcessDefinitionId);
}
