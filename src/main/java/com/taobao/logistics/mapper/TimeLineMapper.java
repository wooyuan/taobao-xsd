package com.taobao.logistics.mapper;

import com.taobao.logistics.model.TimeLine;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 系统时间轴Mapper接口
 */
@Mapper
public interface TimeLineMapper {
    
    /**
     * 查询所有时间轴记录
     * @return 时间轴列表
     */
    List<TimeLine> selectTimeLineList();
    
    /**
     * 根据ID查询时间轴记录
     * @param id 主键ID
     * @return 时间轴记录
     */
    TimeLine selectTimeLineById(Long id);
    
    /**
     * 新增时间轴记录
     * @param timeLine 时间轴记录
     * @return 影响行数
     */
    int insertTimeLine(TimeLine timeLine);
    
    /**
     * 修改时间轴记录
     * @param timeLine 时间轴记录
     * @return 影响行数
     */
    int updateTimeLine(TimeLine timeLine);
    
    /**
     * 删除时间轴记录
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteTimeLineById(Long id);
}