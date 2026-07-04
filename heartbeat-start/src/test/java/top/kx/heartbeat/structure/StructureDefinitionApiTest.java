package top.kx.heartbeat.structure;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class StructureDefinitionApiTest {

//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private SysOperLogMapper sysOperLogMapper;
//
//    @Test
//    void previewsCreatesVersionsActivatesAndValidates() throws Exception {
//        mockMvc.perform(post("/api/v1/structure-definitions/preview")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"samples\":[{\"name\":\"Alice\"},{\"name\":\"Bob\",\"age\":20}],"
//                                + "\"validationMode\":\"LENIENT\",\"uiOverrides\":{}}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("0"))
//                .andExpect(jsonPath("$.data.artifacts.JSON_SCHEMA.$schema")
//                        .value("https://json-schema.org/draft/2020-12/schema"))
//                .andExpect(jsonPath("$.data.artifacts.UI_SCHEMA.fields.name.widget").value("text"));
//
//        String createBody = mockMvc.perform(post("/api/v1/structure-definitions")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"name\":\"第三方用户\",\"description\":\"测试结构\","
//                                + "\"samples\":[{\"name\":\"Alice\"}],\"validationMode\":\"LENIENT\","
//                                + "\"uiOverrides\":{},\"activate\":true}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.activeVersionNo").value(1))
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        JsonNode createJson = objectMapper.readTree(createBody);
//        String id = createJson.at("/data/id").asText();
//
//        mockMvc.perform(post("/api/v1/structure-definitions/{id}/versions", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"samples\":[{\"name\":\"Alice\",\"age\":20}],"
//                                + "\"validationMode\":\"STRICT\",\"uiOverrides\":{}}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.versions.length()").value(2))
//                .andExpect(jsonPath("$.data.activeVersionNo").value(1));
//
//        mockMvc.perform(post("/api/v1/structure-definitions/{id}/draft", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"samples\":[{\"name\":\"Alice\",\"age\":20,\"email\":\"a@example.com\"}],"
//                                + "\"validationMode\":\"LENIENT\","
//                                + "\"fieldOverrides\":{\"$.email\":{\"title\":\"邮箱\"}}}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.draft.artifacts.JSON_SCHEMA.properties.email.type").value("string"));
//
//        mockMvc.perform(get("/api/v1/structure-definitions/{id}/diff?fromVersionNo=1&toDraft=true", id))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.changes.length()").isNotEmpty());
//
//        mockMvc.perform(post("/api/v1/structure-definitions/{id}/versions/from-draft", id))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.versions.length()").value(3))
//                .andExpect(jsonPath("$.data.draft").doesNotExist());
//
//        mockMvc.perform(put("/api/v1/structure-definitions/{id}/active-version", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"versionNo\":3}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.activeVersionNo").value(3));
//
//        mockMvc.perform(post("/api/v1/structure-definitions/{id}/validate", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"payload\":{\"name\":\"Bob\",\"age\":20,\"email\":\"b@example.com\",\"extra\":true}}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.valid").value(true))
//                .andExpect(jsonPath("$.data.versionNo").value(3));
//
//        mockMvc.perform(get("/api/v1/structure-definitions"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data[0].id").value(id));
//
//        mockMvc.perform(get("/api/v1/structure-definitions/{id}", id))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.id").value(id))
//                .andExpect(jsonPath("$.data.versions.length()").value(3));
//
//        long operLogCount = TenantContext.runAsPlatform(() ->
//                sysOperLogMapper.selectCountByQuery(QueryWrapper.create()
//                        .where("request_path like ?", "%/active-version")
//                        .and("result_status", "SUCCESS")));
//        assertTrue(operLogCount > 0, "typed sys_oper_log should persist successful operations");
//    }
}
