package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformSocialRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialBindingDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialBindingDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialBindingDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialProviderDOMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PlatformSocialRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformSocialRepository {

    @Resource
    private AuthSocialProviderDOMapper socialProviderMapper;
    @Resource
    private AuthSocialBindingDOMapper socialBindingMapper;

    @Override
    public List<DomainRecord> listSocialProviders() {
        return records(socialProviderMapper.selectByExample(new AuthSocialProviderDOExample()));
    }

    @Override
    public DomainRecord createSocialProvider(Map<String, Object> command) {
        return create(socialProviderMapper, new AuthSocialProviderDO(), command);
    }

    @Override
    public DomainRecord updateSocialProvider(String id, Map<String, Object> command) {
        return update(socialProviderMapper, new AuthSocialProviderDO(), id, command);
    }

    @Override
    public void deleteSocialProvider(String id) {
        delete(socialProviderMapper, id);
    }

    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andStatusEqualTo("ENABLED");
        return records(socialProviderMapper.selectByExample(example));
    }

    @Override
    public Optional<DomainRecord> findSocialProvider(String provider) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andProviderCodeEqualTo(provider);
        return first(socialProviderMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public Optional<DomainRecord> findSocialBind(String provider, String openId) {
        Optional<DomainRecord> providerRecord = findSocialProvider(provider);
        Long providerId = providerRecord.map(record -> longValue(String.valueOf(record.get("id")))).orElse(null);
        if (providerId == null) {
            return Optional.empty();
        }
        AuthSocialBindingDOExample example = new AuthSocialBindingDOExample();
        example.createCriteria().andProviderIdEqualTo(providerId).andExternalUserIdEqualTo(openId);
        return first(socialBindingMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public DomainRecord saveSocialBind(Map<String, Object> command) {
        return create(socialBindingMapper, new AuthSocialBindingDO(), command);
    }
}
