package com.taobao.logistics.controller;

import com.taobao.logistics.model.TimeLine;
import com.taobao.logistics.service.TimeLineService;
import com.taobao.logistics.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统时间轴Controller
 */
@RestController
@RequestMapping("/api/timeline")
public class TimeLineController {

    @Autowired
    private TimeLineService timeLineService;

    /**
     * 查询所有时间轴记录
     * @return 时间轴列表
     */
    @GetMapping("/list")
    public AjaxResult selectTimeLineList() {
        List<TimeLine> list = timeLineService.selectTimeLineList();
        return AjaxResult.success(list);
    }

    /**
     * 根据ID查询时间轴记录
     * @param id 主键ID
     * @return 时间轴记录
     */
    @GetMapping("/info/{id}")
    public AjaxResult selectTimeLineById(@PathVariable Long id) {
        TimeLine timeLine = timeLineService.selectTimeLineById(id);
        return AjaxResult.success(timeLine);
    }

    /**
     * 新增时间轴记录
     * @param timeLine 时间轴记录
     * @return 结果
     */
    @PostMapping("/add")
    public AjaxResult insertTimeLine(@RequestBody TimeLine timeLine) {
        int rows = timeLineService.insertTimeLine(timeLine);
        if (rows > 0) {
            return AjaxResult.success("新增成功");
        }
        return AjaxResult.error("新增失败");
    }

    /**
     * 修改时间轴记录
     * @param timeLine 时间轴记录
     * @return 结果
     */
    @PutMapping("/update")
    public AjaxResult updateTimeLine(@RequestBody TimeLine timeLine) {
        int rows = timeLineService.updateTimeLine(timeLine);
        if (rows > 0) {
            return AjaxResult.success("修改成功");
        }
        return AjaxResult.error("修改失败");
    }

    /**
     * 删除时间轴记录
     * @param id 主键ID
     * @return 结果
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteTimeLineById(@PathVariable Long id) {
        int rows = timeLineService.deleteTimeLineById(id);
        if (rows > 0) {
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }
}