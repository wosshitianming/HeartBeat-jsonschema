package top.kx.heartbeat.application.user.command;

import lombok.Value;

/**
 * 修改用户邮箱命令。
 */
@Value
public class ChangeEmailCommand {

    Long userId;
    String newEmail;
}
