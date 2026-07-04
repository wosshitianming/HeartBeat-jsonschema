package top.kx.heartbeat.interfaces.iam.request;

import lombok.Data;

import java.util.List;

/**
 * 分配角色菜单请求对象。
 *
 * <p>用于承接角色授权菜单时提交的菜单标识集合。</p>
 */
@Data
public class AssignRoleMenusRequest {

    /**
     * 菜单标识集合。
     */
    private List<String> menuIds;
}
