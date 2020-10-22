package com.github.cregrant.smaliscissors.app;

import java.util.regex.Pattern;

import static java.lang.System.out;

class RuleParser {
    final Pattern patSource = Pattern.compile("SOURCE:\\n(.+)");
    final Pattern patExtract = Pattern.compile("EXTRACT:\\R(.+)");
    final Pattern patAssign = Pattern.compile("\\R*?(.+?=\\$\\{GROUP\\d})");
    final Pattern patReplacement = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
    final Pattern patTarget = Pattern.compile("TARGET:\\R([\\s\\S]*?)(?:(?:MATCH|EXTRACT):|\\[/)");
    final Pattern patMatch = Pattern.compile("MATCH:\\R(.+)");
    final Pattern patRegexEnabled = Pattern.compile("REGEX:\\R(.+)");
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
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.replacement = regex.matchSingleLine(patReplacement, patch);
        rule.isRegex = regex.matchSingleLine(patRegexEnabled, patch);

    }

    void assignRule(Rule rule, String patch) {
        rule.target = regex.matchSingleLine(patTarget, patch);
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = regex.matchSingleLine(patRegexEnabled, patch);
        rule.assignments = regex.matchMultiLines(patAssign, patch, "assign");
    }

    void addRule(Rule rule, String patch) {
        rule.source = regex.matchSingleLine(patSource, patch);
        rule.extract = regex.matchSingleLine(patExtract, patch);
        rule.target = regex.matchSingleLine(patTarget, patch);
    }

    private void removeRule(Rule rule, String patch) {
        rule.target = regex.matchSingleLine(patTarget, patch);
    }
}
