package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;

class Patch {
    ArrayList<Rule> rules = new ArrayList<>();
    int currentRuleNum = 0;

    public void addRule(Rule rule) {
        if (rule!=null)
            rules.add(rule);
    }

    public void setRuleNum(int num){
        currentRuleNum = num;
    }

    Rule getNextRule() {
        if (currentRuleNum < rules.size()) {
            Rule rule = rules.get(currentRuleNum);
            currentRuleNum++;
            return rule;
        }
        return null;
    }
}
