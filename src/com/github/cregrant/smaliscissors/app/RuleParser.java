package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;
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
    private final Pattern patScript = Pattern.compile("SCRIPT:\\R(?:\\s{4})?(.+)");
    private final Pattern patIsSmaliNeeded = Pattern.compile("SMALI_NEEDED:\\R(?:\\s{4})?(.+)");
    private final Pattern patMainClass = Pattern.compile("MAIN_CLASS:\\R(?:\\s{4})?(.+)");
    private final Pattern patEntrance = Pattern.compile("ENTRANCE:\\R(?:\\s{4})?(.+)");
    private final Pattern patParam = Pattern.compile("PARAM:\\R(?:\\s{4})?(.+)");
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
        getType();
        switch (rule.type) {
            case "PACKAGE":
            case "MIN_ENGINE_VER":
            case "AUTHOR":
                break;
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

    private void getType() {
        int k = 1;
        StringBuilder sb = new StringBuilder(17);
        char ch;
        while ((ch = patch.charAt(k))!=']') {
            sb.append(ch);
            k++;
        }
        rule.type = sb.toString();
    }

    void matchRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.targetArr = regex.matchMultiLines(patTarget, patch, "target");
        if (rule.targetArr.get(0).endsWith("xml"))
            rule.isXml = true;
        else if (rule.targetArr.get(0).endsWith("smali"))
            rule.isSmali = true;

        if (rule.targetArr.size() == 1) {
            rule.target = rule.targetArr.get(0);
            rule.targetArr = null;
        }
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.replacement = regex.matchSingleLine(patReplacement, patch);
        rule.isRegex = Boolean.parseBoolean(regex.matchSingleLine(patRegexEnabled, patch).trim());
        fixTarget();
    }

    void assignRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.target = regex.globToRegex(regex.matchSingleLine(patTarget, patch));
        if (Prefs.arch_device.equals("pc"))
            rule.target = rule.target.replace("/", "\\\\");
        if (rule.target.endsWith("xml"))
            rule.isXml = true;
        else if (rule.target.endsWith("smali"))
            rule.isSmali = true;
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = Boolean.getBoolean(regex.matchSingleLine(patRegexEnabled, patch).trim());
        rule.assignments = regex.matchMultiLines(patAssignment, patch, "assign");
        fixTarget();
    }

    void addRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.source = regex.matchSingleLine(patSource, patch);
        rule.extract = Boolean.parseBoolean(regex.matchSingleLine(patExtract, patch).trim());
        rule.target = regex.globToRegex(regex.matchSingleLine(patTarget, patch));
    }

    private void removeRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.target = regex.globToRegex(regex.matchSingleLine(patTarget, patch));
    }

    private void dummyRule() {
        rule.name = regex.matchSingleLine(patName, patch);
    }

    private void dexRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.script = regex.matchSingleLine(patScript, patch);
        rule.isSmali = Boolean.getBoolean(regex.matchSingleLine(patIsSmaliNeeded, patch));
        rule.mainClass = regex.matchSingleLine(patMainClass, patch);
        rule.entrance = regex.matchSingleLine(patEntrance, patch);
        rule.param = regex.matchSingleLine(patParam, patch);
    }

    private void gotoRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.goTo = regex.matchSingleLine(patGoto, patch);
    }

    private void matchGotoRule() {
        rule.name = regex.matchSingleLine(patName, patch);
        rule.target = regex.globToRegex(regex.matchSingleLine(patTarget, patch));
        if (Prefs.arch_device.equals("pc"))
            rule.target = rule.target.replace("/", "\\\\");
        if (rule.target.endsWith("xml"))
            rule.isXml = true;
        else if (rule.target.endsWith("smali"))
            rule.isSmali = true;
        rule.match = regex.matchSingleLine(patMatch, patch);
        rule.isRegex = Boolean.getBoolean(regex.matchSingleLine(patRegexEnabled, patch).trim());
        rule.goTo = regex.matchSingleLine(patGoto, patch);
        fixTarget();
    }

    private void fixTarget() {
        if (rule.isXml) {
            if (rule.targetArr==null)
                rule.target = rule.target.replace("><", ">(?:\\\\s*?)<");
            else {
                ArrayList<String> fixedTargetArr = new ArrayList<>();
                for (String trg : rule.targetArr)
                    fixedTargetArr.add(trg.replace("><", ">(?:\\\\s*?)<"));
                rule.targetArr = fixedTargetArr;
            }
        }
    }
}
