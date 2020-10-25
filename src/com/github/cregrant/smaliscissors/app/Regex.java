package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    ArrayList<String> matchMultiLines(Pattern readyPattern, String content, String mode) {
        Matcher matcher = readyPattern.matcher(content);
        ArrayList<String> matchedArr = new ArrayList<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                String textMatched = matcher.group(i);
                switch (mode) {
                    case "rules":
                    case "replace":
                        matchedArr.add(textMatched);
                        break;
                    case "target":
                    case "assign":
                    case "":
                        for (String str : textMatched.split("\\R"))
                            matchedArr.add(str.replace("smali*/*.smali", ".*smali").replaceAll("([^.])\\*", "$1.*"));
                        break;
                }
            }
        }
        return matchedArr;
    }

    String matchSingleLine(Pattern readyPattern, String content) {
        Matcher matcher = readyPattern.matcher(content);
        if (matcher.find()) {
            if (matcher.groupCount()==0)
                return matcher.group(0).replace("smali*/*.smali", ".*smali");
            return matcher.group(1).replace("smali*/*.smali", ".*smali");
        }
        return null;
    }

    String getEndOfPath(String path) {
        return path.replaceAll(".+\\\\", "");
    }
}