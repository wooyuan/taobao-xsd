package com.taobao.logistics.service.wx;

import com.taobao.logistics.entity.dto.RequisitionOrderDTO2;
import com.taobao.logistics.entity.wx.GoodsPic;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import com.taobao.logistics.repository.GoosaPicRepository;
import com.taobao.logistics.repository.PortalDao;
import com.taobao.logistics.repository.RequisitionOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
@Service
public class WxOrderServices {

    @Autowired
    private RequisitionOrderRepository orderRepository;

    @Autowired
    private GoosaPicRepository goosaPicRepository;

    @Autowired
    private PortalDao portalDao;



    public List<RequisitionOrder> getOrderList(int reqUserId, int orderState) {
        return orderRepository.findAllByReqUserIdAndOrderState(reqUserId, orderState);
    }

    public List<RequisitionOrder> getOrderList(int reqUserId, int orderState, Pageable pageable) {
        return orderRepository.findAllByReqUserIdAndOrderStateAndIsactive(reqUserId, orderState, "Y", pageable);
    }

    public List<Integer> getUnOrderList(int reqUserId, int orderState) {
        return orderRepository.findAllByReqUserIdAndIsactive(reqUserId, orderState);
    }

    public Integer getCountOrder(int reqUserId, int orderState, String isactive) {
        return orderRepository.countByReqUserIdAndOrderStateAndIsactive(reqUserId, orderState, isactive);
    }

    public List<RequisitionOrder> getOrderList(Example<RequisitionOrder> example, Pageable pageable) {
        return orderRepository.findAll(example);
    }

    public List<RequisitionOrder> getOrderList(int reqUserId, int orderState, int mDim12Id, int storeId,String ares, String isactive,
                                               Date startTime, Date endTime, int status, Pageable pageable) {
        return orderRepository.findAllByReqUserIdAndIsactive(reqUserId, orderState, mDim12Id, storeId,
                ares, isactive, startTime, endTime, status, pageable);
    }

    public Integer getCountOrder(int reqUserId, int orderState, int mDim12Id, int storeId,
                                 String ares, String isactive, Date startTime, Date endTime, int status) {
        return orderRepository.countByReqUserIdAndIsactive(reqUserId, orderState, mDim12Id, storeId,
                ares, isactive, startTime, endTime, status);
    }

    public List<RequisitionOrder> getOrderList( int orderState, int mDim12Id, int storeId,String ares, String isactive,
                                               Date startTime, Date endTime, int status, Pageable pageable) {
        if (storeId == 1) {
            System.out.println("orderState "+orderState+"mDim12Id"+mDim12Id+"ares"+ares+"isactive "+isactive+"startTime"+startTime+"endTime"+endTime+"status"+status+"pageable"+pageable);
            return orderRepository.findAllByReqUserIdAndIsactive(orderState, mDim12Id,
                    ares, isactive, startTime, endTime, status, pageable);
        }

//        return orderRepository.findAllByReqUserIdAndIsactive(orderState, mDim12Id, storeId,
//                ares, isactive, startTime, endTime, status, pageable);
        return orderRepository.findAllByReqUserIdAndIsactive(orderState, mDim12Id, storeId,
                ares, isactive, startTime, endTime, status, pageable);
    }

    public Integer getCountOrder(int orderState, int mDim12Id, int storeId,
                                 String ares, String isactive, Date startTime, Date endTime, int status) {
        if (storeId == 1) {
            return orderRepository.countByReqUserIdAndIsactive(orderState, mDim12Id,
                    ares, isactive, startTime, endTime, status);
        }
        return orderRepository.countByReqUserIdAndIsactive(orderState, mDim12Id, storeId,
                ares, isactive, startTime, endTime, status);
    }

    public List<GoodsPic> uploadFiles(List<GoodsPic> files){
        return goosaPicRepository.saveAll(files);
    }

    public GoodsPic uploadFile(GoodsPic files){
        return goosaPicRepository.save(files);
    }



    public int getCountFile(String fileValue) {
        return goosaPicRepository.countByGoodsPicMD5(fileValue);
    }


    public GoodsPic getGoodsPic(String filevalue){
        return goosaPicRepository.findByGoodsPicMD5(filevalue);
    }


    public List<GoodsPic> getGoodsPicList(List<Integer> picIds) {
        return goosaPicRepository.findAllById(picIds);
    }

    public List<GoodsPic> getGoodsPicList(Pageable pageable){
        Page<GoodsPic> all = goosaPicRepository.findAll(pageable);
        return all.get().collect(Collectors.toList());
    }

    public RequisitionOrder getProduct(String no) {
        return portalDao.getPortalProductInfo(no);
    }

    public RequisitionOrder createRequisiontionOrder(RequisitionOrder requisitionOrder) {
        return orderRepository.saveAndFlush(requisitionOrder);
    }

    public void submitRequisitionOrder(Integer orderState, Integer orderId) {
        orderRepository.updateById(orderState, orderId);
    }

    public void submitRequisitionOrder(Integer orderState, List<Integer> orderIds) {
        orderRepository.updateById(orderState, new Date(), orderIds);
    }

    public void deleteRequisitionOrder(String isactive, Integer orderId) {
        orderRepository.updateByIdAndIsactive(isactive, new Date(), orderId);
    }

    public List<Map<String, Object>> getDistinctDim12List() {
        return orderRepository.findDistinctMDim12Id();
    }

    public List<Map<String, Object>> getDistinctDim12List(int requUserId) {
        return orderRepository.findDistinctMDim12Id(requUserId);
    }

    public List<Map<String, Object>> getDistinctProductAreas() {
        List<RequisitionOrderDTO2> distinctProductArea = orderRepository.findDistinctProductArea();
        if (distinctProductArea.size() != 0 && distinctProductArea.get(0) == null) {
            return null;
        }
        return distinctProductArea.stream().sorted(new Comparator<RequisitionOrderDTO2>() {
            @Override
            public int compare(RequisitionOrderDTO2 o1, RequisitionOrderDTO2 o2) {
                return o1.getProductArea().compareTo(o2.getProductArea());
            }
        }).map(req -> {
            String productArea = req.getProductArea();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productArea", productArea);
            return hashMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDistinctProductAreas(int requUserId) {
        return orderRepository.findDistinctProductArea(requUserId);
    }


    public void updateRequisionById(Integer reqId, String message) {
        orderRepository.updateMessageById(reqId, message);
    }

    public void updateMdstatusById(Integer reqId, String mdstatus) {
        orderRepository.updateMdstatusById(reqId, mdstatus);
    }

    public void updateRequisionById2(Integer reqId, String message,String picUrl) {
        orderRepository.updateMessageById2(reqId, message,picUrl);
    }

//    public void updatePicurlById(Integer reqId, String pic_curl) {
//        orderRepository.updatePicurlById(reqId, pic_curl);
//    }


}
