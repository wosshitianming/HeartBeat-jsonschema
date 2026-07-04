package top.kx.heartbeat.interfaces.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.user.UserApplicationService;
import top.kx.heartbeat.application.user.command.ChangeEmailCommand;
import top.kx.heartbeat.application.user.command.RegisterUserCommand;
import top.kx.heartbeat.application.user.dto.UserDTO;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.user.request.ChangeEmailRequest;
import top.kx.heartbeat.interfaces.user.request.RegisterUserRequest;
import top.kx.heartbeat.interfaces.user.response.UserResponse;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 用户接口控制器。
 *
 * <p>负责用户注册、查询、邮箱变更和禁用的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /**
     * 用户应用服务。
     */
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 注册用户。
     *
     * @param request 用户注册请求对象
     * @return 用户注册响应
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('system:user:add')")
    public Result<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        // 创建用户注册命令。
        RegisterUserCommand command = new RegisterUserCommand(request.getUsername(), request.getEmail());
        // 调用应用服务注册用户。
        UserDTO dto = userApplicationService.register(command);
        // 转换用户响应对象。
        UserResponse response = UserResponse.from(dto);
        // 返回用户注册响应。
        return Result.success(response);
    }

    /**
     * 查询用户详情。
     *
     * @param id 用户标识
     * @return 用户详情响应
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('system:user:list')")
    public Result<UserResponse> getById(@PathVariable Long id) {
        // 查询用户应用 DTO。
        UserDTO dto = userApplicationService.getById(id);
        // 转换用户响应对象。
        UserResponse response = UserResponse.from(dto);
        // 返回用户详情响应。
        return Result.success(response);
    }

    /**
     * 修改用户邮箱。
     *
     * @param id 用户标识
     * @param request 邮箱变更请求对象
     * @return 用户邮箱变更响应
     */
    @PutMapping("/{id}/email")
    @PreAuthorize("@permissionGuard.has('system:user:edit')")
    public Result<UserResponse> changeEmail(@PathVariable Long id,
                                            @Valid @RequestBody ChangeEmailRequest request) {
        // 创建邮箱变更命令。
        ChangeEmailCommand command = new ChangeEmailCommand(id, request.getEmail());
        // 调用应用服务修改用户邮箱。
        UserDTO dto = userApplicationService.changeEmail(command);
        // 转换用户响应对象。
        UserResponse response = UserResponse.from(dto);
        // 返回用户邮箱变更响应。
        return Result.success(response);
    }

    /**
     * 禁用用户。
     *
     * @param id 用户标识
     * @return 禁用结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('system:user:remove')")
    public Result<Void> disable(@PathVariable Long id) {
        // 调用应用服务禁用用户。
        userApplicationService.disable(id);
        // 返回禁用成功响应。
        return Result.success();
    }
}
