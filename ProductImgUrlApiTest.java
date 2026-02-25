import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 产品图片URL API测试类
 * 用于测试通过HTTP请求触发同步任务的功能
 */
public class ProductImgUrlApiTest {

    /**
     * 主方法，用于测试API调用
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("开始测试产品图片URL同步任务API...");
        
        // 本地测试地址 - 请根据实际部署情况修改
        String baseUrl = "http://localhost:8080";
        
        // 健康检查接口
        testHealthCheck(baseUrl);
        
        // 同步任务触发接口
        testSyncProductImgUrl(baseUrl);
    }
    
    /**
     * 测试健康检查接口
     */
    private static void testHealthCheck(String baseUrl) {
        String apiUrl = baseUrl + "/api/product-img/health";
        System.out.println("\n测试健康检查接口: " + apiUrl);
        
        try {
            String result = sendGetRequest(apiUrl);
            System.out.println("响应结果: " + result);
        } catch (Exception e) {
            System.out.println("健康检查失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试同步任务触发接口
     */
    private static void testSyncProductImgUrl(String baseUrl) {
        String apiUrl = baseUrl + "/api/product-img/sync";
        System.out.println("\n测试同步任务触发接口: " + apiUrl);
        System.out.println("注意: 此操作会触发实际的同步任务，请确保服务正在运行。");
        
        try {
            String result = sendGetRequest(apiUrl);
            System.out.println("响应结果: " + result);
        } catch (Exception e) {
            System.out.println("触发同步任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送GET请求
     */
    private static String sendGetRequest(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(60000); // 同步任务可能需要较长时间
        
        int responseCode = connection.getResponseCode();
        System.out.println("HTTP 响应码: " + responseCode);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        
        return response.toString();
    }
}
