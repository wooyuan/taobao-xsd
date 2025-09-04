package com.taobao.logistics.service;

import com.taobao.logistics.entity.LogisticsProcedure;
import com.taobao.logistics.repository.LogisticsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogisticsService {

    @Autowired
    private LogisticsDao logisticsDao;

    public LogisticsProcedure processLogistics(String request) {
        // 调用存储过程
        return logisticsDao.callLogisticsProcedure("logistics_proc", request);
    }
}