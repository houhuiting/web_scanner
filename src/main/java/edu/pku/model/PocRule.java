package edu.pku.model;

import java.util.Map;

public class PocRule {
    /*
     * method: string 请求方法
     * path: string 请求的完整 Path，包括 querystring 等
     * headers: map[string]string 请求 HTTP 头，Rule 中指定的值会被覆盖到原始数据包的 HTTP 头中
     * body: string 请求的Body
     * follow_redirects: bool 是否允许跟随300跳转
     * expression: string
     * search: string
     */
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;
    private boolean follow_redirects;
    private String expression;
    private String search;

    public PocRule(String method, String path, Map<String, String> headers, String body, boolean follow_redirects,
                   String expression, String search) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.follow_redirects = follow_redirects;
        this.expression = expression;
        this.search = search;
    }

    public boolean isFollow_redirects() {
        return follow_redirects;
    }

    public String getBody() {
        return body;
    }

    public String getExpression() {
        return expression;
    }

    public String getMethod() {
        return method;
    }

    public String getSearch() {
        return search;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setFollow_redirects(boolean follow_redirects) {
        this.follow_redirects = follow_redirects;
    }

    public String getPath() {
        return path;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("  - method: ").append(this.method).append("\n");
        res.append("    path: ").append(this.path).append("\n");
        res.append("    headers:\n");
        if(headers!=null)for(Map.Entry<String,String> e: headers.entrySet()){
            res.append("      ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        res.append("    body: ").append(this.body).append("\n");
        res.append("    follow_redirects: ").append(this.follow_redirects).append("\n");
        res.append("    expression: ").append(this.expression).append("\n");
        res.append("    search: ").append(this.search).append("\n");
        return res.toString();
    }
}
