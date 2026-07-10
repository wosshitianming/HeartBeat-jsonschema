package top.kx.heartbeat.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.platform.port.PlatformLoginLogRepository;

import javax.annotation.Resource;

@Service
public class LoginAuditService {

    @Resource
    private PlatformLoginLogRepository platformLoginLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String status, String message) {
        platformLoginLogRepository.recordLogin(username, status, message);
    }
}
