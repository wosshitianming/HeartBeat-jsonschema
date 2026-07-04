import fs from "fs";
import path from "path";
import {fileURLToPath} from "url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const entityGen = path.join(root, "heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/gen");
const mapperGen = path.join(root, "heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/gen");
const xmlGen = path.join(root, "heartbeat-infrastructure/src/main/resources/mapper-xml/gen");
const schema = fs.readFileSync(path.join(root, "heartbeat-start/src/main/resources/schema.sql"), "utf8");

const configs = [
  cfg("structure_definition", "StructureDefinitionDO", null, "gen/StructureDefinitionDO.java", ["id", "tenantId", "status"]),
  cfg("structure_draft", "StructureDraftDO", null, "gen/StructureDraftDO.java", ["id", "tenantId", "definitionId"]),
  cfg("structure_version", "StructureVersionDO", null, "gen/StructureVersionDO.java", ["id", "tenantId", "definitionId", "versionNo"]),
  cfg("structure_artifact", "StructureArtifactDO", null, "gen/StructureArtifactDO.java", ["id", "tenantId", "definitionId", "versionId"]),
  cfg("sys_tenant_plan", "SysTenantPlanDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysTenantPlanEntity", "platform/SysTenantPlanEntity.java", ["id", "planCode", "deleteMarker"]),
  cfg("sys_tenant", "SysTenantDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysTenantEntity", "platform/SysTenantEntity.java", ["id", "tenantCode", "deleteMarker"]),
  cfg("sys_dept", "SysDeptDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysDeptEntity", "platform/SysDeptEntity.java", ["id", "tenantId", "deptCode", "deleteMarker"]),
  cfg("sys_user", "SysUserDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysUserEntity", "platform/SysUserEntity.java", ["id", "tenantId", "username", "email", "deleteMarker"]),
  cfg("sys_role", "SysRoleDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysRoleEntity", "platform/SysRoleEntity.java", ["id", "tenantId", "roleCode", "deleteMarker"]),
  cfg("sys_permission", "SysPermissionDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysPermissionEntity", "platform/SysPermissionEntity.java", ["id", "tenantId", "permissionCode", "deleteMarker"]),
  cfg("sys_menu", "SysMenuDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysMenuEntity", "platform/SysMenuEntity.java", ["id", "tenantId", "menuCode", "deleteMarker"]),
  cfg("sys_user_role", "SysUserRoleDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysUserRoleEntity", "platform/SysUserRoleEntity.java", ["id", "tenantId", "userId", "roleId"]),
  cfg("sys_role_permission", "SysRolePermissionDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysRolePermissionEntity", "platform/SysRolePermissionEntity.java", ["id", "tenantId", "roleId", "permissionId"]),
  cfg("sys_menu_permission", "SysMenuPermissionDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysMenuPermissionEntity", "platform/SysMenuPermissionEntity.java", ["id", "tenantId", "menuId", "permissionId"]),
  cfg("sys_config", "SysConfigDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysConfigEntity", "platform/SysConfigEntity.java", ["id", "tenantId", "configKey", "deleteMarker"]),
  cfg("sys_oper_log", "SysOperLogDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.SysOperLogEntity", "platform/SysOperLogEntity.java", ["id", "tenantId", "traceId"]),
  cfg("auth_social_provider", "AuthSocialProviderDO", "top.kx.heartbeat.infrastructure.persistence.entity.platform.AuthSocialProviderEntity", "platform/AuthSocialProviderEntity.java", ["id", "tenantId", "providerCode", "deleteMarker"]),
  cfg("sys_job", "SysJobDO", "top.kx.heartbeat.infrastructure.persistence.entity.tool.SysJobEntity", "tool/SysJobEntity.java", ["id", "tenantId", "jobCode", "deleteMarker"]),
  cfg("flow_wait_state", "FlowWaitStateDO", "top.kx.heartbeat.infrastructure.persistence.entity.event.FlowWaitStateEntity", "event/FlowWaitStateEntity.java", ["id", "tenantId", "correlationKey"]),
  cfg("sys_outbox_event", "SysOutboxEventDO", "top.kx.heartbeat.infrastructure.persistence.entity.event.SysOutboxEventEntity", "event/SysOutboxEventEntity.java", ["id", "eventId"]),
  cfg("sys_inbox_event", "SysInboxEventDO", "top.kx.heartbeat.infrastructure.persistence.entity.event.SysInboxEventEntity", "event/SysInboxEventEntity.java", ["id", "consumerCode", "eventId"]),
];

for (const table of configs) {
  const schemaColumns = readSchemaColumns(table.table);
  const props = readProperties(table.sourcePath);
  table.fields = props
    .map((prop) => attachColumn(prop, schemaColumns))
    .filter(Boolean);
  write(path.join(entityGen, `${table.className}.java`), genDO(table));
  write(path.join(entityGen, `${table.className}Example.java`), genExample(table));
  write(path.join(mapperGen, `${table.mapperName}.java`), genMapper(table));
  write(path.join(xmlGen, `${table.mapperName}.xml`), genXml(table));
}

function cfg(table, className, baseClass, sourcePath, criteria) {
  return {
    table,
    className,
    mapperName: className.replace(/DO$/, "DOMapper"),
    exampleName: `${className}Example`,
    baseClass,
    sourcePath,
    criteria,
  };
}

function write(target, content) {
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, content, "utf8");
}

function readProperties(sourcePath) {
  const base = sourcePath.startsWith("gen/")
    ? path.join(entityGen, path.basename(sourcePath))
    : path.join(root, "heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity", sourcePath);
  const content = fs.readFileSync(base, "utf8");
  const props = [];
  const regex = /private\s+([A-Za-z0-9_.<>]+)\s+([A-Za-z0-9_]+)\s*;/g;
  let match;
  while ((match = regex.exec(content)) !== null) {
    props.push({ type: match[1], prop: match[2] });
  }
  return props;
}

function readSchemaColumns(tableName) {
  const regex = new RegExp(`CREATE TABLE IF NOT EXISTS ${tableName} \\(([^;]+?)\\n\\);`, "m");
  const match = regex.exec(schema);
  if (!match) {
    throw new Error(`Cannot find table ${tableName} in schema.sql`);
  }
  const columns = new Map();
  for (const rawLine of match[1].split(/\r?\n/)) {
    const line = rawLine.trim().replace(/,$/, "");
    if (!line || /^(PRIMARY|UNIQUE|KEY|INDEX|CONSTRAINT|FOREIGN)\b/i.test(line)) {
      continue;
    }
    const column = line.split(/\s+/)[0].replace(/`/g, "");
    columns.set(column, line);
  }
  return columns;
}

function attachColumn(prop, schemaColumns) {
  for (const column of candidateColumns(prop.prop)) {
    const line = schemaColumns.get(column);
    if (line) {
      return { ...prop, column, jdbc: jdbcType(line) };
    }
  }
  return null;
}

function candidateColumns(prop) {
  const special = {
    createTime: ["created_at", "create_time"],
    updateTime: ["updated_at", "update_time"],
    createBy: ["created_by", "create_by"],
    updateBy: ["updated_by", "update_by"],
    createdBy: ["created_by"],
    updatedBy: ["updated_by"],
    passwordUpdateTime: ["password_updated_at", "password_update_time"],
  };
  return [...(special[prop] || []), snake(prop)];
}

function snake(prop) {
  return prop.replace(/[A-Z]/g, (c) => `_${c.toLowerCase()}`);
}

function jdbcType(line) {
  const upper = line.toUpperCase();
  if (upper.includes("BIGINT")) return "BIGINT";
  if (upper.includes("INT")) return "INTEGER";
  if (upper.includes("BOOLEAN")) return "BOOLEAN";
  if (upper.includes("TIMESTAMP")) return "TIMESTAMP";
  if (upper.includes("TEXT")) return "LONGVARCHAR";
  if (upper.includes("DECIMAL")) return "DECIMAL";
  return "VARCHAR";
}

function cap(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function genDO(table) {
  if (table.baseClass) {
    return `package top.kx.heartbeat.infrastructure.persistence.entity.gen;\n\npublic class ${table.className} extends ${table.baseClass} {\n}\n`;
  }
  const fields = table.fields.map((f) => `    private ${f.type} ${f.prop};`).join("\n");
  const methods = table.fields
    .map((f) => {
      const name = cap(f.prop);
      return `    public ${f.type} get${name}() { return ${f.prop}; }\n    public void set${name}(${f.type} ${f.prop}) { this.${f.prop} = ${f.prop}; }`;
    })
    .join("\n");
  return `package top.kx.heartbeat.infrastructure.persistence.entity.gen;\n\npublic class ${table.className} {\n\n${fields}\n\n${methods}\n}\n`;
}

function genExample(table) {
  const fields = new Map(table.fields.map((f) => [f.prop, f]));
  const criteria = table.criteria
    .map((prop) => {
      const f = fields.get(prop);
      if (!f) throw new Error(`Missing criteria field ${prop} for ${table.table}`);
      return `        public Criteria and${cap(prop)}EqualTo(${f.type} value) { addCriterion("${f.column} =", value, "${prop}"); return this; }`;
    })
    .join("\n");
  return `package top.kx.heartbeat.infrastructure.persistence.entity.gen;\n\nimport java.util.ArrayList;\nimport java.util.List;\n\npublic class ${table.exampleName} {\n\n    protected String orderByClause;\n    protected boolean distinct;\n    protected List<Criteria> oredCriteria;\n\n    public ${table.exampleName}() {\n        oredCriteria = new ArrayList<>();\n    }\n\n    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }\n    public String getOrderByClause() { return orderByClause; }\n    public void setDistinct(boolean distinct) { this.distinct = distinct; }\n    public boolean isDistinct() { return distinct; }\n    public List<Criteria> getOredCriteria() { return oredCriteria; }\n\n    public Criteria createCriteria() {\n        Criteria criteria = createCriteriaInternal();\n        if (oredCriteria.size() == 0) {\n            oredCriteria.add(criteria);\n        }\n        return criteria;\n    }\n\n    protected Criteria createCriteriaInternal() { return new Criteria(); }\n\n    public void clear() {\n        oredCriteria.clear();\n        orderByClause = null;\n        distinct = false;\n    }\n\n    public static class Criteria {\n        protected List<Criterion> criteria = new ArrayList<>();\n\n        public boolean isValid() { return criteria.size() > 0; }\n        public List<Criterion> getAllCriteria() { return criteria; }\n\n        protected void addCriterion(String condition) {\n            if (condition == null) { throw new IllegalArgumentException("Value for condition cannot be null"); }\n            criteria.add(new Criterion(condition));\n        }\n\n        protected void addCriterion(String condition, Object value, String property) {\n            if (value == null) { throw new IllegalArgumentException("Value for " + property + " cannot be null"); }\n            criteria.add(new Criterion(condition, value));\n        }\n\n${criteria}\n    }\n\n    public static class Criterion {\n        private String condition;\n        private Object value;\n        private boolean noValue;\n        private boolean singleValue;\n\n        protected Criterion(String condition) {\n            this.condition = condition;\n            this.noValue = true;\n        }\n\n        protected Criterion(String condition, Object value) {\n            this.condition = condition;\n            this.value = value;\n            this.singleValue = true;\n        }\n\n        public String getCondition() { return condition; }\n        public Object getValue() { return value; }\n        public boolean isNoValue() { return noValue; }\n        public boolean isSingleValue() { return singleValue; }\n    }\n}\n`;
}

function genMapper(table) {
  return `package top.kx.heartbeat.infrastructure.persistence.mapper.gen;\n\nimport java.util.List;\nimport org.apache.ibatis.annotations.Param;\nimport top.kx.heartbeat.infrastructure.persistence.entity.gen.${table.className};\nimport top.kx.heartbeat.infrastructure.persistence.entity.gen.${table.exampleName};\n\npublic interface ${table.mapperName} {\n    long countByExample(${table.exampleName} example);\n    int deleteByExample(${table.exampleName} example);\n    int deleteByPrimaryKey(@Param("id") Long id);\n    int insert(${table.className} record);\n    int insertSelective(${table.className} record);\n    List<${table.className}> selectByExample(${table.exampleName} example);\n    ${table.className} selectByPrimaryKey(@Param("id") Long id);\n    int updateByPrimaryKeySelective(${table.className} record);\n    int updateByPrimaryKey(${table.className} record);\n}\n`;
}

function genXml(table) {
  const resultMap = table.fields
    .map((f) => `        <${f.prop === "id" ? "id" : "result"} column="${f.column}" property="${f.prop}" jdbcType="${f.jdbc}"/>`)
    .join("\n");
  const columns = table.fields.map((f) => f.column).join(", ");
  const values = table.fields.map((f) => `#{${f.prop}}`).join(", ");
  const selectiveColumns = table.fields.map((f) => `<if test="${f.prop} != null">${f.column},</if>`).join("");
  const selectiveValues = table.fields.map((f) => `<if test="${f.prop} != null">#{${f.prop}},</if>`).join("");
  const updateSet = table.fields
    .filter((f) => f.prop !== "id")
    .map((f) => `<if test="${f.prop} != null">${f.column} = #{${f.prop}},</if>`)
    .join("");
  const updateAll = table.fields
    .filter((f) => f.prop !== "id")
    .map((f) => `${f.column} = #{${f.prop}}`)
    .join(", ");
  const ns = `top.kx.heartbeat.infrastructure.persistence.mapper.gen.${table.mapperName}`;
  const type = `top.kx.heartbeat.infrastructure.persistence.entity.gen.${table.className}`;
  return `<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">\n<mapper namespace="${ns}">\n    <resultMap id="BaseResultMap" type="${type}">\n${resultMap}\n    </resultMap>\n    <sql id="Base_Column_List">${columns}</sql>\n    <sql id="Example_Where_Clause">\n        <where>\n            <foreach collection="oredCriteria" item="criteria" separator="or">\n                <if test="criteria.valid">\n                    <trim prefix="(" suffix=")" prefixOverrides="and">\n                        <foreach collection="criteria.allCriteria" item="criterion">\n                            <choose>\n                                <when test="criterion.noValue">and \${criterion.condition}</when>\n                                <when test="criterion.singleValue">and \${criterion.condition} #{criterion.value}</when>\n                            </choose>\n                        </foreach>\n                    </trim>\n                </if>\n            </foreach>\n        </where>\n    </sql>\n    <select id="countByExample" resultType="long">SELECT count(*) FROM ${table.table} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if></select>\n    <select id="selectByExample" resultMap="BaseResultMap">SELECT <if test="distinct">DISTINCT</if> <include refid="Base_Column_List"/> FROM ${table.table} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if> <if test="orderByClause != null">ORDER BY \${orderByClause}</if></select>\n    <select id="selectByPrimaryKey" resultMap="BaseResultMap">SELECT <include refid="Base_Column_List"/> FROM ${table.table} WHERE id = #{id}</select>\n    <delete id="deleteByPrimaryKey">DELETE FROM ${table.table} WHERE id = #{id}</delete>\n    <delete id="deleteByExample">DELETE FROM ${table.table} <if test="_parameter != null"><include refid="Example_Where_Clause"/></if></delete>\n    <insert id="insert" useGeneratedKeys="true" keyProperty="id">INSERT INTO ${table.table} (<include refid="Base_Column_List"/>) VALUES (${values})</insert>\n    <insert id="insertSelective" useGeneratedKeys="true" keyProperty="id">INSERT INTO ${table.table} <trim prefix="(" suffix=")" suffixOverrides=",">${selectiveColumns}</trim> <trim prefix="VALUES (" suffix=")" suffixOverrides=",">${selectiveValues}</trim></insert>\n    <update id="updateByPrimaryKeySelective">UPDATE ${table.table} <set>${updateSet}</set> WHERE id = #{id}</update>\n    <update id="updateByPrimaryKey">UPDATE ${table.table} SET ${updateAll} WHERE id = #{id}</update>\n</mapper>\n`;
}
