package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.Regex.MatchType;
import com.github.cregrant.smaliscissors.structures.rules.*;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.Regex.matchMultiLines;
import static com.github.cregrant.smaliscissors.Regex.matchSingleLine;

class RuleParser {
    private static final Pattern RULE = Pattern.compile("(\\[.+?]\\R(?:NAME|GOTO|SOURCE|SCRIPT|TARGET):[\\s\\S]*?\\[/.+?])");
    private static final Pattern SOURCE = Pattern.compile("SOURCE:\\R(.+)");
    private static final Pattern EXTRACT = Pattern.compile("EXTRACT:\\R(.+)");
    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern ASSIGNMENT = Pattern.compile("\\R(.+?=\\$\\{GROUP\\d{1,2}\\})");
    private static final Pattern REPLACEMENT = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
    private static final Pattern TARGET = Pattern.compile("TARGET:\\R\\s*?([\\s\\S]*?)\\R(?:(?:MATCH|EXTRACT|SOURCE):|\\[/)");
    private static final Pattern MATCH = Pattern.compile("MATCH:\\R(.+)");
    private static final Pattern PAT_NAME = Pattern.compile("NAME:\\R(.+)");
    private static final Pattern REGEX = Pattern.compile("REGEX:\\R(.+)");
    private static final Pattern SCRIPT = Pattern.compile("SCRIPT:\\R(.+)");
    private static final Pattern SMALI_NEEDED = Pattern.compile("SMALI_NEEDED:\\R(.+)");
    private static final Pattern MAIN_CLASS = Pattern.compile("MAIN_CLASS:\\R(.+)");
    private static final Pattern ENTRANCE = Pattern.compile("ENTRANCE:\\R(.+)");
    private static final Pattern PARAM = Pattern.compile("PARAM:\\R(.+)");
    private static final Pattern GOTO = Pattern.compile("GOTO:\\R(.+)");
    private ArrayList<IRule> rules = new ArrayList<>();
    private boolean isSmaliNeeded;
    private boolean isXmlNeeded;

    public RuleParser(String rawString) {
        ArrayList<String> tempArr = Regex.matchMultiLines(rawString, RULE, Regex.MatchType.FULL);
        ArrayList<IRule> rulesArr = new ArrayList<>(tempArr.size());

        for (int i = 0; i < tempArr.size(); i++) {
            String raw = tempArr.get(i);
            rulesArr.add(parseRule(raw, i));
        }

        if (Prefs.optimizeRules) {
            for (int i = 0; i < rulesArr.size(); i++) {     //merge similar MATCH_REPLACE rules
                IRule rule = rulesArr.get(i);
                while (Prefs.optimizeRules) {
                    int next = i + 1;
                    if (next < rulesArr.size()) {
                        IRule nextRule = rulesArr.get(next);
                        if (rule.canBeMerged(nextRule)) {
                            Replace replace = ((Replace) rule);
                            if (replace.mergedRules == null)
                                replace.mergedRules = new ArrayList<>(5);
                            replace.mergedRules.add(nextRule);
                            i++;
                        } else
                            break;
                    } else
                        break;
                }
                rules.add(rule);
            }
            /*if (rawRulesArr.size()!=patch.getRulesCount())
                Main.out.println(rawRulesArr.size() + " rules " + "shrunk to " + patch.getRulesCount() + ".\n");
            else
                Main.out.println(rawRulesArr.size() + " rules found.\n");*/
        } else
            rules = rulesArr;
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
            Main.out.println("Error parsing rule №" + num + ": " + typeString);
            return null;
        }

        //noinspection EnhancedSwitchMigration
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

    private Replace replaceRule(String patchStr) {
        Replace rule = new Replace();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.replacement = matchSingleLine(patchStr, REPLACEMENT);
        rule.isSmali = ends(rule.targets, "smali");
        rule.isXml = ends(rule.targets, "xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        if (rule.replacement != null && rule.isXml)
            rule.replacement = rule.replacement.replace("><", ">\n<");
        return rule;
    }

    private Assign assignRule(String patchStr) {
        Assign rule = new Assign();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.assignments = matchMultiLines(patchStr, ASSIGNMENT, MatchType.SPLIT);
        rule.isSmali = ends(rule.targets, "smali");
        rule.isXml = ends(rule.targets, "xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        return rule;
    }

    private Add addRule(String patchStr) {
        Add rule = new Add();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        rule.source = parseString(patchStr, SOURCE);
        rule.extract = parseBoolean(patchStr, EXTRACT);
        return rule;
    }

    private RemoveCode removeCodeRule(String patchStr) {
        RemoveCode rule = new RemoveCode();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        isSmaliNeeded = true;
        return rule;
    }

    private RemoveFiles removeFilesRule(String patchStr) {
        RemoveFiles rule = new RemoveFiles();
        rule.setName(parseString(patchStr, PAT_NAME));
        rule.setTargets(parseTargets(patchStr));
        return rule;
    }

    private Dummy dummyRule(String patchStr) {
        Dummy rule = new Dummy();
        rule.name = parseString(patchStr, PAT_NAME);
        return rule;
    }

    private Execute dexRule(String patchStr) {
        Execute rule = new Execute();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.script = parseString(patchStr, SCRIPT);
        rule.isSmali = parseBoolean(patchStr, SMALI_NEEDED);
        rule.mainClass = parseString(patchStr, MAIN_CLASS);
        rule.entrance = parseString(patchStr, ENTRANCE);
        rule.param = parseString(patchStr, PARAM);
        return rule;
    }

    private Goto gotoRule(String patchStr) {
        Goto rule = new Goto();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.goTo = parseString(patchStr, GOTO);
        return rule;
    }

    private MatchGoto matchGotoRule(String patchStr) {
        MatchGoto rule = new MatchGoto();
        rule.name = parseString(patchStr, PAT_NAME);
        rule.targets = parseTargets(patchStr);
        rule.isRegex = parseBoolean(patchStr, REGEX);
        rule.match = matchSingleLine(patchStr, MATCH);
        rule.goTo = parseString(patchStr, GOTO);
        rule.isSmali = ends(rule.targets, "smali");
        rule.isXml = ends(rule.targets, "xml");

        if (rule.isRegex)
            rule.match = rule.isXml ? fixRegexMatchXml(rule.match) : fixRegexMatch(rule.match);
        return rule;
    }

    private ArrayList<String> parseTargets(String patchStr) {
        ArrayList<String> targets = matchMultiLines(patchStr, TARGET, MatchType.SPLIT_PATH);
        if (!isSmaliNeeded)
            isSmaliNeeded = ends(targets, "smali");
        if (!isXmlNeeded)
            isXmlNeeded = ends(targets, "xml");
        return targets;
    }

    private boolean ends(ArrayList<String> strings, String end) {
        for (String s : strings) {
            if (s.endsWith(end))
                return true;
        }
        return false;
    }

    private String parseString(String patchStr, Pattern pattern) {       //removes some whitespace
        String text = matchSingleLine(patchStr, pattern);
        if (text == null)
            return null;
        else
            return text.trim();
    }

    private boolean parseBoolean(String patchStr, Pattern pattern) {
        String text = matchSingleLine(patchStr, pattern);
        return text != null && text.trim().equalsIgnoreCase("true");
    }

    private String fixRegexMatch(String match) {   //add compatibility for windows line separator
        return match == null ? null : match.replace("\\n", "\\R");
    }

    private String fixRegexMatchXml(String match) {   //add compatibility with non-ApkEditor xml style
        return match == null ? null : match.replace("><", ">\\s*?<").replaceAll(" +", "\\s+?");
    }
}
