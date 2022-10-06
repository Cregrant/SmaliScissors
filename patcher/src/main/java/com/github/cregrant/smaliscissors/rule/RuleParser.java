package com.github.cregrant.smaliscissors.rule;

import com.github.cregrant.smaliscissors.rule.types.*;
import com.github.cregrant.smaliscissors.util.Regex;
import com.github.cregrant.smaliscissors.util.Regex.ResultFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.util.Regex.matchMultiLines;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class RuleParser {
    private static final String CRASH_REPORTERS = "com/rollbar/android/\ncom/userexperior/\ncom/instabug/\ncom/bugsnag/\ncom/bugfender/sdk/\ncom/microsoft/appcenter/crashes/\ncom/bugsee/library/Bugsee/\ncom/crashlytics/\ncom/google/firebase/crashlytics/\ncom/google/firebase/crash/\ncom/bugsense/trace/\ncom/applause/android/\ncom/mindscapehq/android/raygun4android/\nio/fabric/\nio/invertase/firebase/crashlytics/\nnet/hockeyapp/";
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
    private static final Pattern PARAM = Pattern.compile("PARAM:\\s+([\\S\\s]*?)\\R\\[/EXECUTE_DEX]");
    private static final Pattern GOTO = Pattern.compile("GOTO:\\s+(.+?)\\s*\\R");
    private final ArrayList<Rule> rules;
    private boolean isSmaliNeeded;
    private boolean isXmlNeeded;

    public RuleParser(String rawString) {
        ArrayList<String> tempArr = Regex.matchMultiLines(rawString, RULE, ResultFormat.FULL);
        rules = new ArrayList<>(tempArr.size());

        for (int i = 0; i < tempArr.size(); i++) {
            String raw = tempArr.get(i);
            Rule rule = parseRule(raw, i);
            if (!isSmaliNeeded) {
                isSmaliNeeded = rule.smaliNeeded();
            }
            if (!isXmlNeeded) {
                isXmlNeeded = rule.xmlNeeded();
            }
            rules.add(rule);
        }

        for (Rule rule : rules) {              //also delete crash reporters if [REMOVE_CODE] applied
            if (rule instanceof RemoveCode) {
                RemoveCode removeCode = new RemoveCode();
                removeCode.setTargets(new ArrayList<>(Arrays.asList(CRASH_REPORTERS.split("\n"))));  //keep new ArrayList<>
                removeCode.setInternal();
                rules.add(removeCode);
                break;
            }
        }
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public boolean isSmaliNeeded() {
        return isSmaliNeeded;
    }

    public boolean isXmlNeeded() {
        return isXmlNeeded;
    }

    Rule parseRule(String patchStr, int num) {
        Rule rule;
        String typeString = patchStr.substring(patchStr.indexOf('[') + 1, patchStr.indexOf(']'));
        Rule.Type type;
        try {
            type = Rule.Type.valueOf(typeString);
        } catch (EnumConstantNotPresentException e) {
            throw new InputMismatchException("Unknown rule №" + num + ": " + typeString);
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
                rule = new Dummy();     //will trigger parse error
        }
        if (rule.isValid()) {
            return rule;
        } else {
            throw new InputMismatchException("Error parsing rule №" + num + ":\n------------\n" + patchStr + "\n--------------");
        }
    }

    private Add addRule(String patchStr) {
        Add rule = new Add();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTarget(matchSingleLine(patchStr, TARGET));
        rule.setSource(matchSingleLine(patchStr, SOURCE));
        rule.setExtract(parseBoolean(patchStr, EXTRACT));
        return rule;
    }

    private Assign assignRule(String patchStr) {
        Assign rule = new Assign();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTarget(matchSingleLine(patchStr, TARGET));
        rule.setMatch(matchSingleLine(patchStr, MATCH));
        rule.setAssignments(matchMultiLines(patchStr, ASSIGNMENT, ResultFormat.SPLIT));
        rule.setRegex(parseBoolean(patchStr, REGEX));
        rule.setSmali(rule.getTarget().endsWith("smali"));
        rule.setXml(rule.getTarget().endsWith("xml"));

        if (rule.isRegex()) {
            rule.setMatch(rule.isXml() ? fixRegexMatchXml(rule.getMatch()) : fixRegexMatch(rule.getMatch()));
        }
        return rule;
    }

    private ExecuteDex dexRule(String patchStr) {
        ExecuteDex rule = new ExecuteDex();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setScript(matchSingleLine(patchStr, SCRIPT));
        rule.setSmali(parseBoolean(patchStr, SMALI_NEEDED));
        rule.setMainClass(matchSingleLine(patchStr, MAIN_CLASS));
        rule.setEntrance(matchSingleLine(patchStr, ENTRANCE));
        StringBuilder sb = new StringBuilder();
        for (String s : matchMultiLines(patchStr, PARAM, ResultFormat.SPLIT_TRIM)) {
            sb.append(s);
        }
        rule.setParam(sb.toString());
        return rule;
    }

    private Dummy dummyRule(String patchStr) {
        Dummy rule = new Dummy();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
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
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTarget(matchSingleLine(patchStr, TARGET));
        rule.setMatch(matchSingleLine(patchStr, MATCH));
        rule.setGoTo(matchSingleLine(patchStr, GOTO));
        rule.setRegex(parseBoolean(patchStr, REGEX));
        rule.setSmali(rule.getTarget().endsWith("smali"));
        rule.setXml(rule.getTarget().endsWith("xml"));

        if (rule.isRegex()) {
            rule.setMatch(rule.isXml() ? fixRegexMatchXml(rule.getMatch()) : fixRegexMatch(rule.getMatch()));
        }
        return rule;
    }

    private RemoveCode removeCodeRule(String patchStr) {
        RemoveCode rule = new RemoveCode();
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTargets(parseTargets(patchStr));
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
        rule.setName(matchSingleLine(patchStr, PAT_NAME));
        rule.setTarget(matchSingleLine(patchStr, TARGET));
        rule.setMatch(matchSingleLine(patchStr, MATCH));
        rule.setReplacement(matchSingleLine(patchStr, REPLACEMENT));
        rule.setRegex(parseBoolean(patchStr, REGEX));
        rule.setSmali(rule.getTarget().endsWith("smali"));
        rule.setXml(rule.getTarget().endsWith("xml"));

        if (rule.isRegex()) {
            rule.setMatch(rule.isXml() ? fixRegexMatchXml(rule.getMatch()) : fixRegexMatch(rule.getMatch()));
        }
        if (rule.getReplacement() != null && rule.isXml()) {
            rule.setReplacement(rule.getReplacement().replace("><", ">\n<"));
        }
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
        if (match == null) {
            return null;
        }
        return match
                .replace("><", ">\\s*?<")
                .replace("\" />", "\" ?/>")
                .replaceAll(" +", "\\\\s+?");

    }
}
