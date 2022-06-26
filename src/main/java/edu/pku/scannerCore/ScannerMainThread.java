package edu.pku.scannerCore;

import edu.pku.model.FormInfo;
import edu.pku.model.Poc;
import edu.pku.model.Vulnerability;
import edu.pku.utils.POCLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class ScannerMainThread extends Thread {// 扫描器的主线程

    // 首先是线程初始化时的参数
    private final String startUrl; // 初始url
    private final String targetType; //目标类型
    private final Integer threadNum; // 最大的线程数量
    private final Set<String> pocParam;// 加载的POC参数

    // 运行时用到的变量
    private ArrayList<Poc> pocs; //加载的poc列表

    private String[] urls;// 目标的url列表
    private FormInfo[] formInfos; // 每个目标url里面含有的表单信息

    // 给查询用的结果
    private String statusMessage;//当前状态信息
    private ArrayList<Vulnerability>[] results;// 探测的漏洞结果

    // 构造器
    public ScannerMainThread(String url, String targetType, Integer threadNum,
                             Set<String> pocParam) {
        this.startUrl = url;
        this.targetType = targetType;
        this.threadNum = threadNum;
        this.pocParam = pocParam;
        this.statusMessage = "Thread is Ready to run";
    }

    // 线程执行的函数
    public void run() {
        // 加载POC文件
        this.statusMessage = "Loading the poc files";
        this.pocs = POCLoader.loadPOC(pocParam);
        this.statusMessage = "Loaded the poc files";

        // 只爬取一个网站
        this.urls = new String[1];
        this.urls[0] = startUrl;

        // 爬取url后, 处理每个页面的表单信息
        this.statusMessage = "Crawling the form information";
        this.formInfos = FormCrawler.crawler(urls, threadNum);
        this.statusMessage = "Crawling the form information over";

        // 挨个探测url
        this.statusMessage = "detecting Vulnerabilities start";

        this.results = new ArrayList[urls.length];
        for (int i = 0; i < urls.length; ++i) {
            this.statusMessage = "detecting url: " + urls[i];
            results[i] = null;
            if (formInfos[i] == null) continue;// 没有表单域我们就假定它没有漏洞
            // 执行检测
            results[i] = Detection.detection(urls[i], formInfos[i], pocs, threadNum);
        }

        this.statusMessage = "over";
    }

    public String getStatusMsg() {
        return this.statusMessage;
    }

    public ArrayList<Poc> getPocs() {
        return pocs;
    }

    public String[] getUrls() {
        return urls;
    }

    public String getUrl(int index) {
        if (urls == null || index < 0 || index > urls.length) return null;
        return urls[index];
    }

    public FormInfo[] getFormInfos() {
        return formInfos;
    }

    public String getResults() {
        return Arrays.toString(results);
    }

    public ArrayList<Vulnerability> getResult(int index) {
        if (results==null || index < 0 || index > results.length) return null;
        return results[index];
    }
}
