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
    String script;
    String smali_needed;
    String main_class;
    String entrance;
    String param;
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
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:\n    ").append(type);

        if (target != null) {
            sb.append("Target:\n    ");
            out.println(target);
        }
        else {
            out.println("Targets:\n    ");
            for (String target : targetArr) sb.append(target).append("\n    ");
        }
        switch (type) {
            case "MATCH_ASSIGN":
                sb
                        .append("Match:\n    ").append(match)
                        .append("IsRegex:\n    ").append(isRegex)
                        .append("Assignments:\n    ");
                for (String ass : assignments) sb.append(ass).append("\n    ");

                break;
            case "ADD_FILES":
                sb
                        .append("Source:\n    ").append(source)
                        .append("Extract:\n    ").append(extract);
                break;
            case "MATCH_REPLACE":
                sb
                        .append("Match:\n    ").append(match)
                        .append("IsRegex:\n    ").append(isRegex)
                        .append("Replacement:\n    ").append(replacement);
                break;
        }
        return sb.toString();
    }
}
