package edu.pku.scannerCore;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.seimicrawler.xpath.JXDocument;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * 爬虫类, 通过一个初始的url, 不断进行爬取, 抓取到网站所有的url, 也相当于是一种BFS搜索
 * 使用多线程并发爬取, 提高爬取速度
 */

public class UrlCrawler implements Runnable {
    private static final Logger logger = LogManager.getLogger(UrlCrawler.class);

    private final CountDownLatch countDownLatch; // 保存当前运行的线程数, 都是原子操作

    // 已经访问过的url
    Set<String> urlVisited;
    // 将要访问的url
    Set<String> urlVisiting;

    private UrlCrawler(String urlStart, CountDownLatch countDownLatch) { // 构造器
        urlVisited = new HashSet<>();
        urlVisiting = new HashSet<>();
        urlVisiting.add(urlStart);
        this.countDownLatch = countDownLatch;
    }

    // 需要同步的方法, 获取下一个没有访问的url, 如果到了最后就返回null(线程也停止了)
    // 并且把这个url设置为已访问
    private synchronized String getNextUrl() {
        Iterator<String> it = urlVisiting.iterator();
        if (it.hasNext()) {
            String res = it.next();
            urlVisiting.remove(res);
            urlVisited.add(res);
            return res;
        } else return null;
    }


    // 需要同步的方法, 将当前的url列表插入待访问的集合中
    private synchronized void addUrls(ArrayList<String> urls) {
        for (String url : urls) {
            // 如果没有被访问过, 并且不在等待列表里面, 就加入等待访问的列表
            if (!urlVisited.contains(url))
                urlVisiting.add(url);
        }
    }

    public void run() { // 每个线程的运行函数
        String nowUrl;
        // http客户端
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        // http响应
        CloseableHttpResponse response = null;

        while (true) {
            nowUrl = getNextUrl();// 每次从待访问列表拿出一个url

            // 如果为空, 就等待0.3s, 等待后还为空, 就结束当前进程
            if(nowUrl == null){
                try{
                    Thread.sleep(300);
                }catch (Exception e){
                    logger.error(e);
                }
                nowUrl = getNextUrl();
            }
            if(nowUrl == null)break;
            if(nowUrl.contains("github") || nowUrl.contains("twitter"))continue;
            // 创建GET请求
            HttpGet httpGet = new HttpGet(nowUrl);
            //发送请求访问, 并且获取response里面的所有超链接
            try {
                response = httpClient.execute(httpGet);
                int code = response.getStatusLine().getStatusCode();
                if (code == 200 || code == 204 || code == 304) {
                    // 解析html
                    JXDocument jxDocument = JXDocument.create(EntityUtils.toString(response.getEntity()));
                    // 查找所有a标签的href
                    List<Object> rs = jxDocument.sel("//a/@href");
                    // 所有新的url列表
                    ArrayList<String> new_url = new ArrayList<>();
                    for (Object o : rs) {
                        String url = o.toString();
                        // 绝对地址
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            new_url.add(url);
                        } else { // 相对地址需要转化为绝对地址
                            if (url.equals("#") || url.startsWith("javascript:")) continue;
                            try {
                                URI base = new URI(nowUrl);
                                URI abs = base.resolve(url);
                                new_url.add(abs.toURL().toString());
                            } catch (Exception e) {
                                logger.error(e);
                            }
                        }
                    }
                    // 将超链接加入待访问列表, 继续下一次循环, (加入的时候会判断是否已经访问了)
                    addUrls(new_url);
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


    // 提供的唯一静态接口, 参数为开始的url, 最大线程数量, 返回所有url的列表
    public static String[] crawler(String startUrl, int threadNum) {
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        UrlCrawler urlCrawler = new UrlCrawler(startUrl, countDownLatch);
        // 初始化线程池, 并且开始子线程爬取
        Thread[] threadPool = new Thread[threadNum];
        for (int i = 0; i < threadNum; ++i) {
            threadPool[i] = new Thread(urlCrawler);
            threadPool[i].start();
        }

        // 等待所有子线程结束
        try{
            countDownLatch.await();
        }catch (Exception e){
            logger.error(e);
        }
        return urlCrawler.urlVisited.toArray(new String[0]);
    }

    // for test
//    public static void main(String[] args) {
//        String[] s = Crawler.crawler("http://127.0.0.1/pikachu/", 6);
//        for (String value : s) {
//            System.out.println(value);
//        }
//    }
}