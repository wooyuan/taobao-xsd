import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class SimpleExcelGenerator {

    public static void main(String[] args) {
        String filePath = "D:\\2.xlsx";
        int targetTotal = 110324;
        int minQty = 1;
        int maxQty = 28;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            Row headerRow = sheet.createRow(0);
            Cell headerCell1 = headerRow.createCell(0);
            headerCell1.setCellValue("Barcode");
            Cell headerCell2 = headerRow.createCell(1);
            headerCell2.setCellValue("Quantity");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerCell1.setCellStyle(headerStyle);
            headerCell2.setCellStyle(headerStyle);

            Random random = new Random();
            int currentTotal = 0;
            int rowIndex = 1;

            while (currentTotal < targetTotal) {
                int remaining = targetTotal - currentTotal;
                int qty = Math.min(random.nextInt(maxQty - minQty + 1) + minQty, remaining);
                
                String barcode = String.format("BAR-%s-%04d", "20260123", rowIndex);
                
                Row dataRow = sheet.createRow(rowIndex);
                Cell barcodeCell = dataRow.createCell(0);
                barcodeCell.setCellValue(barcode);
                Cell qtyCell = dataRow.createCell(1);
                qtyCell.setCellValue(qty);
                
                currentTotal += qty;
                rowIndex++;
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                System.out.println("Excel generated successfully! Path: " + filePath);
                System.out.println("Rows generated: " + (rowIndex - 1));
                System.out.println("Total quantity: " + currentTotal);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}