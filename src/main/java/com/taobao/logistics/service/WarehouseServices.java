package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.entity.PrintView;
import com.taobao.logistics.entity.WarehouseShip;
import com.taobao.logistics.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ShiShiDaWei on 2021/9/30.
 * change by otteryuan on 2022/11/07
 */
@Slf4j
@Service
public class WarehouseServices {

    @Autowired
    private WarehouseRepository warehouseRepository;


    public WarehouseShip getPrintInfo(String waybillCode) {
        log.debug("The printed waybillCode={}", waybillCode);
        Optional<WarehouseShip> wareHouseShip = warehouseRepository.findByWaybillCode(waybillCode);
        if (wareHouseShip.isPresent()) {
            WarehouseShip ship = wareHouseShip.get();
            String printData = ship.getPrintDate();
            PrintView view = new PrintView();
            view.setCmd("print");
            view.setRequestID(waybillCode);
            view.setVersion("");
            PrintView.Task task = new PrintView.Task();
            task.setPreview(false);
            task.setPreviewType("pdf");
            task.setTaskID(String.valueOf(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli()));
            task.setPrinter("");
            PrintView.Documents documents = new PrintView.Documents();
            documents.setDocumentID(waybillCode);
            PrintView.Contents contents = new PrintView.Contents();
            JSONObject jsonObject = JSONObject.parseObject(printData);
            contents.setData(jsonObject);
            String templateURL = jsonObject.getString("templateURL");
            contents.setTemplateURL(templateURL);
            List<PrintView.Contents> collect = Stream.of(contents).collect(Collectors.toList());
            documents.setContents(collect);
            List<PrintView.Documents> documentList = Stream.of(documents).collect(Collectors.toList());
            task.setDocuments(documentList);
            view.setTask(task);
            String jsonString = JSONObject.toJSONString(view);
            ship.setPrintDate(jsonString);
            return ship;
        }
        return wareHouseShip.orElse(null);
    }
}
