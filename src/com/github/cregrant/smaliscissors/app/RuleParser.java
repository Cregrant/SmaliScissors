package com.github.cregrant.smaliscissors.app;

import java.util.regex.Pattern;

import static java.lang.System.out;

class RuleParser {
    final Pattern patSource = Pattern.compile("SOURCE:\\n(?:\\s{4})?(.+)");
    final Pattern patExtract = Pattern.compile("EXTRACT:\\R(?:\\s{4})?(.+)");
    final Pattern patAssignment = Pattern.compile("\\R(?:\\s{4})?(.+?=\\$\\{GROUP\\d})");
    final Pattern patReplacement = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
    final Pattern patTarget = Pattern.compile("TARGET:\\R(?:\\s{4})?([\\s\\S]*?)\\R(?:(?:MATCH|EXTRACT):|\\[/)");
    final Pattern patMatch = Pattern.compile("MATCH:\\R(.+)");
    final Pattern patRegexEnabled = Pattern.compile("REGEX:\\R(?:\\s{4})?(.+)");
    Regex regex = new Regex();
    int num = 0;

    Rule parseRule(String patch) {
        if (Prefs.rules_AEmode == 0) {
            out.println("TruePatcher mode on.");
        }
        final Pattern patDetect = Pattern.compile("\\[(.+?)][\\S\\s]*?\\[/.+?]");
        Rule rule = new Rule();
        rule.type = regex.matchSingleLine(patDetect, patch);
        rule.num = num;
        num++;
        switch (rule.type) {
            case "MATCH_ASSIGN":
                assignRule(rule, patch);
                break;
            case "ADD_FILES":
                addRule(rule, patch);
                break;
            case "MATCH_REPLACE":
                matchRule(rule, patch);
                break;
            case "REMOVE_FILES":
                removeRule(rule, patch);
                break;
        }
        if (rule.checkRuleIntegrity())
            return rule;
        return null;
    }

    void matchRule(Rule rule, String patch) {
        rule.targetArr = regex.matchMultiLines(patTarget, patch, "target");
        if (rule.targetArr.size() == 1) {
            rule.target = rule.targetArr.get(0);
            rule.targetArr = null;
        }
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.replacement = regex.matchSingleLine(patReplacement, patch);
        rule.isRegex = Boolean.parseBoolean(regex.matchSingleLine(patRegexEnabled, patch).strip());
    }

    void assignRule(Rule rule, String patch) {
        rule.target = regex.matchSingleLine(patTarget, patch);
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = Boolean.getBoolean(regex.matchSingleLine(patRegexEnabled, patch).strip());
        rule.assignments = regex.matchMultiLines(patAssignment, patch, "assign");
    }

    void addRule(Rule rule, String patch) {
        rule.source = regex.matchSingleLine(patSource, patch);
        rule.extract = Boolean.parseBoolean(regex.matchSingleLine(patExtract, patch).strip());
        rule.target = regex.matchSingleLine(patTarget, patch);
    }

    private void removeRule(Rule rule, String patch) {
        rule.target = regex.matchSingleLine(patTarget, patch);
    }
}
