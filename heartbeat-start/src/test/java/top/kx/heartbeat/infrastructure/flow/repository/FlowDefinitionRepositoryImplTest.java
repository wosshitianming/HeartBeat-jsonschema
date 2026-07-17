package top.kx.heartbeat.infrastructure.flow.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.infrastructure.flow.convert.FlowConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.FlowDefinitionRuntimeMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowVersionDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowDefinitionRepositoryImplTest {

    @Mock
    private HbFlowDefinitionDOMapper definitionMapper;
    @Mock
    private HbFlowVersionDOMapper versionMapper;
    @Mock
    private FlowConvert convert;
    @Mock
    private FlowDefinitionRuntimeMapper runtimeMapper;

    private FlowDefinitionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new FlowDefinitionRepositoryImpl();
        ReflectionTestUtils.setField(repository, "definitionDOMapper", definitionMapper);
        ReflectionTestUtils.setField(repository, "versionDOMapper", versionMapper);
        ReflectionTestUtils.setField(repository, "convert", convert);
        ReflectionTestUtils.setField(repository, "runtimeMapper", runtimeMapper);
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void detailLookupReadsTheDslBlob() {
        HbFlowDefinitionDO row = new HbFlowDefinitionDO();
        row.setId(42L);
        row.setDslJson("{\"nodes\":[{\"id\":\"start\"}],\"edges\":[]}");
        FlowDefinition expected = new FlowDefinition();
        expected.setId("42");
        when(definitionMapper.selectByExampleWithBLOBs(any(HbFlowDefinitionDOExample.class)))
                .thenReturn(Collections.singletonList(row));
        when(convert.toDomain(row)).thenReturn(expected);

        FlowDefinition actual = repository.findById("42").orElseThrow(AssertionError::new);

        assertSame(expected, actual);
        verify(definitionMapper).selectByExampleWithBLOBs(any(HbFlowDefinitionDOExample.class));
        verify(definitionMapper, never()).selectByExample(any(HbFlowDefinitionDOExample.class));
    }

    @Test
    void codeLookupReadsTheDslBlob() {
        HbFlowDefinitionDO row = new HbFlowDefinitionDO();
        row.setId(42L);
        row.setDslJson("{\"nodes\":[],\"edges\":[]}");
        FlowDefinition expected = new FlowDefinition();
        expected.setId("42");
        when(definitionMapper.selectByExampleWithBLOBs(any(HbFlowDefinitionDOExample.class)))
                .thenReturn(Collections.singletonList(row));
        when(convert.toDomain(row)).thenReturn(expected);

        FlowDefinition actual = repository.findByCode("order_paid").orElseThrow(AssertionError::new);

        assertSame(expected, actual);
        verify(definitionMapper).selectByExampleWithBLOBs(any(HbFlowDefinitionDOExample.class));
        verify(definitionMapper, never()).selectByExample(any(HbFlowDefinitionDOExample.class));
    }
}
