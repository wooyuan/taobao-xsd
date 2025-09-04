package com.taobao.logistics.service.wx;

import com.taobao.logistics.entity.wx.RequisitionUser;
import com.taobao.logistics.repository.RequisitionUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
@Service
public class WxUserServices {


    @Autowired
    private RequisitionUserRepository requisitionUserRepository;


    public int getCountUser(String jobNum) {
        return requisitionUserRepository.countByJobNum(jobNum);
    }

    public RequisitionUser getUser(String jobNum) {
        return requisitionUserRepository.findByJobNum(jobNum);
    }


    public List<Map<String, Object>> getStoreList() {
        List<Map<String, Object>> stores = requisitionUserRepository.findDistinctStoreIdAndStoreName();
//        if (stores.size() != 0 && null != stores.get(0)) {
//            Stream<Map<String, Object>> stream = stores.stream().map(req -> {
//                HashMap<String, Object> hashMap = new HashMap<>();
//                String storeName = req.getStoreName();
//                Integer storeId = req.getStoreId();
//                hashMap.put("storeId", storeId);
//                hashMap.put("storeName", storeName);
//                return hashMap;
//            });
//            return stream.collect(Collectors.toList());
//        }
        return stores;
    }


    public List<Map<String, Object>> getStoreList(Integer reqUserId) {
        return requisitionUserRepository.findDistinctStoreIdAndStoreName(reqUserId);
    }
}
