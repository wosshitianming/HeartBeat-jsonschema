package top.kx.heartbeat.report;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportQueryMapperXmlTest {

    @Test
    void reportQueryIsWrappedWithDatabaseSideLimit() throws Exception {
        String resource = "mapper-xml/ReportQueryMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        MappedStatement statement = configuration.getMappedStatement(
                "top.kx.heartbeat.infrastructure.persistence.mapper.ReportQueryMapper.executeReportQuery");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sql", "select id from sys_user");
        parameters.put("params", Collections.emptyMap());
        parameters.put("limit", 500);
        BoundSql boundSql = statement.getBoundSql(parameters);
        String normalizedSql = boundSql.getSql().replaceAll("\\s+", " ").trim().toLowerCase();

        assertTrue(normalizedSql.startsWith("select * from ( select id from sys_user ) hb_report_query"));
        assertTrue(normalizedSql.endsWith("limit ?"));
        assertEquals("limit", boundSql.getParameterMappings().get(0).getProperty());
    }
}
