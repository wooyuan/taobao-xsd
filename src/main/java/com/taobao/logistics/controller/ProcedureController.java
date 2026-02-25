package com.taobao.logistics.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/procedure")
public class ProcedureController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行自动跨年存储过程
     * @param piId 存储过程参数
     * @return 执行结果
     */
    @GetMapping("/executeAutoTsGuonian")
    public JSONObject executeAutoTsGuonian(@RequestParam(required = true) Integer piId) {
        log.info("执行自动跨年存储过程，参数：{}", piId);
        
        JSONObject result = new JSONObject();
        
        try {
            // 执行存储过程
            String procedureSql = "{call auto_ts_guonian(?)}";
            jdbcTemplate.execute(procedureSql, (Map<String, Object> inParams) -> {
                inParams.put("p_pi_id", piId);
                return null;
            });
            
            log.info("存储过程执行成功");
            result.put("code", 200);
            result.put("message", "存储过程执行成功");
            result.put("piId", piId);
            
        } catch (Exception e) {
            log.error("执行存储过程失败", e);
            result.put("code", 500);
            result.put("message", "执行失败：" + e.getMessage());
            result.put("piId", piId);
        }
        
        return result;
    }

    /**
     * 执行自动跨年存储过程（默认参数893）
     * @return 执行结果
     */
    @GetMapping("/executeAutoTsGuonianDefault")
    public JSONObject executeAutoTsGuonianDefault() {
        return executeAutoTsGuonian(893);
    }
}
