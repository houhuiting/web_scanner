package edu.pku.model;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.JXDocument;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/*
    一个有表单的页面, 里面包含的表单信息, 包括
    提交方法GET,POST,
    action: 目标的url
    表单编码: application/x-www-form-urlencoded(默认), multipart/form-data, text/plain
 */
public class FormInfo {
    private String method;
    private String action;
    private String enctype;
    private ArrayList<FormField> formFields;

    // 私有使用构造器
    private FormInfo() {
    }

    // 使用静态函数创建, 通过html解析
    public static FormInfo create(String baseUrl, String html) {
        FormInfo formInfo = new FormInfo();
        // 解析html
        JXDocument jxDocument = JXDocument.create(html);
        List<Object> res = jxDocument.sel("//form");
        if (res == null || res.size() < 1) return null;
        FormElement formElement = (FormElement) res.get(0);

        if (formElement == null) return null;
        formInfo.method = formElement.attributes().get("method");
        formInfo.action = formElement.attributes().get("action");
        // 变成绝对地址
        try {
            System.err.println(baseUrl);
            if (formInfo.action != null && formInfo.action.length() > 0) {
                URI base = new URI(baseUrl);
                URI abs = base.resolve(formInfo.action);
                formInfo.action = abs.toURL().toString();
            } else {
                formInfo.action = baseUrl;
            }
        } catch (Exception e) {
            return null;
        }
        // 表单编码
        formInfo.enctype = formElement.attributes().get("enctype");
        if (formInfo.enctype == null || formInfo.enctype.equals("")) {
            formInfo.enctype = "application/x-www-form-urlencoded";// 默认
        }

        // 表单域
        formInfo.formFields = new ArrayList<>();
        // input
        Elements elements = formElement.getElementsByTag("input");
        for (Element element : elements) {
            String name = element.attributes().get("name");
            String type = element.attributes().get("type");
            String value = element.attributes().get("value");
            formInfo.formFields.add(new FormField(name, type, value));
        }
        // 除了input, 还有可能是textarea
        elements = formElement.getElementsByTag("textarea");
        for (Element element : elements) {
            String name = element.attributes().get("name");
            String type = "text";
            String value = element.attributes().get("value");
            formInfo.formFields.add(new FormField(name, type, value));
        }

        return formInfo;
    }

    @Override
    public String toString() {
        return "FormInfo{" +
                "method='" + method + '\'' +
                ", action='" + action + '\'' +
                ", enctype='" + enctype + '\'' +
                ", formFields=" + formFields +
                '}';
    }

    public String getMethod() {
        return method;
    }

    public ArrayList<FormField> getFormFields() {
        return formFields;
    }

    public String getAction() {
        return action;
    }

    public String getEnctype() {
        return enctype;
    }

}


