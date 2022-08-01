package com.github.cregrant.smaliscissors.utils;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.utils.Regex.ResultFormat;
import com.github.cregrant.smaliscissors.structures.rules.*;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.utils.Regex.matchMultiLines;
import static com.github.cregrant.smaliscissors.utils.Regex.matchSingleLine;

public class RuleParser {
    private static final Pattern RULE = Pattern.compile("(\\[.+?]\\R(?:NAME|GOTO|SOURCE|SCRIPT|TARGET):[\\s\\S]*?\\[/.+?])");
    private static final Pattern SOURCE = Pattern.compile("SOURCE:\\s+(.+?)\\s*\\R");
    private static final Pattern EXTRACT = Pattern.compile("EXTRACT:\\s+(.+?)\\s*\\R");
    private static final Pattern ASSIGNMENT = Pattern.compile("ASSIGN:\\s+(.+?\\$\\{GROUP\\d{1,2}.*)");
    private static final Pattern REPLACEMENT = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
    private static final Pattern TARGET = Pattern.compile("TARGET:\\s+([\\s\\S]*?)\\s+(?:MATCH|EXTRACT|SOURCE|\\[)");
    private static final Pattern MATCH = Pattern.compile("MATCH:\\R(.+)");
    private static final Pattern PAT_NAME = Pattern.compile("NAME:\\s+(.+?)\\s*\\R");
    private static final Pattern REGEX = Pattern.compile("REGEX:\\s+(.+?)\\s*\\R");
    private static final Pattern SCRIPT = Pattern.compile("SCRIPT:\\s+(.+?)\\s*\\R");
    private static final Pattern SMALI_NEEDED = Pattern.compile("SMALI_NEEDED:\\s+(.+?)\\s*\\R");
    private static final Pattern MAIN_CLASS = Pattern.compile("MAIN_CLASS:\\s+(.+?)\\s*\\R");
    private static final Pattern ENTRANCE = Pattern.compile("ENTRANCE:\\s+(.+?)\\s*\\R");
    private static final Pattern PARAM = Pattern.compile("PARAM:\\s+(.+?)\\s*\\R");
    private static final Pattern GOTO = Pattern.compile("GOTO:\\s+(.+?)\\s*\\R");
    private final ArrayList<IRule> rules;
    private boolean isSmaliNeeded;
    private boolean isXmlNeeded;

    public RuleParser(String rawString) {
        ArrayList<String> tempArr = Regex.matchMultiLines(rawString, RULE, ResultFormat.FULL);
        rules = new ArrayList<>(tempArr.size());

        for (int i = 0; i < tempArr.size(); i++) {
            String raw = tempArr.get(i);
            IRule rule = parseRule(raw, i);
            if (!isSmaliNeeded)
                isSmaliNeeded = rule.smaliNeeded();
            if (!isXmlNeeded)
                isXmlNeeded = rule.xmlNeeded();
            rules.add(rule);
        }
    }

    public ArrayList<IRule> getRules() {
        return rules;
    }

    public boolean isSmaliNeeded() {
        return isSmaliNeeded;
    }

    public boolean isXmlNeeded() {
        return isXmlNeeded;
    }

    IRule parseRule(String patchStr, int num) {
        IRule rule;
        String typeString = patchStr.substring(patchStr.indexOf('[') + 1, patchStr.indexOf(']'));
        IRule.Type type;
        try {
            type = IRule.Type.valueOf(typeString);
        } catch (EnumConstantNotPresentException e) {
            Main.out.println("Unknown rule №" + num + ": " + typeString);
            return null;
        }

        switch (type) {
            case MATCH_ASSIGN:
                rule = assignRule(patchStr);
                break;
            case ADD_FILES:
                rule = addRule(patchStr);
                break;
            case MATCH_REPLACE:
                rule = replaceRule(patchStr);
                break;
            case REMOVE_FILES:
                rule = removeFilesRule(patchStr);
                break;
            case DUMMY:
                rule = dummyRule(patchStr);
                break;
            case EXECUTE_DEX:
                rule = dexRule(patchStr);
                break;
            case GOTO:
                rule = gotoRule(patchStr);
                break;
            case MATCH_GOTO:
                rule = matchGotoRule(patchStr);
                break;
            case REMOVE_CODE:
                rule = removeCodeRule(patchStr);
                break;
            default:
                Main.out.println("Parse error. Contact devs");
                return null;
        }
        if (rule.integrityCheckPassed())
            return rule;
        else {
            Main.out.println("Error parsing rule №" + num);
            return null;
        }
    }

    private Add addRule(String patchStr) {
        Add rule = new Add();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.target = matchSingleLine(patchStr, TARGET);
        rule.source = matchSingleLine(patchStr, SOURCE);
        rule.extract = parseBoolean(patchStr, EXTRACT);
        return rule;
    }

    private Assign assignRule(String patchStr) {
        Assign rule = new Assign();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.target = matchSingleLine(patchStr, TARGET);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.assignments = matchMultiLines(patchStr, ASSIGNMENT, ResultFormat.SPLIT);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.isSmali = rule.target.endsWith("smali");
        rule.isXml = rule.target.endsWith("xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        return rule;
    }

    private Dex dexRule(String patchStr) {
        Dex rule = new Dex();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.script = matchSingleLine(patchStr, SCRIPT);
        rule.mainClass = matchSingleLine(patchStr, MAIN_CLASS);
        rule.entrance = matchSingleLine(patchStr, ENTRANCE);
        rule.param = matchSingleLine(patchStr, PARAM);
        rule.isSmali = parseBoolean(patchStr, SMALI_NEEDED);
        return rule;
    }

    private Dummy dummyRule(String patchStr) {
        Dummy rule = new Dummy();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        return rule;
    }

    private Goto gotoRule(String patchStr) {
        Goto rule = new Goto();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.goTo = matchSingleLine(patchStr, GOTO);
        return rule;
    }

    private MatchGoto matchGotoRule(String patchStr) {
        MatchGoto rule = new MatchGoto();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.target = matchSingleLine(patchStr, TARGET);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.goTo = matchSingleLine(patchStr, GOTO);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.isSmali = rule.target.endsWith("smali");
        rule.isXml = rule.target.endsWith("xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        return rule;
    }

    private RemoveCode removeCodeRule(String patchStr) {
        RemoveCode rule = new RemoveCode();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        isSmaliNeeded = true;
        return rule;
    }

    private RemoveFiles removeFilesRule(String patchStr) {
        RemoveFiles rule = new RemoveFiles();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTargets(parseTargets(patchStr));
        return rule;
    }

    private Replace replaceRule(String patchStr) {
        Replace rule = new Replace();
        rule.name = matchSingleLine(patchStr, PAT_NAME);
        rule.target = matchSingleLine(patchStr, TARGET);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.replacement = matchSingleLine(patchStr, REPLACEMENT);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.isSmali = rule.target.endsWith("smali");
        rule.isXml = rule.target.endsWith("xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        if (rule.replacement != null && rule.isXml)
            rule.replacement = rule.replacement.replace("><", ">\n<");
        return rule;
    }

    private ArrayList<String> parseTargets(String patchStr) {
        ArrayList<String> strings = matchMultiLines(patchStr, TARGET, ResultFormat.SPLIT_TRIM);
        for (int i = 0; i < strings.size(); i++) {
            strings.set(i, strings.get(i).trim());
        }
        return strings;
    }

    private boolean parseBoolean(String patchStr, Pattern pattern) {
        String text = matchSingleLine(patchStr, pattern);
        return text != null && text.equalsIgnoreCase("true");
    }

    private String fixRegexMatch(String match) {   //add compatibility for windows line separator
        return match == null ? null : match.replace("\\n", "\\R");
    }

    private String fixRegexMatchXml(String match) {   //add compatibility with non-ApkEditor xml style
        return match == null ? null : match.replace("><", ">\\s*?<").replaceAll(" +", "\\s+?");
    }
}
