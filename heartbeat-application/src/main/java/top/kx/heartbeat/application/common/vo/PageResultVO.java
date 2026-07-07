package top.kx.heartbeat.application.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果 VO
 *
 * @author heartbeat-team
 */
@Data
public class PageResultVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 总记录数 */
    private Long total;

    /** 数据集合 */
    private List<T> records;

    public static <T> PageResultVO<T> of(int pageNum, int pageSize, long total, List<T> records) {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        PageResultVO<T> vo = new PageResultVO<>();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        vo.setPageNum(pageNum);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        vo.setPageSize(pageSize);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        vo.setTotal(total);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        vo.setRecords(records == null ? Collections.emptyList() : records);
        // 返回已经完成封装的业务结果。
        return vo;
    }
}
