package com.github.cregrant.smaliscissors.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    public static ArrayList<String> matchMultiLines(CharSequence content, Pattern readyPattern, ResultFormat format) {
        Matcher matcher = readyPattern.matcher(content);
        ArrayList<String> matchedArr = new ArrayList<>();
        while (matcher.find()) {
            int size = matcher.groupCount();
            for (int i = 1; i <= size; ++i) {
                String textMatched = matcher.group(i);
                switch (format) {
                    case FULL:
                        matchedArr.add(textMatched);
                        break;
                    case SPLIT:
                        matchedArr.addAll(Arrays.asList(textMatched.split("\\R")));
                        break;
                    case SPLIT_TRIM:
                        for (String str : textMatched.split("\\R")) {
                            matchedArr.add(str.trim());
                        }
                        break;
                }
            }
        }
        return matchedArr;
    }

    public static String matchSingleLine(CharSequence content, Pattern readyPattern) {
        Matcher matcher = readyPattern.matcher(content);
        if (matcher.find()) {
            if (matcher.groupCount() == 0) {
                return matcher.group(0);
            }
            return matcher.group(1);
        }
        return null;
    }

    public static String getFilename(String path) {
        int last = path.lastIndexOf('/') + 1;
        if (last == 0) {
            last = path.lastIndexOf('\\') + 1;
        }
        return path.substring(last);
    }

    public static String globToRegex(String line) {
        line = line.trim();
        StringBuilder sb = new StringBuilder(line.length());
        boolean escaping = false;
        int inBraces = 0;
        char prevChar = 0;
        for (char currentChar : line.toCharArray()) {
            switch (currentChar) {
                case '*':
                    if (escaping) {
                        sb.append("\\*");
                    } else if (currentChar != prevChar) {
                        sb.append(".*");
                    }
                    escaping = false;
                    break;
                case '?':
                    if (escaping) {
                        sb.append("\\?");
                    } else {
                        sb.append('.');
                    }
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
                    if (escaping) {
                        sb.append("\\");
                        escaping = false;
                    } else {
                        escaping = true;
                    }
                    break;
                case '{':
                    if (escaping) {
                        sb.append("\\{");
                    } else {
                        sb.append('(');
                        inBraces++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inBraces > 0 && !escaping) {
                        sb.append(')');
                        inBraces--;
                    } else if (escaping) {
                        sb.append("\\}");
                    } else {
                        sb.append("}");
                    }
                    escaping = false;
                    break;
                case ',':
                    if (inBraces > 0 && !escaping) {
                        sb.append('|');
                    } else if (escaping) {
                        sb.append("\\,");
                    } else {
                        sb.append(",");
                    }
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
            prevChar = currentChar;
        }
        return sb.toString();
    }

    /**
     * Custom implementation of matcher.replaceAll()
     * because android skip ${GROUP10} and nextPart.
     */
    public static String replaceAll(String body, Pattern match, String replacement) {
        Matcher matcher = match.matcher(body);
        if (replacement.length() == 0) {
            StringBuffer sb = new StringBuffer(body.length());
            while (matcher.find()) {
                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        ArrayList<Integer> numArr = new ArrayList<>(5);
        int endPos = 0;
        int pos;
        while ((pos = replacement.indexOf("${GROUP", endPos)) != -1) {
            endPos = replacement.indexOf('}', pos + 7);
            String s = replacement.substring(pos + 7, endPos);
            numArr.add(Integer.decode(s));
        }

        StringBuilder newBodyBuilder = new StringBuilder(body);
        HashMap<Integer, Integer> offsetMap = new HashMap<>();

        try {
            while (matcher.find()) {
                StringBuilder replacementBuilder = new StringBuilder(replacement);
                String s = matcher.group(0);
                int start = matcher.start(0);
                if (start == -1) {
                    continue;
                }
                for (int i : numArr) {
                    String group = "${GROUP" + i + "}";
                    int index = replacementBuilder.indexOf(group);
                    replacementBuilder.replace(index, index + group.length(), matcher.group(i));
                }

                int realOffset = 0;
                if (!offsetMap.isEmpty())  //the previous replacement changes the real position
                {
                    for (Map.Entry<Integer, Integer> entry : offsetMap.entrySet()) {
                        if (entry.getKey() < start) {
                            realOffset += entry.getValue();
                        }
                    }
                }
                offsetMap.put(start, replacementBuilder.length() - s.length());
                newBodyBuilder.replace(start + realOffset, matcher.end(0) + realOffset, replacementBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(body.length() + " " + newBodyBuilder.toString().length());
        }
        return newBodyBuilder.toString();
    }

    public enum ResultFormat {
        FULL,
        SPLIT,
        SPLIT_TRIM,
    }
}