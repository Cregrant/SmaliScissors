package com.github.cregrant.smaliscissors.rule;

import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.Rule;
import com.github.cregrant.smaliscissors.util.Regex;
import com.github.cregrant.smaliscissors.util.Regex.ResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class RuleParser {

    public static final Logger logger = LoggerFactory.getLogger(RuleParser.class);
    public static final String CRASH_REPORTERS_RULE = "[REMOVE_CODE]\nTARGET:\ncom/rollbar/android/\ncom/userexperior/\ncom/instabug/\ncom/bugsnag/\ncom/bugfender/sdk/\ncom/microsoft/appcenter/crashes/\ncom/bugsee/library/Bugsee/\ncom/crashlytics/\ncom/google/firebase/crashlytics/\ncom/google/firebase/crash/\ncom/bugsense/trace/\ncom/applause/android/\ncom/mindscapehq/android/raygun4android/\nio/fabric/\nio/invertase/firebase/crashlytics/\nnet/hockeyapp/\n[/REMOVE_CODE]";
    public static final Pattern RULE = Pattern.compile("(\\[.+?]\\R(?:NAME|GOTO|SOURCE|SCRIPT|TARGET|ACTION):[\\s\\S]*?\\[/.+?])");
    public static final Pattern SOURCE = Pattern.compile("SOURCE:\\s+(.+?)\\s*\\R");
    public static final Pattern EXTRACT = Pattern.compile("EXTRACT:\\s+(.+?)\\s*\\R");
    public static final Pattern ASSIGNMENT = Pattern.compile("ASSIGN:\\R((?:.+?=.+\\R)*)");
    public static final Pattern REPLACEMENT = Pattern.compile("REPLACE:\\R(\\R?|[\\S\\s]*?\\R)(?:[A-Z]+?:\\R|\\[)");  //issue: match extra \n at the end
    public static final Pattern TARGET = Pattern.compile("TARGET:\\s+([\\s\\S]*?)\\R(?:[A-Z]+?:\\R|\\[)");
    public static final Pattern MATCH = Pattern.compile("MATCH:\\R(.+)");
    public static final Pattern PAT_NAME = Pattern.compile("NAME:\\s+(.+?)\\s*\\R");
    public static final Pattern REGEX = Pattern.compile("REGEX:\\s+(.+?)\\s*\\R");
    public static final Pattern SCRIPT = Pattern.compile("SCRIPT:\\s+(.+?)\\s*\\R");
    public static final Pattern SMALI_NEEDED = Pattern.compile("SMALI_NEEDED:\\s+(.+?)\\s*\\R");
    public static final Pattern MAIN_CLASS = Pattern.compile("MAIN_CLASS:\\s+(.+?)\\s*\\R");
    public static final Pattern ENTRANCE = Pattern.compile("ENTRANCE:\\s+(.+?)\\s*\\R");
    public static final Pattern PARAM = Pattern.compile("PARAM:\\s+([\\S\\s]*?)\\R(?:[A-Z]+?:\\R|\\[)");
    public static final Pattern GOTO = Pattern.compile("GOTO:\\s+(.+?)\\s*\\R");
    public static final Pattern ACTION = Pattern.compile("ACTION:\\s+(.+?)\\s*\\R");
    private final ArrayList<Rule> rules;
    private boolean isSmaliNeeded;
    private boolean isXmlNeeded;

    public RuleParser(String patch) {
        ArrayList<String> rawRules = Regex.matchMultiLines(patch, RULE, ResultFormat.FULL);
        rules = new ArrayList<>(rawRules.size());

        for (int i = 0; i < rawRules.size(); i++) {
            String ruleString = rawRules.get(i);
            Rule rule = parseSingleRule(ruleString, i);
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
                RemoveCode removeCode = new RemoveCode(CRASH_REPORTERS_RULE);
                removeCode.setInternal();
                rules.add(removeCode);
                break;
            }
        }
    }

    public static boolean parseBoolean(String patchStr, Pattern pattern) {
        String text = matchSingleLine(patchStr, pattern);
        return text != null && text.equalsIgnoreCase("true");
    }

    public static String fixRegexMatch(String match) {   //add compatibility for a windows line separator
        if (match == null) {
            return null;
        }
        return match
                .replace("\\r\\n", "\\R")
                .replace("\\n", "\\R")
                .replace("registers", "(?:registers|locals)");
    }

    public static String fixRegexMatchXml(String match) {   //add compatibility with non-ApkEditor xml style
        if (match == null) {
            return null;
        }
        return match
                .replace("\\r\\n", "\\R")
                .replace("\\n", "\\R")
                .replace("><", ">\\s*?<")
                .replace("\" />", "\" ?/>")
                .replaceAll(" +|\t+", "(?:\\\\s+?)");
    }

    public static String fixRegexReplacement(String replacement) {   //force locals to fix some old patches compatibility
        if (replacement == null) {
            return null;
        }
        return replacement
                .replace(".registers", ".locals");
    }

    Rule parseSingleRule(String ruleString, int num) {
        try {
            return Rule.parseRule(ruleString);
        } catch (Exception e) {
            logger.error("Unable to parse rule " + num + ":\n------------\n" + ruleString + "\n--------------\n" + e.getMessage());
            throw new InputMismatchException("Please fix the rule and try again.");
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

}
