package edu.pku.controller;

import edu.pku.model.FormInfo;
import edu.pku.model.Vulnerability;
import edu.pku.scannerCore.ScannerMainThread;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@Controller
@RequestMapping("/result")
public class ResultController {

    @RequestMapping("/poc")
    @ResponseBody
    public String getPOC(HttpSession session) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return "null";
        if (mainThread.getPocs() != null)
            return mainThread.getPocs().toString();
        else return "null";
    }

    @RequestMapping("/status")
    @ResponseBody
    public String status(HttpSession session) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return "null";
        return mainThread.getStatusMsg();
    }

    @RequestMapping("/urls")
    @ResponseBody
    public String[] urls(HttpSession session) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return null;
        return mainThread.getUrls();
    }

    @RequestMapping("/forminfos")
    @ResponseBody
    public FormInfo[] forminfos(HttpSession session) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return null;
        return mainThread.getFormInfos();
    }

    @RequestMapping("/results")
    @ResponseBody
    public String results(HttpSession session) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return "null";
        return mainThread.getResults();
    }

    @RequestMapping("/result")
    @ResponseBody
    public ArrayList<Vulnerability> result(HttpSession session, @RequestParam Integer index) {
        ScannerMainThread mainThread = (ScannerMainThread) session.getAttribute("process");
        if (mainThread == null) return null;
        return mainThread.getResult(index);
    }


}
