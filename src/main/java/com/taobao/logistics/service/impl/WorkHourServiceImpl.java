package com.taobao.logistics.service.impl;

import com.taobao.logistics.mapper.WorkHourMapper;
import com.taobao.logistics.model.WorkHour;
import com.taobao.logistics.service.WorkHourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 员工工时Service实现类
 */
@Service
public class WorkHourServiceImpl implements WorkHourService {
    
    @Autowired
    private WorkHourMapper workHourMapper;
    
    @Override
    public List<WorkHour> selectWorkHourList() {
        return workHourMapper.selectWorkHourList();
    }
    
    @Override
    public WorkHour selectWorkHourById(Long id) {
        return workHourMapper.selectWorkHourById(id);
    }
    
    @Override
    public int insertWorkHour(WorkHour workHour) {
        return workHourMapper.insertWorkHour(workHour);
    }
    
    @Override
    public int batchInsertWorkHour(List<WorkHour> list) {
        return workHourMapper.batchInsertWorkHour(list);
    }
    
    @Override
    public int updateWorkHour(WorkHour workHour) {
        return workHourMapper.updateWorkHour(workHour);
    }
    
    @Override
    public int deleteWorkHourById(Long id) {
        return workHourMapper.deleteWorkHourById(id);
    }
    
    @Override
    public int batchDeleteWorkHour(Long[] ids) {
        return workHourMapper.batchDeleteWorkHour(ids);
    }
    
    @Override
    public List<java.util.Map<String, Object>> selectWorkHourStatisticsByCostDepartment() {
        return workHourMapper.selectWorkHourStatisticsByCostDepartment();
    }
    
    @Override
    public List<java.util.Map<String, Object>> selectWorkHourStatisticsBySystem() {
        return workHourMapper.selectWorkHourStatisticsBySystem();
    }
    
    @Override
    public List<java.util.Map<String, Object>> selectWorkHourStatisticsByEmployee() {
        return workHourMapper.selectWorkHourStatisticsByEmployee();
    }
    
    @Override
    public List<java.util.Map<String, Object>> selectWorkHourStatisticsBySystemAndEmployee() {
        return workHourMapper.selectWorkHourStatisticsBySystemAndEmployee();
    }
    
    @Override
    public List<java.util.Map<String, Object>> selectDevelopmentTimelineBySystem(String system) {
        return workHourMapper.selectDevelopmentTimelineBySystem(system);
    }
}