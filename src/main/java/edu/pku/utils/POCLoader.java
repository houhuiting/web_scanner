package edu.pku.utils;

import com.sun.org.apache.xpath.internal.operations.Bool;
import edu.pku.model.Poc;
import edu.pku.model.PocRule;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;

public class POCLoader {
    private static final Logger logger = LogManager.getLogger(POCLoader.class);

    public static ArrayList<Poc> loadPOC(Set<String> set){
        // 首先加载POC文件的列表
        Map<String,Object> yml_list = YmlUtils.yaml2Map("POC-list.yml");

        // res 存储结果
        ArrayList<Poc> res = new ArrayList<>();

        if(yml_list==null){
            logger.error("POC-list file is null!");
            return res;
        }
        for(Map.Entry<String,Object> entry : yml_list.entrySet()){
            // 如果不是设定类型的poc, 就跳过
            if(!set.contains(entry.getKey()))continue;
            Object value = entry.getValue();
            if(value instanceof ArrayList){// 加载这个类型所有的poc文件
                ArrayList<String> pocs = (ArrayList<String>) value;
                for(String pocFile : pocs){
                    Map<String,Object> pocMap = YmlUtils.yaml2Map(pocFile);
                    if(pocMap == null)logger.error("POC file '"+pocFile+"' is not Found!");
                    else {// 解析成POC类
                        Poc poc = parsePoc(pocMap);
                        if(poc!=null)res.add(poc);
                        else{
                            logger.error("parse poc file "+pocFile+" error");
                        }
                    }
                }
            }
        }
        return res;
    }

    public static Poc parsePoc(Map<String,Object> map){
        Object st=map.get("set");
        Map<String,String> mpSet = null;
        Map<String,String> mpDetail = null;

        if(st instanceof Map)mpSet = (Map<String, String>)st;
        st = map.get("detail");
        if(st instanceof Map)mpDetail = (Map<String, String>)st;

        Object rulesArr = map.get("rules");
        ArrayList<PocRule> rules = new ArrayList<>();
        if(rulesArr instanceof ArrayList){
            ArrayList<Map<String ,Object>>arr= (ArrayList<Map<String,Object>>)rulesArr;
            for(Map<String,Object> rulesMap: arr){
                PocRule pocRule=parsePocRule(rulesMap);
                if(pocRule!=null)rules.add(pocRule);
            }
        }
        if(rules.size() == 0)return null;
        return new Poc((String)map.get("name"), mpSet, rules, mpDetail);
    }

    public static PocRule parsePocRule(Map<String,Object> rulesMap){
        String method = (String)rulesMap.get("method");
        String path = (String)rulesMap.get("path");
        String expression = (String)rulesMap.get("expression");
        String body = (String)rulesMap.get("body");
        String search = (String)rulesMap.get("search");
        Object headersObj = rulesMap.get("headers");
        Map<String,String>headers = null;
        if(headersObj instanceof Map)headers = (Map<String, String>)headersObj;
        Object follow_redirects = rulesMap.get("follow_redirects");
        boolean follow = false;
        if(follow_redirects instanceof Boolean)follow=(Boolean)follow_redirects;

        // 方法默认GET, 如果path是null或者expression, 那就无效
        if(method==null)method = "GET";
        if(path==null||expression==null)return null;
        return new PocRule(method, path, headers,body,follow, expression,search);
    }

}
