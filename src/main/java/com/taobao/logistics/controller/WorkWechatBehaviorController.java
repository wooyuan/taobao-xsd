package com.taobao.logistics.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workwechat")
public class WorkWechatBehaviorController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/query")
    public JSONObject queryBehaviorData(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(required = false) String account) {
        log.info("查询企业微信行为数据，开始日期：{}，结束日期：{}，品牌：{}", startDate, endDate, account);
        
        JSONObject result = new JSONObject();
        
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM WORKWECHAT_BEHAVIOR_D@crm_134 WHERE 1=1");
            List<Object> params = new ArrayList<>();
            
            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND day_wid >= TO_NUMBER(TO_CHAR(TO_DATE(?, 'YYYY-MM-DD'), 'YYYYMMDD'))");
                params.add(startDate);
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND day_wid <= TO_NUMBER(TO_CHAR(TO_DATE(?, 'YYYY-MM-DD'), 'YYYYMMDD'))");
                params.add(endDate);
            }
            
            if (account != null && !account.isEmpty()) {
                sql.append(" AND account = ?");
                params.add(account);
            }
            
            sql.append(" ORDER BY day_wid DESC");
            
            log.info("执行SQL：{}，参数：{}", sql.toString(), params);
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            log.info("查询到 {} 条记录", data.size());
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", data);
            result.put("total", data.size());
            
        } catch (Exception e) {
            log.error("查询企业微信行为数据失败", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
            result.put("data", new ArrayList<>());
            result.put("total", 0);
        }
        
        return result;
    }

    @GetMapping("/brands")
    public JSONObject getBrands() {
        log.info("获取品牌列表");
        JSONObject result = new JSONObject();
        
        try {
            String sql = "SELECT DISTINCT account FROM WORKWECHAT_BEHAVIOR_D WHERE account IS NOT NULL ORDER BY account";
            List<Map<String, Object>> brands = jdbcTemplate.queryForList(sql);
            
            List<String> brandList = new ArrayList<>();
            for (Map<String, Object> brand : brands) {
                brandList.add((String) brand.get("account"));
            }
            
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", brandList);
            
        } catch (Exception e) {
            log.error("获取品牌列表失败", e);
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
            result.put("data", new ArrayList<>());
        }
        
        return result;
    }
}
