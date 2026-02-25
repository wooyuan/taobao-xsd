package com.taobao.logistics.mapper;

import com.taobao.logistics.model.SystemInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统信息Mapper接口
 */
@Mapper
public interface SystemInfoMapper {
    
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
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 系统信息列表
     */
    List<SystemInfo> selectSystemInfoListByPage(@Param("offset") Long offset, @Param("limit") Long limit);
}