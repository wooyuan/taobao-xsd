package com.taobao.logistics.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Created by ShiShiDaWei on 2021/8/14.
 */
@Slf4j
public class RequestUtils {


    public static void doAsyncGet(String url) {
        String response = "";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request build = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = okHttpClient.newCall(build);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println(response.body().string());
                }
            }
        });
    }


    /**
     *
     * @param url
     * @param map
     * @return
     */
    public static String doFormAsyncPost(String url, Map<String, String> map) {
        String response = "";
        OkHttpClient okHttpClient = new OkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry :
                map.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        FormBody formBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Headers headers = response.headers();
                    for (int i = 0; i < headers.size(); i++) {
                        System.out.println(headers.name(i) + ":" + headers.value(i));
                    }
                    ResponseBody body = response.body();
                    assert body != null;
                    System.out.println("body = " + body.string());
                }
            }
        });

        return "";
    }


    /**
     * application/x-www-form-urlencoded
     * @param url
     * @param data
     * @return
     * @throws IOException
     */
    public static String doFormPost(String url, String data) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        //name=123&size=a
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            assert response.body() != null;
            return response.body().string();
        }
        log.debug("respose:{}", JSONObject.toJSONString(response));
        return "";
    }

    public static String doGet(String url) throws IOException {
        //url = http://cainiao.enjoyme.cn:9213/entrepot/getPrint?waybillCode=75814105412636
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }


    public static String request(String strUrl, String method, String reqdata, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        String UTF8 = "UTF-8";
        URL url = new URL(strUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoOutput(true);// 设置是否向HttpURLConnection输出, 不设默认为false
        httpURLConnection.setRequestMethod(method.toUpperCase());
        httpURLConnection.setConnectTimeout(connectTimeoutMs);
        httpURLConnection.setReadTimeout(readTimeoutMs);
        httpURLConnection.setRequestProperty("jobNum", "00039385");
        //设置使用标准编码格式编码参数的名-值对
        //httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");//设置参数类型是json格式
        httpURLConnection.connect();
        OutputStream outputStream = null;
        //post请求，从连接中得到一个输出流，通过输出流把数据写到服务器
        if (!"GET".equals(httpURLConnection.getRequestMethod())) {
            outputStream = httpURLConnection.getOutputStream();
            outputStream.write(reqdata.getBytes(UTF8));
        }
        InputStream inputStream = httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String resp = sb.toString();
        if (sb != null) {
            try {
                bufferedReader.close();
            } catch (IOException var18) {
                var18.printStackTrace();
            }
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException var17) {
                var17.printStackTrace();
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException var16) {
                var16.printStackTrace();
            }
        }

        return resp;
    }


    public static StringBuilder jointStr(final Map<String, String> data){
        Set<String> keySet = data.keySet();
        String[] toArray = keySet.toArray(new String[keySet.size()]);
        Arrays.sort(toArray);
        StringBuilder stringBuilder = new StringBuilder("&");
        for (String str :
                toArray) {
            stringBuilder.append(str).append("=").append(data.get(str)).append("&");
        }
        return stringBuilder;
    }


}
