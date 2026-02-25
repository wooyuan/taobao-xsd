package com.taobao.logistics.service;

import com.taobao.logistics.model.SystemInfo;
import com.taobao.logistics.utils.PageResult;
import java.util.List;

/**
 * 系统信息Service接口
 */
public interface SystemInfoService {
    
    /**
     * 查询所有系统信息
     * @return 系统信息列表
     */
    List<SystemInfo> selectSystemInfoList();
    
    /**
     * 根据ID查询系统信息
     * @param id 主键ID
     * @return 系统信息
     */
    SystemInfo selectSystemInfoById(Long id);
    
    /**
     * 新增系统信息
     * @param systemInfo 系统信息
     * @return 影响行数
     */
    int insertSystemInfo(SystemInfo systemInfo);
    
    /**
     * 批量新增系统信息
     * @param systemInfoList 系统信息列表
     * @return 影响行数
     */
    int batchInsertSystemInfo(List<SystemInfo> systemInfoList);
    
    /**
     * 修改系统信息
     * @param systemInfo 系统信息
     * @return 影响行数
     */
    int updateSystemInfo(SystemInfo systemInfo);
    
    /**
     * 删除系统信息
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteSystemInfoById(Long id);
    
    /**
     * 批量删除系统信息
     * @param ids 主键ID列表
     * @return 影响行数
     */
    int batchDeleteSystemInfo(Long[] ids);
    
    /**
     * 查询系统信息总数
     * @return 总数
     */
    Long selectSystemInfoCount();
    
    /**
     * 分页查询系统信息
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<SystemInfo> selectSystemInfoListByPage(Long current, Long size);
}