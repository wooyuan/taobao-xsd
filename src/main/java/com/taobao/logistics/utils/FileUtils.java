package com.taobao.logistics.utils;

import com.taobao.logistics.entity.wx.GoodsPic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by ShiShiDaWei on 2021/11/1.
 */
@Slf4j
public class FileUtils {
    private static final String IMAGE_PNG = "image/png";

    private static final String IMAGE_JPG = "image/jpg";

    private static final String IMAGE_JPEG = "image/jpeg";

    private static final String IMAGE_BMP = "image/bmp";

    private static final String IMAGE_GIF = "image/gif";

    public static final String[] IMAGE_EXTENSION = { "bmp", "gif", "jpg", "jpeg", "png" };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            default:
                return "";
        }
    }


    public static List<GoodsPic> getUploadFileMD5(MultipartFile[] file) {
        List<GoodsPic> pics = new ArrayList<>();
        for (MultipartFile f :
                file) {
            if (f.isEmpty() || f.getSize() == 0) {
                continue;
            }
            if ((f.getSize() / 1024D) > 2048) {
                return pics;
            }
            String filename = f.getOriginalFilename();
            assert filename != null;
            String extension = getExtension(Objects.requireNonNull(f.getContentType()));
            if (extension.length() == 0) {
                log.warn("===Upload File Layout Error===");
                continue;
            }
            try(InputStream inputStream = f.getInputStream()) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] line = new byte[1024];
                int len;
                while ( (len = inputStream.read(line)) != -1) {
                    md5.update(line, 0, len);
                }
                //md5验证文件完整性
                byte[] array = md5.digest();
                StringBuilder sb = new StringBuilder();
                for (byte item : array) {
                    sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
                }
                String md5Vaule = sb.toString().toUpperCase();
//                System.out.println(md5Vaule);
                log.debug("File MD5 : {}", md5Vaule);
                GoodsPic goods = new GoodsPic();
                goods.setGoodsPicMD5(md5Vaule);
                pics.add(goods);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                log.error("上传文件异常!{}", e.getMessage());
            }
        }
        return pics;
    }


    public static String  getUploadFileMD5Fh(MultipartFile file) {
        String md5Vaule = "";

            String filename = file.getOriginalFilename();
            assert filename != null;
            String extension = getExtension(Objects.requireNonNull(file.getContentType()));
            if (extension.length() == 0) {
                log.warn("===Upload File Layout Error===");

            }
            try(InputStream inputStream = file.getInputStream()) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] line = new byte[1024];
                int len;
                while ( (len = inputStream.read(line)) != -1) {
                    md5.update(line, 0, len);
                }
                //md5验证文件完整性
                byte[] array = md5.digest();
                StringBuilder sb = new StringBuilder();
                for (byte item : array) {
                    sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
                }
                  md5Vaule = sb.toString().toUpperCase();
//                System.out.println(md5Vaule);
                log.debug("File MD5 : {}", md5Vaule);
                return md5Vaule ;
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                log.error("上传文件异常!{}", e.getMessage());
            }

        return md5Vaule;
    }


    public static GoodsPic uploadFile(MultipartFile f, String path) {
        String filename = f.getOriginalFilename();
        assert filename != null;
        int indexOf = filename.lastIndexOf(".");
        String suffix = filename.substring(indexOf);
        long epochMilli = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        String fName = epochMilli + suffix;
        File newFile = new File(path + File.separator + fName);
        if (!newFile.getParentFile().exists()) {
            boolean mkdirs = newFile.getParentFile().mkdirs();
            log.debug("ParentFile mkdirs {}, {}", mkdirs, path + fName);
        }
        try(InputStream inputStream = f.getInputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newFile))) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] line = new byte[1024];
            int len;
            while ( (len = inputStream.read(line)) != -1) {
                bufferedOutputStream.write(line, 0 , len);
                md5.update(line, 0, len);
            }
            bufferedOutputStream.flush();
            //md5验证文件完整性
            byte[] array = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            String md5Vaule = sb.toString().toUpperCase();
//            System.out.println(md5Vaule);
            log.debug("File MD5 : {}", md5Vaule);
            GoodsPic goods = new GoodsPic();
            goods.setGoodsPicMD5(md5Vaule);
            goods.setFileUri("/upload/" + fName);
            goods.setGoodsPicNo(fName);
//            goods.setCreateDate(new Date());
            return goods;
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error("上传文件异常!{}", e.getMessage());
        }
        return null;
    }


    public static String uploadFileFh(MultipartFile f, String path) {
        String filename = f.getOriginalFilename();
        assert filename != null;
        int indexOf = filename.lastIndexOf(".");
        String suffix = filename.substring(indexOf);
        long epochMilli = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        String fName = epochMilli + suffix;
        File newFile = new File(path + File.separator + fName);
        if (!newFile.getParentFile().exists()) {
            boolean mkdirs = newFile.getParentFile().mkdirs();
            log.debug("ParentFile mkdirs {}, {}", mkdirs, path + fName);
        }
        try(InputStream inputStream = f.getInputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newFile))) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] line = new byte[1024];
            int len;
            while ( (len = inputStream.read(line)) != -1) {
                bufferedOutputStream.write(line, 0 , len);
                md5.update(line, 0, len);
            }
            bufferedOutputStream.flush();
            //md5验证文件完整性
            byte[] array = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            String md5Vaule = sb.toString().toUpperCase();
//            System.out.println(md5Vaule);
            log.debug("File MD5 : {}", md5Vaule);
//            goods.setCreateDate(new Date());
            return "/upload/" + fName;
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error("上传文件异常!{}", e.getMessage());
        }
        return null;
    }








}
