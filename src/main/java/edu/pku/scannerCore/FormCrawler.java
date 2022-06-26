package edu.pku.scannerCore;


/*
    将每个url访问, 如果里面有表单就提取出来信息
 */

import edu.pku.model.FormInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class FormCrawler implements Runnable {
    private static final Logger logger = LogManager.getLogger(FormCrawler.class);

    private final CountDownLatch countDownLatch; // 保存当前运行的线程数, 都是原子操作

    // 要访问的url列表
    private String[] urls;
    // 下一个要扫描的url的index
    private int index;
    // 对应的表单列表
    private FormInfo[] formInfos;

    private FormCrawler(String[] urls, CountDownLatch countDownLatch) { // 构造器
        this.index = 0;
        this.urls = urls;
        this.countDownLatch = countDownLatch;
        this.formInfos = new FormInfo[urls.length];
    }


    public void run() { // 每个线程的运行函数
        // http客户端
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        // http响应
        CloseableHttpResponse response = null;

        while (true) {
            int nowIndex = -1;
            // 获取当前的index, 这一块需要访问临界区, 需要同步
            synchronized (this) {
                nowIndex = index;
                ++index;
            }
            if (nowIndex < 0 || nowIndex >= urls.length) break;
            formInfos[nowIndex] = null;

            // 创建http请求
            HttpGet httpGet = new HttpGet(urls[nowIndex]);
            //发送请求访问
            try {
                response = httpClient.execute(httpGet);
                int code = response.getStatusLine().getStatusCode();
                if (code == 200 || code == 204 || code == 304) {
                    // 解析html, 从中提取表单
                    formInfos[nowIndex] = FormInfo.create(urls[nowIndex], EntityUtils.toString(response.getEntity()));
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                try {
                    // 尝试关闭当前的相应类
                    if (response != null) response.close();
                    response = null;
                } catch (Exception e) {
                    logger.error(e);
                }
            }

        }

        // 最后关闭http客户端
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("httpclient close error: " + e);
        } finally {
            countDownLatch.countDown();
        }
    }

    // 提供的唯一静态接口, 参数为url列表, 最大线程数量, 返回所有url对应的表单信息
    public static FormInfo[] crawler(String[] urls, int threadNum) {
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        FormCrawler formCrawler = new FormCrawler(urls, countDownLatch);

        // 初始化线程池, 并且开始子线程爬取
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(formCrawler);
            threads[i].start();
        }
        // 等待所有子线程结束
        try {
            countDownLatch.await();
        } catch (Exception e) {
            logger.error(e);
        }
        return formCrawler.formInfos;
    }


}
