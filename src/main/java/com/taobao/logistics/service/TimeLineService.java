package com.taobao.logistics.service;

import com.taobao.logistics.mapper.TimeLineMapper;
import com.taobao.logistics.model.TimeLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统时间轴Service
 */
@Service
public class TimeLineService {

    @Autowired
    private TimeLineMapper timeLineMapper;

    /**
     * 查询所有时间轴记录
     * @return 时间轴列表
     */
    public List<TimeLine> selectTimeLineList() {
        return timeLineMapper.selectTimeLineList();
    }

    /**
     * 根据ID查询时间轴记录
     * @param id 主键ID
     * @return 时间轴记录
     */
    public TimeLine selectTimeLineById(Long id) {
        return timeLineMapper.selectTimeLineById(id);
    }

    /**
     * 新增时间轴记录
     * @param timeLine 时间轴记录
     * @return 影响行数
     */
    public int insertTimeLine(TimeLine timeLine) {
        return timeLineMapper.insertTimeLine(timeLine);
    }

    /**
     * 修改时间轴记录
     * @param timeLine 时间轴记录
     * @return 影响行数
     */
    public int updateTimeLine(TimeLine timeLine) {
        return timeLineMapper.updateTimeLine(timeLine);
    }

    /**
     * 删除时间轴记录
     * @param id 主键ID
     * @return 影响行数
     */
    public int deleteTimeLineById(Long id) {
        return timeLineMapper.deleteTimeLineById(id);
    }
}