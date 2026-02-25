package com.taobao.logistics.service;

import com.taobao.logistics.model.WorkHour;
import java.util.List;

/**
 * 员工工时Service接口
 */
public interface WorkHourService {
    /**
     * 查询所有工时记录
     * @return 工时记录列表
     */
    List<WorkHour> selectWorkHourList();
    
    /**
     * 根据ID查询工时记录
     * @param id 主键ID
     * @return 工时记录
     */
    WorkHour selectWorkHourById(Long id);
    
    /**
     * 新增工时记录
     * @param workHour 工时记录
     * @return 结果
     */
    int insertWorkHour(WorkHour workHour);
    
    /**
     * 批量新增工时记录
     * @param list 工时记录列表
     * @return 结果
     */
    int batchInsertWorkHour(List<WorkHour> list);
    
    /**
     * 修改工时记录
     * @param workHour 工时记录
     * @return 结果
     */
    int updateWorkHour(WorkHour workHour);
    
    /**
     * 删除工时记录
     * @param id 主键ID
     * @return 结果
     */
    int deleteWorkHourById(Long id);
    
    /**
     * 批量删除工时记录
     * @param ids 主键ID列表
     * @return 结果
     */
    int batchDeleteWorkHour(Long[] ids);
    
    /**
     * 按费用归属部门统计执行工时合计
     * @return 统计结果列表
     */
    List<java.util.Map<String, Object>> selectWorkHourStatisticsByCostDepartment();
    
    /**
     * 按系统统计执行工时合计
     * @return 统计结果列表
     */
    List<java.util.Map<String, Object>> selectWorkHourStatisticsBySystem();
    
    /**
     * 按员工统计执行工时合计
     * @return 统计结果列表
     */
    List<java.util.Map<String, Object>> selectWorkHourStatisticsByEmployee();
    
    /**
     * 按系统和员工统计执行工时合计
     * @return 统计结果列表
     */
    List<java.util.Map<String, Object>> selectWorkHourStatisticsBySystemAndEmployee();
    
    /**
     * 获取系统发展时间轴数据
     * @param system 系统名称
     * @return 发展时间轴数据列表
     */
    List<java.util.Map<String, Object>> selectDevelopmentTimelineBySystem(String system);
}