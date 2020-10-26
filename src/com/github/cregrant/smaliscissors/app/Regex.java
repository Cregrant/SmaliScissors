package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;
import java.util.Arrays;
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
                    case "assign":
                        matchedArr.addAll(Arrays.asList(textMatched.split("\\R")));
                        break;
                    case "target":
                    case "":
                        //todo move it to single match?
                        for (String str : textMatched.split("\\R")) {
                            if (Prefs.arch_device.equals("pc"))
                                str = str.replace("/", "\\\\");
                            matchedArr.add(globToRegex(str));
                        }
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
                return matcher.group(0);
            return matcher.group(1);
        }
        return null;
    }

    String getEndOfPath(String path) {
        int last = path.lastIndexOf('\\')+1;
        if (last == 0) return path;
        return path.substring(last);
    }

    String globToRegex(String line)
    {
        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (line.startsWith("*"))
        {
            line = line.substring(1);
            strLen--;
        }
        if (line.endsWith("*"))
        {
            line = line.substring(0, strLen-1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray())
        {
            switch (currentChar)
            {
                case '*':
                    if (escaping)
                        sb.append("\\*");
                    else
                        sb.append(".*");
                    escaping = false;
                    break;
                case '?':
                    if (escaping)
                        sb.append("\\?");
                    else
                        sb.append('.');
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(currentChar);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping)
                    {
                        sb.append("\\\\");
                        escaping = false;
                    }
                    else
                        escaping = true;
                    break;
                case '{':
                    if (escaping)
                    {
                        sb.append("\\{");
                    }
                    else
                    {
                        sb.append('(');
                        inCurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append(')');
                        inCurlies--;
                    }
                    else if (escaping)
                        sb.append("\\}");
                    else
                        sb.append("}");
                    escaping = false;
                    break;
                case ',':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append('|');
                    }
                    else if (escaping)
                        sb.append("\\,");
                    else
                        sb.append(",");
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
        }
        return sb.toString();
    }
}