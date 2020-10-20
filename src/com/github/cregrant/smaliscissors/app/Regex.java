package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    ArrayList<String> match(Pattern readyPattern, String content, String mode) {
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
                    case "":
                        matchedArr.addAll(Arrays.asList(textMatched.split("\\R")));
                        break;
                }
            }
        }
        return matchedArr;
    }

    String getEndOfPath(File path){
        return path.toString().replaceAll(".+[^a-z0-9 _]","");
    }
}