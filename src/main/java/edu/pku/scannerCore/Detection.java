package edu.pku.scannerCore;


import edu.pku.model.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/*
    对某个url执行检测的类, 也是基于爬虫上传payload的检测
 */
public class Detection implements Runnable {
    private static final Logger logger = LogManager.getLogger(Detection.class);

    private final String url; // 保存的当前需要探测的url的引用
    private final FormInfo formInfo; // 保存的当前需要探测的页面里面的表单的引用
    private final ArrayList<Poc> pocs; // 保存的当前需要使用的poc的列表
    private int poc_index; // 下一个要使用的poc

    private final ArrayList<Vulnerability> vulnerabilities; // 探测到的漏洞列表

    private Detection(String url, FormInfo formInfo, ArrayList<Poc> pocs) {// 构造器
        this.url = url;
        this.formInfo = formInfo;
        this.pocs = pocs;
        this.poc_index = 0;
        this.vulnerabilities = new ArrayList<>();
    }

    public void run() {
        // http客户端
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        while (true) {
            Poc poc;
            // 获取下一个要探测的poc, 这一块需要访问临界区, 需要同步
            synchronized (this) {
                if (poc_index < 0 || poc_index >= pocs.size()) break;
                poc = pocs.get(poc_index);
                ++poc_index;
            }
            if (poc == null) break;


            //根据poc和form表单发送请求访问
            try {
                CloseableHttpResponse response;
                for (int i = 0; i < formInfo.getFormFields().size(); ++i) {// 首先枚举payload在表单对应的位置
                    if (!formInfo.getFormFields().get(i).type.equals("text")) {
                        continue;
                    }

                    // 保存这个poc对应的请求
                    ArrayList<CloseableHttpResponse> responses = new ArrayList<>();
                    ArrayList<String> requests = new ArrayList<>();

                    for (PocRule pocRule : poc.getRules()) {// 枚举每个rule
                        // get请求的话
                        if (formInfo.getMethod().equals("get")) {
                            URI uri = setGetParam(i, pocRule.getBody());
                            if (uri == null) continue;
                            HttpGet httpGet = new HttpGet(uri);
                            if (pocRule.getHeaders() != null) {
                                for (Map.Entry<String, String> header : pocRule.getHeaders().entrySet()) {
                                    httpGet.addHeader(header.getKey(), header.getValue());
                                }
                            }
                            // 执行请求
                            response = httpClient.execute(httpGet);
                            // 保存一下对应的响应和请求
                            requests.add(httpGet + "\n" + Arrays.toString(httpGet.getAllHeaders()));
                            responses.add(response);
                        } else if (formInfo.getMethod().equals("post")) {// post请求的话
                            UrlEncodedFormEntity param = setPostParam(i, pocRule.getBody());
                            if (param == null) continue;
                            HttpPost httpPost = new HttpPost(formInfo.getAction());
                            httpPost.setEntity(param);
                            if (pocRule.getHeaders() != null) {
                                for (Map.Entry<String, String> header : pocRule.getHeaders().entrySet()) {
                                    httpPost.addHeader(header.getKey(), header.getValue());
                                }
                            }
                            // 执行请求
                            response = httpClient.execute(httpPost);
                            // 保存一下对应的响应和请求
                            responses.add(response);
                            requests.add(httpPost + "\n" + Arrays.toString(httpPost.getAllHeaders()) + "\n" + EntityUtils.toString(httpPost.getEntity()));
                        }
                    }
                    ArrayList<String> entries = new ArrayList<>();
                    for (CloseableHttpResponse res : responses) {
                        entries.add(EntityUtils.toString(res.getEntity()));
                    }
                    // 将每个pocRule发完之后, 就进行判断了
                    if (judgeOK(responses, entries, poc)) {
                        // 判断成功的话,就将漏洞加入扫描的漏洞列表里面
                        addVulnerability(entries, poc, formInfo.getFormFields().get(i).name, poc.getRules().get(0).getBody(), requests);
                    }
                }

            } catch (Exception e) {
                logger.error(e);
            }

        }

        // 最后关闭http客户端
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("httpclient close error: " + e);
        }
    }


    // 生成参数
    private List<NameValuePair> getParam(int fieldIndex, String payload) throws URISyntaxException {
        List<NameValuePair> list = new ArrayList<>();
        for (int i = 0; i < formInfo.getFormFields().size(); ++i) {
            BasicNameValuePair param;
            FormField field = formInfo.getFormFields().get(i);
            if (fieldIndex == i) {
                param = new BasicNameValuePair(field.getName(), payload);
            } else {
                param = new BasicNameValuePair(field.getName(), field.getValue());
            }
            list.add(param);
        }
        return list;
    }

    // 设置某个param为payload, 生成http get的参数
    private URI setGetParam(int fieldIndex, String payload) {
        try {
            URIBuilder uriBuilder = new URIBuilder(formInfo.getAction());
            uriBuilder.setParameters(getParam(fieldIndex, payload));

            URI i = uriBuilder.build();
            return i;
        } catch (Exception e) {
            return null;
        }
    }

    // 生成http post的参数
    private UrlEncodedFormEntity setPostParam(int fieldIndex, String payload) {
        try {
            return new UrlEncodedFormEntity(getParam(fieldIndex, payload));
        } catch (Exception e) {
            return null;
        }
    }

    // 判断是否攻击成功
    // 三种: response.status, response.body.bcontains, lengthDiff
    private boolean judgeOK(ArrayList<CloseableHttpResponse> responses, ArrayList<String> entries, Poc poc) {
        int lastIndex = url.lastIndexOf("/");
        String path = url.substring(lastIndex+1, url.length()-4);
        if (poc.getRules().size() < 1 || responses.size() < 1) return false;
        PocRule pocRule = poc.getRules().get(0);
        String expression = pocRule.getExpression();
        if (expression.contains("response.status")) {
            if (responses.get(0).getStatusLine().getStatusCode() != 200) return false;
            if (expression.contains(path)) return true;
        }
        if (expression.contains("response.body.bcontains")) {
            int start = expression.indexOf("'");
            int end = expression.lastIndexOf("'");
            try {
                if (EntityUtils.toString(responses.get(0).getEntity()).contains(expression.substring(start, end))) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }
        if (expression.contains("lengthDiff")) {
            if (responses.size() < 1) return false;
            //long len = responses.get(0).getEntity().getContentLength();
            long len = responses.get(0).toString().length();
            for (int i = 1; i < responses.size(); ++i) {
                long len2 = responses.get(i).toString().length();
                if (responses.get(i).toString().length() == len) return false;
            }
        }
        return true;
    }

    // 如果攻击成功, 将漏洞保存, 需要同步的方法
    private synchronized void addVulnerability(ArrayList<String> entries, Poc poc, String formKey, String formvalue, ArrayList<String> requests) {
        try {
            ArrayList<String> responses_str = new ArrayList<>();
            responses_str.addAll(entries);
            Vulnerability vulnerability = new Vulnerability(
                    new Date(), formInfo.getAction(), formInfo, poc, poc.getName(), poc.getRules().get(0).getBody(), formKey, formvalue, requests, responses_str);
            this.vulnerabilities.add(vulnerability);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    // 对外提供的调用接口, 负责使用poc列表进行探测,
    public static ArrayList<Vulnerability> detection(String url, FormInfo formInfo, ArrayList<Poc> pocs, int threadNum) {
        Detection detection = new Detection(url, formInfo, pocs);
        detection.run();
        return detection.vulnerabilities;
    }
}
