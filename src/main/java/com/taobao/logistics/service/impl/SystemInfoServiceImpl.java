package com.taobao.logistics.service.impl;

import com.taobao.logistics.mapper.SystemInfoMapper;
import com.taobao.logistics.model.SystemInfo;
import com.taobao.logistics.service.SystemInfoService;
import com.taobao.logistics.utils.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 系统信息Service实现类
 */
@Service
public class SystemInfoServiceImpl implements SystemInfoService {

    @Autowired
    private SystemInfoMapper systemInfoMapper;

    @Override
    public List<SystemInfo> selectSystemInfoList() {
        return systemInfoMapper.selectSystemInfoList();
    }

    @Override
    public SystemInfo selectSystemInfoById(Long id) {
        return systemInfoMapper.selectSystemInfoById(id);
    }

    @Override
    public int insertSystemInfo(SystemInfo systemInfo) {
        return systemInfoMapper.insertSystemInfo(systemInfo);
    }

    @Override
    public int batchInsertSystemInfo(List<SystemInfo> systemInfoList) {
        return systemInfoMapper.batchInsertSystemInfo(systemInfoList);
    }

    @Override
    public int updateSystemInfo(SystemInfo systemInfo) {
        return systemInfoMapper.updateSystemInfo(systemInfo);
    }

    @Override
    public int deleteSystemInfoById(Long id) {
        return systemInfoMapper.deleteSystemInfoById(id);
    }

    @Override
    public int batchDeleteSystemInfo(Long[] ids) {
        return systemInfoMapper.batchDeleteSystemInfo(ids);
    }

    @Override
    public Long selectSystemInfoCount() {
        return systemInfoMapper.selectSystemInfoCount();
    }

    @Override
    public PageResult<SystemInfo> selectSystemInfoListByPage(Long current, Long size) {
        // 计算偏移量
        Long offset = (current - 1) * size;
        // 查询分页数据
        List<SystemInfo> records = systemInfoMapper.selectSystemInfoListByPage(offset, size);
        // 查询总数
        Long total = systemInfoMapper.selectSystemInfoCount();
        
        return PageResult.of(records, total, current, size);
    }
}