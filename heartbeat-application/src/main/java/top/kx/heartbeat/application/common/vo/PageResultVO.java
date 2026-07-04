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
        PageResultVO<T> vo = new PageResultVO<>();
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        vo.setTotal(total);
        vo.setRecords(records == null ? Collections.emptyList() : records);
        return vo;
    }
}