package com.github.cregrant.smaliscissors.app;

import java.util.regex.Pattern;

import static java.lang.System.out;

class RuleParser {
    private final Pattern patSource = Pattern.compile("SOURCE:\\n(?:\\s{4})?(.+)");
    private final Pattern patExtract = Pattern.compile("EXTRACT:\\R(?:\\s{4})?(.+)");
    private final Pattern patAssignment = Pattern.compile("\\R(?:\\s{4})?(.+?=\\$\\{GROUP\\d})");
    private final Pattern patReplacement = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
    private final Pattern patTarget = Pattern.compile("TARGET:\\R(?:\\s{4})?([\\s\\S]*?)\\R(?:(?:MATCH|EXTRACT):|\\[/)");
    private final Pattern patMatch = Pattern.compile("MATCH:\\R(.+)");
    private final Pattern patName = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patRegexEnabled = Pattern.compile("REGEX:\\R(?:\\s{4})?(.+)");
    private final Pattern patScript = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patIsSmaliNeeded = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patMainClass = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patEntrance = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patParam = Pattern.compile("NAME:\\R(?:\\s{4})?(.+)");
    private final Pattern patGoto = Pattern.compile("GOTO:\\R(?:\\s{4})?(.+)");
    private Rule rule;
    private String patch;
    private final Regex regex = new Regex();
    private int num = 0;

    Rule parseRule(String patchStr) {
        if (!Prefs.rules_AEmode) {
            out.println("TruePatcher mode on.");
        }
        rule = new Rule();
        patch = patchStr;
        rule.num = num;
        num++;
        final Pattern patDetect = Pattern.compile("\\[(.+?)][\\S\\s]*?\\[/.+?]");
        rule.type = regex.matchSingleLine(patDetect, patch);
        switch (rule.type) {
            case "MATCH_ASSIGN":
                assignRule();
                break;
            case "ADD_FILES":
                addRule();
                break;
            case "MATCH_REPLACE":
                matchRule();
                break;
            case "REMOVE_FILES":
                removeRule();
                break;
            case "DUMMY":
                dummyRule();
                break;
            case "EXECUTE_DEX":
                dexRule();
                break;
            case "GOTO":
                gotoRule();
                break;
            case "MATCH_GOTO":
                matchGotoRule();
                break;
        }
        if (rule.checkRuleIntegrity())
            return rule;
        return null;
    }

    void matchRule() {
        rule.targetArr = regex.matchMultiLines(patTarget, patch, "target");
        if (rule.targetArr.size() == 1) {
            rule.target = rule.targetArr.get(0);
            rule.targetArr = null;
        }
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.replacement = regex.matchSingleLine(patReplacement, patch);
        rule.isRegex = Boolean.parseBoolean(regex.matchSingleLine(patRegexEnabled, patch).strip());
    }

    void assignRule() {
        rule.target = regex.matchSingleLine(patTarget, patch);
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = Boolean.getBoolean(regex.matchSingleLine(patRegexEnabled, patch).strip());
        rule.assignments = regex.matchMultiLines(patAssignment, patch, "assign");
    }

    void addRule() {
        rule.source = regex.matchSingleLine(patSource, patch);
        rule.extract = Boolean.parseBoolean(regex.matchSingleLine(patExtract, patch).strip());
        rule.target = regex.matchSingleLine(patTarget, patch);
    }

    private void removeRule() {
        rule.target = regex.matchSingleLine(patTarget, patch);
    }

    private void dummyRule() {
        rule.name = regex.matchSingleLine(patName, patch);
    }

    private void dexRule() {
        rule.script = regex.matchSingleLine(patScript, patch);
        rule.isSmaliNeeded = regex.matchSingleLine(patIsSmaliNeeded, patch);
        rule.mainClass = regex.matchSingleLine(patMainClass, patch);
        rule.entrance = regex.matchSingleLine(patEntrance, patch);
        rule.param = regex.matchSingleLine(patParam, patch);
    }

    private void gotoRule() {
        rule.goTo = regex.matchSingleLine(patGoto, patch);
    }

    private void matchGotoRule() {
        rule.target = regex.matchSingleLine(patTarget, patch);
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = Boolean.getBoolean(regex.matchSingleLine(patRegexEnabled, patch).strip());
        rule.goTo = regex.matchSingleLine(patGoto, patch);
    }
}
