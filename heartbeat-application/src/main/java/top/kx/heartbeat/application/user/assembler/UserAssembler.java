package top.kx.heartbeat.application.user.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.kx.heartbeat.application.user.dto.UserDTO;
import top.kx.heartbeat.domain.user.model.User;

/**
 * 用户装配器：领域模型 -> 应用层 DTO 的单向转换。
 *
 * <p>装配器集中承担"模型到协议"的映射，避免转换逻辑散落在应用服务里。
 * 此处用例简单，手写映射比引入框架更直观；映射复杂时可改用 MapStruct。
 */
@Mapper(componentModel = "spring")
public interface UserAssembler {

    @Mapping(target = "id", expression = "java(user.getId() == null ? null : user.getId().value())")
    @Mapping(target = "email", expression = "java(user.getEmail() == null ? null : user.getEmail().value())")
    @Mapping(target = "status", expression = "java(user.getStatus() == null ? null : user.getStatus().name())")
    UserDTO toDTO(User user);
}
