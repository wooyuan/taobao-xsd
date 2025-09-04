package com.taobao.logistics.repository;

import com.taobao.logistics.entity.LogisticsProcedure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.sql.Types;
import java.util.Map;

@Repository
public class LogisticsDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public LogisticsDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 调用Oracle存储过程示例
     * @param procedureName 存储过程名称
     * @param requestParams 请求参数
     * @return 包含响应数据和错误信息的实体
     */
    public LogisticsProcedure callLogisticsProcedure(String procedureName, String requestParams) {
        // 创建存储过程调用对象
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName(procedureName)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        // 声明输入参数 (IN)
                        new SqlParameter("p_request", Types.VARCHAR),
                        // 声明输出参数 (OUT)
                        new SqlOutParameter("p_code", Types.INTEGER),
                        new SqlOutParameter("p_data", Types.VARCHAR),
                        new SqlOutParameter("p_msg", Types.VARCHAR)
                );

        // 设置输入参数
       // Map<String, Object> inParams = Map.of("p_request", requestParams);

        // 执行存储过程并获取结果
        Map<String, Object> out = null;//jdbcCall.execute(inParams);

        // 创建返回实体
        LogisticsProcedure result = new LogisticsProcedure();
        result.setProcedureName(procedureName);
        result.setRequestParams(requestParams);
        result.setStatusCode((Integer) out.get("p_code"));
        result.setResponseData((String) out.get("p_data"));
        result.setErrorMessage((String) out.get("p_msg"));

        return result;
    }
}