from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTITY_GEN = ROOT / "heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/gen"
MAPPER_GEN = ROOT / "heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/gen"
XML_GEN = ROOT / "heartbeat-infrastructure/src/main/resources/mapper-xml/gen"


def field(column, prop, java_type, jdbc_type):
    return {"column": column, "prop": prop, "type": java_type, "jdbc": jdbc_type}


COMMON_AUDIT = [
    field("status", "status", "String", "VARCHAR"),
    field("version", "version", "Integer", "INTEGER"),
    field("delete_marker", "deleteMarker", "Long", "BIGINT"),
    field("created_by", "createBy", "Long", "BIGINT"),
    field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
    field("updated_by", "updateBy", "Long", "BIGINT"),
    field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
]

TABLES = [
    {
        "table": "structure_definition",
        "class": "StructureDefinitionDO",
        "base": None,
        "criteria": ["id", "tenantId", "status"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("name", "name", "String", "VARCHAR"),
            field("description", "description", "String", "VARCHAR"),
            field("active_version_no", "activeVersionNo", "Integer", "INTEGER"),
            field("status", "status", "String", "VARCHAR"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("created_by", "createBy", "Long", "BIGINT"),
            field("updated_by", "updateBy", "Long", "BIGINT"),
        ],
    },
    {
        "table": "structure_draft",
        "class": "StructureDraftDO",
        "base": None,
        "criteria": ["id", "tenantId", "definitionId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("definition_id", "definitionId", "Long", "BIGINT"),
            field("structure_model", "structureModel", "String", "LONGVARCHAR"),
            field("generation_config", "generationConfig", "String", "LONGVARCHAR"),
            field("field_overrides", "fieldOverrides", "String", "LONGVARCHAR"),
            field("artifacts", "artifacts", "String", "LONGVARCHAR"),
            field("warnings", "warnings", "String", "LONGVARCHAR"),
            field("validation_mode", "validationMode", "String", "VARCHAR"),
            field("sample_digest", "sampleDigest", "String", "VARCHAR"),
            field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "structure_version",
        "class": "StructureVersionDO",
        "base": None,
        "criteria": ["id", "tenantId", "definitionId", "versionNo"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("definition_id", "definitionId", "Long", "BIGINT"),
            field("version_no", "versionNo", "Integer", "INTEGER"),
            field("structure_model", "structureModel", "String", "LONGVARCHAR"),
            field("generation_config", "generationConfig", "String", "LONGVARCHAR"),
            field("field_overrides", "fieldOverrides", "String", "LONGVARCHAR"),
            field("artifacts", "artifacts", "String", "LONGVARCHAR"),
            field("warnings", "warnings", "String", "LONGVARCHAR"),
            field("validation_mode", "validationMode", "String", "VARCHAR"),
            field("sample_digest", "sampleDigest", "String", "VARCHAR"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "structure_artifact",
        "class": "StructureArtifactDO",
        "base": None,
        "criteria": ["id", "tenantId", "definitionId", "versionId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("definition_id", "definitionId", "Long", "BIGINT"),
            field("version_id", "versionId", "Long", "BIGINT"),
            field("artifact_key", "artifactKey", "String", "VARCHAR"),
            field("artifact_json", "artifactJson", "String", "LONGVARCHAR"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_tenant_plan",
        "class": "SysTenantPlanDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysTenantPlanEntity",
        "criteria": ["id", "planCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("plan_code", "planCode", "String", "VARCHAR"),
            field("plan_name", "planName", "String", "VARCHAR"),
            field("plan_type", "planType", "String", "VARCHAR"),
            field("description", "description", "String", "VARCHAR"),
            field("max_user_count", "maxUserCount", "Integer", "INTEGER"),
            field("max_storage_mb", "maxStorageMb", "Long", "BIGINT"),
            field("feature_policy", "featurePolicy", "String", "LONGVARCHAR"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_tenant",
        "class": "SysTenantDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysTenantEntity",
        "criteria": ["id", "tenantCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("plan_id", "planId", "Long", "BIGINT"),
            field("tenant_code", "tenantCode", "String", "VARCHAR"),
            field("tenant_name", "tenantName", "String", "VARCHAR"),
            field("tenant_type", "tenantType", "String", "VARCHAR"),
            field("domain", "domain", "String", "VARCHAR"),
            field("contact_name", "contactName", "String", "VARCHAR"),
            field("contact_phone", "contactPhone", "String", "VARCHAR"),
            field("contact_email", "contactEmail", "String", "VARCHAR"),
            field("logo_url", "logoUrl", "String", "VARCHAR"),
            field("timezone", "timezone", "String", "VARCHAR"),
            field("locale", "locale", "String", "VARCHAR"),
            field("expire_at", "expireAt", "java.time.LocalDateTime", "TIMESTAMP"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_dept",
        "class": "SysDeptDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysDeptEntity",
        "criteria": ["id", "tenantId", "deptCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("parent_id", "parentId", "Long", "BIGINT"),
            field("dept_code", "deptCode", "String", "VARCHAR"),
            field("dept_name", "deptName", "String", "VARCHAR"),
            field("ancestors", "ancestors", "String", "VARCHAR"),
            field("dept_level", "deptLevel", "Integer", "INTEGER"),
            field("leader_user_id", "leaderUserId", "Long", "BIGINT"),
            field("phone", "phone", "String", "VARCHAR"),
            field("email", "email", "String", "VARCHAR"),
            field("sort_no", "sortNo", "Integer", "INTEGER"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_user",
        "class": "SysUserDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysUserEntity",
        "criteria": ["id", "tenantId", "username", "email", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("dept_id", "deptId", "Long", "BIGINT"),
            field("username", "username", "String", "VARCHAR"),
            field("nickname", "nickname", "String", "VARCHAR"),
            field("real_name", "realName", "String", "VARCHAR"),
            field("email", "email", "String", "VARCHAR"),
            field("phone", "phone", "String", "VARCHAR"),
            field("avatar_url", "avatarUrl", "String", "VARCHAR"),
            field("password_hash", "passwordHash", "String", "VARCHAR"),
            field("password_algo", "passwordAlgo", "String", "VARCHAR"),
            field("password_updated_at", "passwordUpdateTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("user_type", "userType", "String", "VARCHAR"),
            field("status", "status", "String", "VARCHAR"),
            field("last_login_at", "lastLoginAt", "java.time.LocalDateTime", "TIMESTAMP"),
            field("last_login_ip", "lastLoginIp", "String", "VARCHAR"),
            field("version", "version", "Integer", "INTEGER"),
            field("delete_marker", "deleteMarker", "Long", "BIGINT"),
            field("created_by", "createBy", "Long", "BIGINT"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("updated_by", "updateBy", "Long", "BIGINT"),
            field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_role",
        "class": "SysRoleDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysRoleEntity",
        "criteria": ["id", "tenantId", "roleCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("role_code", "roleCode", "String", "VARCHAR"),
            field("role_name", "roleName", "String", "VARCHAR"),
            field("role_type", "roleType", "String", "VARCHAR"),
            field("data_scope", "dataScope", "String", "VARCHAR"),
            field("description", "description", "String", "VARCHAR"),
            field("sort_no", "sortNo", "Integer", "INTEGER"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_permission",
        "class": "SysPermissionDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysPermissionEntity",
        "criteria": ["id", "tenantId", "permissionCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("permission_code", "permissionCode", "String", "VARCHAR"),
            field("permission_name", "permissionName", "String", "VARCHAR"),
            field("permission_type", "permissionType", "String", "VARCHAR"),
            field("resource_type", "resourceType", "String", "VARCHAR"),
            field("resource_path", "resourcePath", "String", "VARCHAR"),
            field("http_method", "httpMethod", "String", "VARCHAR"),
            field("description", "description", "String", "VARCHAR"),
            field("sort_no", "sortNo", "Integer", "INTEGER"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_menu",
        "class": "SysMenuDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysMenuEntity",
        "criteria": ["id", "tenantId", "menuCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("parent_id", "parentId", "Long", "BIGINT"),
            field("menu_code", "menuCode", "String", "VARCHAR"),
            field("menu_name", "menuName", "String", "VARCHAR"),
            field("menu_type", "menuType", "String", "VARCHAR"),
            field("route_path", "routePath", "String", "VARCHAR"),
            field("component_path", "componentPath", "String", "VARCHAR"),
            field("redirect_path", "redirectPath", "String", "VARCHAR"),
            field("icon", "icon", "String", "VARCHAR"),
            field("visible", "visible", "Boolean", "BOOLEAN"),
            field("keep_alive", "keepAlive", "Boolean", "BOOLEAN"),
            field("external_link", "externalLink", "String", "VARCHAR"),
            field("permission_mode", "permissionMode", "String", "VARCHAR"),
            field("sort_no", "sortNo", "Integer", "INTEGER"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_user_role",
        "class": "SysUserRoleDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysUserRoleEntity",
        "criteria": ["id", "tenantId", "userId", "roleId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("user_id", "userId", "Long", "BIGINT"),
            field("role_id", "roleId", "Long", "BIGINT"),
            field("created_by", "createdBy", "Long", "BIGINT"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_role_permission",
        "class": "SysRolePermissionDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysRolePermissionEntity",
        "criteria": ["id", "tenantId", "roleId", "permissionId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("role_id", "roleId", "Long", "BIGINT"),
            field("permission_id", "permissionId", "Long", "BIGINT"),
            field("created_by", "createdBy", "Long", "BIGINT"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_menu_permission",
        "class": "SysMenuPermissionDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysMenuPermissionEntity",
        "criteria": ["id", "tenantId", "menuId", "permissionId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("menu_id", "menuId", "Long", "BIGINT"),
            field("permission_id", "permissionId", "Long", "BIGINT"),
            field("created_by", "createdBy", "Long", "BIGINT"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_config",
        "class": "SysConfigDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysConfigEntity",
        "criteria": ["id", "tenantId", "configKey", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("config_key", "configKey", "String", "VARCHAR"),
            field("config_name", "configName", "String", "VARCHAR"),
            field("config_value", "configValue", "String", "LONGVARCHAR"),
            field("value_type", "valueType", "String", "VARCHAR"),
            field("encrypted", "encrypted", "Boolean", "BOOLEAN"),
            field("config_group", "configGroup", "String", "VARCHAR"),
            field("description", "description", "String", "VARCHAR"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "auth_social_provider",
        "class": "AuthSocialProviderDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.platform.AuthSocialProviderEntity",
        "criteria": ["id", "tenantId", "providerCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("provider_code", "providerCode", "String", "VARCHAR"),
            field("provider_name", "providerName", "String", "VARCHAR"),
            field("provider_type", "providerType", "String", "VARCHAR"),
            field("client_id", "clientId", "String", "VARCHAR"),
            field("app_key", "appKey", "String", "VARCHAR"),
            field("app_secret_cipher", "appSecretCipher", "String", "VARCHAR"),
            field("authorize_url", "authorizeUrl", "String", "VARCHAR"),
            field("token_url", "tokenUrl", "String", "VARCHAR"),
            field("user_info_url", "userInfoUrl", "String", "VARCHAR"),
            field("scopes", "scopes", "String", "VARCHAR"),
            field("enabled", "enabled", "Boolean", "BOOLEAN"),
        ] + COMMON_AUDIT,
    },
    {
        "table": "sys_job",
        "class": "SysJobDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.tool.SysJobEntity",
        "criteria": ["id", "tenantId", "jobCode", "deleteMarker"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("job_code", "jobCode", "String", "VARCHAR"),
            field("job_name", "jobName", "String", "VARCHAR"),
            field("job_group", "jobGroup", "String", "VARCHAR"),
            field("invoke_target", "invokeTarget", "String", "VARCHAR"),
            field("cron_expression", "cronExpression", "String", "VARCHAR"),
            field("misfire_policy", "misfirePolicy", "String", "VARCHAR"),
            field("concurrent", "concurrent", "Boolean", "BOOLEAN"),
            field("status", "status", "String", "VARCHAR"),
            field("version", "version", "Integer", "INTEGER"),
            field("delete_marker", "deleteMarker", "Long", "BIGINT"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "flow_wait_state",
        "class": "FlowWaitStateDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.event.FlowWaitStateEntity",
        "criteria": ["id", "tenantId", "correlationKey"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("run_id", "runId", "Long", "BIGINT"),
            field("node_id", "nodeId", "String", "VARCHAR"),
            field("correlation_key", "correlationKey", "String", "VARCHAR"),
            field("status", "status", "String", "VARCHAR"),
            field("payload_json", "payloadJson", "String", "LONGVARCHAR"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("updated_at", "updateTime", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_outbox_event",
        "class": "SysOutboxEventDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.event.SysOutboxEventEntity",
        "criteria": ["id", "eventId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("event_id", "eventId", "String", "VARCHAR"),
            field("event_type", "eventType", "String", "VARCHAR"),
            field("aggregate_type", "aggregateType", "String", "VARCHAR"),
            field("aggregate_id", "aggregateId", "String", "VARCHAR"),
            field("payload_json", "payloadJson", "String", "LONGVARCHAR"),
            field("status", "status", "String", "VARCHAR"),
            field("created_at", "createTime", "java.time.LocalDateTime", "TIMESTAMP"),
            field("published_at", "publishedAt", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
    {
        "table": "sys_inbox_event",
        "class": "SysInboxEventDO",
        "base": "top.kx.heartbeat.infrastructure.persistence.entity.event.SysInboxEventEntity",
        "criteria": ["id", "consumerCode", "eventId"],
        "fields": [
            field("id", "id", "Long", "BIGINT"),
            field("tenant_id", "tenantId", "Long", "BIGINT"),
            field("consumer_code", "consumerCode", "String", "VARCHAR"),
            field("event_id", "eventId", "String", "VARCHAR"),
            field("status", "status", "String", "VARCHAR"),
            field("processed_at", "processedAt", "java.time.LocalDateTime", "TIMESTAMP"),
        ],
    },
]


def method_suffix(prop):
    return prop[0].upper() + prop[1:]


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def gen_do(table):
    class_name = table["class"]
    if table["base"]:
        return (
            "package top.kx.heartbeat.infrastructure.persistence.entity.gen;\n\n"
            f"public class {class_name} extends {table['base']} {{\n"
            "}\n"
        )
    lines = ["package top.kx.heartbeat.infrastructure.persistence.entity.gen;", "", f"public class {class_name} {{", ""]
    for f in table["fields"]:
        lines.append(f"    private {f['type']} {f['prop']};")
    lines.append("")
    for f in table["fields"]:
        suffix = method_suffix(f["prop"])
        lines.append(f"    public {f['type']} get{suffix}() {{ return {f['prop']}; }}")
        lines.append(f"    public void set{suffix}({f['type']} {f['prop']}) {{ this.{f['prop']} = {f['prop']}; }}")
    lines.append("}")
    return "\n".join(lines) + "\n"


def gen_example(table):
    class_name = table["class"] + "Example"
    field_by_prop = {f["prop"]: f for f in table["fields"]}
    lines = [
        "package top.kx.heartbeat.infrastructure.persistence.entity.gen;",
        "",
        "import java.util.ArrayList;",
        "import java.util.List;",
        "",
        f"public class {class_name} {{",
        "",
        "    protected String orderByClause;",
        "    protected boolean distinct;",
        "    protected List<Criteria> oredCriteria;",
        "",
        f"    public {class_name}() {{",
        "        oredCriteria = new ArrayList<>();",
        "    }",
        "",
        "    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }",
        "    public String getOrderByClause() { return orderByClause; }",
        "    public void setDistinct(boolean distinct) { this.distinct = distinct; }",
        "    public boolean isDistinct() { return distinct; }",
        "    public List<Criteria> getOredCriteria() { return oredCriteria; }",
        "",
        "    public Criteria createCriteria() {",
        "        Criteria criteria = createCriteriaInternal();",
        "        if (oredCriteria.size() == 0) {",
        "            oredCriteria.add(criteria);",
        "        }",
        "        return criteria;",
        "    }",
        "",
        "    protected Criteria createCriteriaInternal() { return new Criteria(); }",
        "",
        "    public void clear() {",
        "        oredCriteria.clear();",
        "        orderByClause = null;",
        "        distinct = false;",
        "    }",
        "",
        "    public static class Criteria {",
        "        protected List<Criterion> criteria = new ArrayList<>();",
        "",
        "        public boolean isValid() { return criteria.size() > 0; }",
        "        public List<Criterion> getAllCriteria() { return criteria; }",
        "",
        "        protected void addCriterion(String condition) {",
        "            if (condition == null) { throw new IllegalArgumentException(\"Value for condition cannot be null\"); }",
        "            criteria.add(new Criterion(condition));",
        "        }",
        "",
        "        protected void addCriterion(String condition, Object value, String property) {",
        "            if (value == null) { throw new IllegalArgumentException(\"Value for \" + property + \" cannot be null\"); }",
        "            criteria.add(new Criterion(condition, value));",
        "        }",
        "",
    ]
    for prop in table["criteria"]:
        f = field_by_prop[prop]
        suffix = method_suffix(prop)
        lines.append(
            f"        public Criteria and{suffix}EqualTo({f['type']} value) "
            f"{{ addCriterion(\"{f['column']} =\", value, \"{prop}\"); return this; }}"
        )
    lines += [
        "    }",
        "",
        "    public static class Criterion {",
        "        private String condition;",
        "        private Object value;",
        "        private boolean noValue;",
        "        private boolean singleValue;",
        "",
        "        protected Criterion(String condition) {",
        "            this.condition = condition;",
        "            this.noValue = true;",
        "        }",
        "",
        "        protected Criterion(String condition, Object value) {",
        "            this.condition = condition;",
        "            this.value = value;",
        "            this.singleValue = true;",
        "        }",
        "",
        "        public String getCondition() { return condition; }",
        "        public Object getValue() { return value; }",
        "        public boolean isNoValue() { return noValue; }",
        "        public boolean isSingleValue() { return singleValue; }",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def gen_mapper(table):
    class_name = table["class"]
    mapper = class_name.replace("DO", "DOMapper")
    example = class_name + "Example"
    return f"""package top.kx.heartbeat.infrastructure.persistence.mapper.gen;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import top.kx.heartbeat.infrastructure.persistence.entity.gen.{class_name};
import top.kx.heartbeat.infrastructure.persistence.entity.gen.{example};

public interface {mapper} {{
    long countByExample({example} example);
    int deleteByExample({example} example);
    int deleteByPrimaryKey(@Param("id") Long id);
    int insert({class_name} record);
    int insertSelective({class_name} record);
    List<{class_name}> selectByExample({example} example);
    {class_name} selectByPrimaryKey(@Param("id") Long id);
    int updateByPrimaryKeySelective({class_name} record);
    int updateByPrimaryKey({class_name} record);
}}
"""


def if_not_null(f):
    return f'<if test="{f["prop"]} != null">'


def gen_xml(table):
    class_name = table["class"]
    mapper = class_name.replace("DO", "DOMapper")
    ns = f"top.kx.heartbeat.infrastructure.persistence.mapper.gen.{mapper}"
    typ = f"top.kx.heartbeat.infrastructure.persistence.entity.gen.{class_name}"
    result_lines = []
    for i, f in enumerate(table["fields"]):
        tag = "id" if f["prop"] == "id" else "result"
        result_lines.append(f'        <{tag} column="{f["column"]}" property="{f["prop"]}" jdbcType="{f["jdbc"]}"/>')
    columns = ", ".join(f["column"] for f in table["fields"])
    values = ", ".join(f"#{{{f['prop']}}}" for f in table["fields"])
    insert_columns = "".join(f'{if_not_null(f)}{f["column"]},</if>' for f in table["fields"])
    insert_values = "".join(f'{if_not_null(f)}}#{{{f["prop"]}}},</if>' for f in table["fields"])
    update_set = "".join(
        f'{if_not_null(f)}{f["column"]} = #{{{f["prop"]}}},</if>'
        for f in table["fields"]
        if f["prop"] != "id"
    )
    update_all = ", ".join(f'{f["column"]} = #{{{f["prop"]}}}' for f in table["fields"] if f["prop"] != "id")
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{ns}">
    <resultMap id="BaseResultMap" type="{typ}">
{chr(10).join(result_lines)}
    </resultMap>
    <sql id="Base_Column_List">{columns}</sql>
    <sql id="Example_Where_Clause">
        <where>
            <foreach collection="oredCriteria" item="criteria" separator="or">
                <if test="criteria.valid">
                    <trim prefix="(" suffix=")" prefixOverrides="and">
                        <foreach collection="criteria.allCriteria" item="criterion">
                            <choose>
                                <when test="criterion.noValue">and ${{criterion.condition}}</when>
                                <when test="criterion.singleValue">and ${{criterion.condition}} #{{criterion.value}}</when>
                            </choose>
                        </foreach>
                    </trim>
                </if>
            </foreach>
        </where>
    </sql>
    <select id="countByExample" resultType="long">SELECT count(*) FROM {table["table"]} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if></select>
    <select id="selectByExample" resultMap="BaseResultMap">SELECT <if test="distinct">DISTINCT</if> <include refid="Base_Column_List"/> FROM {table["table"]} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if> <if test="orderByClause != null">ORDER BY ${{orderByClause}}</if></select>
    <select id="selectByPrimaryKey" resultMap="BaseResultMap">SELECT <include refid="Base_Column_List"/> FROM {table["table"]} WHERE id = #{{id}}</select>
    <delete id="deleteByPrimaryKey">DELETE FROM {table["table"]} WHERE id = #{{id}}</delete>
    <delete id="deleteByExample">DELETE FROM {table["table"]} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if></delete>
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">INSERT INTO {table["table"]} (<include refid="Base_Column_List"/>) VALUES ({values})</insert>
    <insert id="insertSelective" useGeneratedKeys="true" keyProperty="id">INSERT INTO {table["table"]} <trim prefix="(" suffix=")" suffixOverrides=",">{insert_columns}</trim> <trim prefix="VALUES (" suffix=")" suffixOverrides=",">{insert_values}</trim></insert>
    <update id="updateByPrimaryKeySelective">UPDATE {table["table"]} <set>{update_set}</set> WHERE id = #{{id}}</update>
    <update id="updateByPrimaryKey">UPDATE {table["table"]} SET {update_all} WHERE id = #{{id}}</update>
</mapper>
"""


def main():
    for table in TABLES:
        class_name = table["class"]
        mapper = class_name.replace("DO", "DOMapper")
        write(ENTITY_GEN / f"{class_name}.java", gen_do(table))
        write(ENTITY_GEN / f"{class_name}Example.java", gen_example(table))
        write(MAPPER_GEN / f"{mapper}.java", gen_mapper(table))
        write(XML_GEN / f"{mapper}.xml", gen_xml(table))


if __name__ == "__main__":
    main()
