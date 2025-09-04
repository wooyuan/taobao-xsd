package com.taobao.logistics.repository;

import com.taobao.logistics.entity.dto.RequisitionUserDTO;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
@Component
public class PortalDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public RequisitionUserDTO getUser(String jobNum) {
        String sql = "SELECT u.name user_name, c.name store_name, u.c_store_id, t.id, t.brand, t.job_num, t.mobile, t.userid, t.wx_status " +
                " from USERS u, C_STORE c, HR_EMPLOYEE h, T_REQUISITION_USER t " +
                " WHERE u.isactive = 'Y' AND u.hr_employee_id = h.id AND u.c_store_id = c.id AND t.job_num = h.no AND h.no = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            RequisitionUserDTO requisitionUser1 = new RequisitionUserDTO();
            requisitionUser1.setName(rs.getString("user_name"));
            requisitionUser1.setStoreName(rs.getString(2));
            requisitionUser1.setStoreId(rs.getInt(3));
            requisitionUser1.setId(rs.getInt(4));
            requisitionUser1.setBrand(rs.getString(5));
            requisitionUser1.setJobNum(rs.getString(6));
            requisitionUser1.setMobile(rs.getString(7));
            requisitionUser1.setUserid(rs.getString(8));
            requisitionUser1.setWxStatus(rs.getInt(9));
            return requisitionUser1;
        }, jobNum);
    }


    public RequisitionUserDTO getPortalUser(String jobNum) {
        String sql = "SELECT distinct h.name user_name, c.name store_name, h.c_store_id, c.code " +
                "         from   C_STORE c, HR_EMPLOYEE h " +
                "    WHERE h.isactive = 'Y' AND h.c_store_id = c.id AND h.no =  ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            RequisitionUserDTO userDTO = new RequisitionUserDTO();
            userDTO.setName(rs.getString("user_name"));
            userDTO.setStoreName(rs.getString(2));
            userDTO.setStoreId(rs.getInt(3));
            userDTO.setStoreCode(rs.getString(4));
            return userDTO;
        }, jobNum);
    }


    public RequisitionOrder getPortalProductInfo(String productNo) {
        String sql = "SELECT mp.no, m.id m_product_id, m.uda_32, m.M_DIM12_ID, md12.attribname, sp.code, m.serialno, ma.value1, ma.value2, mp.m_attributesetinstance_id" +
                "  from m_product_alias mp, m_product m, m_attributesetinstance ma , m_dim md12, C_SUPPLIER sp" +
                " WHERE mp.m_product_id = m.id AND mp.m_attributesetinstance_id = ma.id AND mp.no = ?" +
                " AND m.m_dim12_id = md12.id AND m.c_supplier_id = sp.id and mp.isactive = 'Y' ";
        List<Map<String, Object>> maps = this.jdbcTemplate.queryForList("select count(*) count from m_product_alias mp where mp.no = ? and mp.isactive = 'Y'", productNo);
        Map<String, Object> map = maps.get(0);
        int count = ((BigDecimal) map.get("count")).intValue();
        if (count < 1) {
            return null;
        }

        return jdbcTemplate.queryForObject(sql, (RowMapper<RequisitionOrder>) (rs, rowNum) -> {
            RequisitionOrder order = new RequisitionOrder();
            order.setProductNo(rs.getString(1));
            order.setmProductId(rs.getInt(2));
            order.setProductArea(rs.getString(3));
            order.setmDim12Id(rs.getInt(4));
            order.setmDim12Attr(rs.getString(5));
            //原厂编号
            order.setSerialNo(rs.getString("serialno"));
            //供应商编码
            order.setProductCode(rs.getString("code"));
            order.setmColor(rs.getString(8));
            order.setmSize(rs.getString(9));
            order.setmAttributesetinstanceId(rs.getInt(10));
            return order;
        }, productNo);
    }




}
