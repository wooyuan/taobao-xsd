package com.taobao.logistics.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class ExcelGenerator {

    public static void main(String[] args) {
        // 定义输出文件路径
        String filePath = "D:\\2.xlsx";
        // 目标总数量
        int targetTotal = 110324;
        // 数量范围
        int minQty = 1;
        int maxQty = 28;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("条码数据");

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            Cell headerCell1 = headerRow.createCell(0);
            headerCell1.setCellValue("条码");
            Cell headerCell2 = headerRow.createCell(1);
            headerCell2.setCellValue("数量");

            // 设置表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerCell1.setCellStyle(headerStyle);
            headerCell2.setCellStyle(headerStyle);

            // 生成数据
            Random random = new Random();
            int currentTotal = 0;
            int rowIndex = 1;

            while (currentTotal < targetTotal) {
                // 生成随机数量，确保不超过剩余数量
                int remaining = targetTotal - currentTotal;
                int qty = Math.min(random.nextInt(maxQty - minQty + 1) + minQty, remaining);
                
                // 生成条码（格式：BAR-YYYYMMDD-XXXX，其中XXXX为4位序号）
                String barcode = String.format("BAR-%s-%04d", "20260123", rowIndex);
                
                // 创建数据行
                Row dataRow = sheet.createRow(rowIndex);
                Cell barcodeCell = dataRow.createCell(0);
                barcodeCell.setCellValue(barcode);
                Cell qtyCell = dataRow.createCell(1);
                qtyCell.setCellValue(qty);
                
                // 更新当前总计
                currentTotal += qty;
                rowIndex++;
            }

            // 调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                System.out.println("Excel文件生成成功！文件路径：" + filePath);
                System.out.println("生成的行数：" + (rowIndex - 1));
                System.out.println("实际总数量：" + currentTotal);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}