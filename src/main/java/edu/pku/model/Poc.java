package edu.pku.model;

import java.util.ArrayList;
import java.util.Map;

public class Poc {
    private String name;
    private Map<String, String> set;
    private ArrayList<PocRule> rules;
    private Map<String, String> details;

    public Poc(String name, Map<String, String> set, ArrayList<PocRule> rules, Map<String, String> details) {
        this.name = name;
        this.set = set;
        this.rules = rules;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<PocRule> getRules() {
        return rules;
    }

    public Map<String, String> getSet() {
        return set;
    }

    public void setRules(ArrayList<PocRule> rules) {
        this.rules = rules;
    }

    public void setSet(Map<String, String> set) {
        this.set = set;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("name: " + name + "\nset: \n");

        if(set!=null)for(Map.Entry<String,String> e: set.entrySet()) {
            res.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        res.append("rules:\n");
        for(PocRule rule: rules){
            res.append(rule);
        }
        res.append("\ndetail:\n");
        if(details!=null)for(Map.Entry<String,String> e:details.entrySet()){
            res.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        return res.toString();
    }
}
