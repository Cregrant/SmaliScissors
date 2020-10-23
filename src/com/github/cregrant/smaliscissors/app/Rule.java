package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;

import static java.lang.System.out;

class Rule {
    int num;
    String type;
    String source;
    String match;
    String target;
    String replacement;
    String name;
    String script;
    String isSmaliNeeded;
    String mainClass;
    String entrance;
    String param;
    String goTo;
    boolean isRegex = false;
    boolean extract = false;
    ArrayList<String> targetArr;
    ArrayList<String> assignments;

    boolean checkRuleIntegrity() {
        switch (type) {
            case "MATCH_ASSIGN":
                if (target==null | match==null | assignments==null)
                    return false;
                break;
            case "ADD_FILES":
                if (target==null | source==null)
                    return false;
                break;
            case "MATCH_REPLACE":
                if (target==null | match==null | replacement==null)
                    return false;
                break;
            case "REMOVE_FILES":
                if (target==null)
                    return false;
                break;
            case "DUMMY":
                if (name==null)
                    return false;
                break;
            case "EXECUTE_DEX":
                if (script==null | isSmaliNeeded==null | mainClass==null | entrance==null | param==null)
                    return false;
                break;
            case "GOTO":
                if (goTo==null)
                    return false;
                break;
            case "MATCH_GOTO":
                if (target==null | match==null | goTo==null)
                    return false;
                break;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    ").append(type);

        if (target != null) {
            sb.append("Target:    ").append(target);
        }
        else {
            out.println("Targets:\n    ");
            for (String target : targetArr) sb.append(target).append("\n    ");
        }
        switch (type) {
            case "MATCH_ASSIGN":
                sb
                        .append("Match:    ").append(match)
                        .append("Regex:    ").append(isRegex)
                        .append("Assignments:");
                if (assignments.size()==1)
                    sb.append("    ").append(assignments.get(0));
                else {
                    sb.append("\n    ");
                    for (String ass : assignments) sb.append(ass).append("\n    ");
                }
                break;
            case "ADD_FILES":
                sb
                        .append("Source:    ").append(source)
                        .append("Extract:    ").append(extract);
                break;
            case "MATCH_REPLACE":
                sb
                        .append("Match:    ").append(match)
                        .append("Regex:    ").append(isRegex)
                        .append("Replacement:    ").append(replacement);
            case "DUMMY":
                sb.append(name);
                break;
            case "EXECUTE_DEX":
                sb
                        .append("Script:    ").append(script)
                        .append("Smali needed:    ").append(isSmaliNeeded)
                        .append("Main class:    ").append(mainClass)
                        .append("Entrance:    ").append(entrance)
                        .append("Param:    ").append(param);
                break;
            case "GOTO":
                sb
                        .append("Goto:    ").append(goTo);
                break;
            case "MATCH_GOTO":
                sb
                        .append("Match:    ").append(match)
                        .append("Goto:    ").append(goTo);
                break;
        }
        return sb.toString();
    }
}
