package top.kx.heartbeat.application.user.command;

import lombok.Value;

/**
 * 注册用户命令。
 *
 * <p>命令（Command）表达一次"改变系统状态的意图"，是接口层与应用层之间的输入契约，
 * 与领域模型解耦。命令对象不可变。
 */
@Value
public class RegisterUserCommand {

    String username;
    String email;
}
