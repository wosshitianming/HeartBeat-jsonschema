# HeartBeat-JSONSchema 四层架构改造说明

## 一、目标

将原 MyBatis-Flex 改造为经典 MyBatis + MyBatis Generator 风格，参考互联网大厂分层：

```
┌──────────────────────────────────────────────────────────┐
│  interfaces   Controller  接收 Param、返回 VO             │
├──────────────────────────────────────────────────────────┤
│  application  Service     编排用例、转换 DTO ↔ VO         │
├──────────────────────────────────────────────────────────┤
│  domain       Model       充血模型、仓储接口              │
├──────────────────────────────────────────────────────────┤
│  infrastructure  Repository  拼装 Example → DO ↔ Domain   │
└──────────────────────────────────────────────────────────┘
```

## 二、四类对象职责

| 类型 | 全称 | 命名示例 | 所在层 | 职责 |
|------|------|----------|--------|------|
| DO | Data Object | `HbFlowDefinitionDO` | infrastructure | 数据库表映射，与字段一一对应 |
| DTO | Data Transfer Object | `FlowDefinitionDTO` | application | 跨层传输，扁平贫血对象 |
| VO | View Object | `FlowDefinitionVO` | interfaces | 视图展示，可裁剪/补充字段 |
| Param | Query/Save Param | `FlowDefinitionQueryParam` | interfaces | Controller 入参，承载查询条件 |

## 三、Example/Criteria 拼装模具

`HbFlowDefinitionExample` 是 MyBatis Generator 风格的条件拼装类，使用方式：

```java
HbFlowDefinitionExample example = new HbFlowDefinitionExample();
example.createCriteria()
        .andNameLike("%flow%")
        .andStatusEqualTo("ONLINE")
        .andCreateTimeGreaterThanOrEqualTo(from);
example.or().andCodeEqualTo("manual");
example.setOrderByClause("update_time desc");
example.setLimit(20);
example.setOffset(0);

List<HbFlowDefinitionDO> records = mapper.selectByExample(example);
```

对应的 Mapper XML 通过 `<foreach>` + `<where>` 动态拼装 SQL，对应字段名严格遵循下划线命名。

## 四、MapStruct 转换链

`FlowDefinitionStructMapper` 负责 DO ↔ DTO ↔ VO ↔ Domain 之间的字段搬运，DSL JSON 序列化/反序列化在此处完成。

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FlowDefinitionStructMapper {
    public abstract HbFlowDefinitionDO domainToDo(FlowDefinition source);
    public abstract FlowDefinitionDTO doToDto(HbFlowDefinitionDO source);
    public abstract FlowDefinitionVO dtoToVo(FlowDefinitionDTO source);
    // ...
}
```

## 五、典型调用链

```
Controller 接收 Param
    ↓
Application Service 拼装 Example
    ↓
Ext DAO 调用 Mapper（selectByExample）
    ↓
返回 DO 列表 → DTO 列表 → VO 列表
    ↓
Controller 返回 PageResultVO<VO>
```

## 六、注释规范

- **类**：JavaDoc 说明职责
- **方法**：JavaDoc 说明用途、参数、返回值
- **字段**：紧贴字段上一行的小注释（不用行尾注释）

示例：

```java
/** 流程定义主键 ID */
private String id;

/** 流程名称 */
private String name;
```

## 七、生成器使用

修改 `heartbeat-infrastructure/src/main/resources/generator/generatorConfig.xml` 中的数据库连接和表清单：

```xml
<table tableName="hb_flow_definition"
       domainObjectName="HbFlowDefinitionDO"
       enableCountByExample="true"
       enableUpdateByExample="true"
       enableDeleteByExample="true"
       enableSelectByExample="true">
    <generatedKey column="id" sqlStatement="MySql" identity="false"/>
</table>
```

执行生成：

```bash
mvn mybatis-generator:generate
```

或直接在 IDE 中运行 `GeneratorRunner.main()`。

## 八、改造完成度

- ✅ `HbFlowDefinition` 全套装（DO / Mapper / XML / Example / Criteria）
- ✅ `HbFlowVersion` 全套装（同上）
- ✅ `FlowDefinitionDTO` / `FlowDefinitionVO` / `FlowDefinitionQueryParam` / `FlowDefinitionSaveParam`
- ✅ `PageResultVO` 通用分页视图
- ✅ `FlowDefinitionStructMapper`（MapStruct，DO ↔ DTO ↔ VO ↔ Domain）
- ✅ `FlowDefinitionRepositoryImpl`（实现 FlowRepository + FlowDefinitionExtDao）
- ✅ `FlowDefinitionQueryService`（Param → Example → DO → DTO → VO）
- ✅ `FlowDefinitionController`（Param → Service → VO）
- ✅ MyBatis Generator 配置 + 插件 + 运行入口
- ✅ 分页插件（PageHelper）
- ✅ application.properties 调整
- ⏳ 其他表（sys_/structure_/pay_/...）可按 hb_flow_definition 模板批量生成