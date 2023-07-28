package com.github.cregrant.smaliscissors.util;

import java.util.Collection;

public class Misc {

    public static String stacktraceToString(Exception e) {
        StackTraceElement[] stack = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("\nUnexpected error occured:\n");
        sb.append(e.getMessage()).append("\n\n");
        int limit = Math.min(stack.length, 6);
        for (int i = 0; i < limit; i++) {
            sb.append(stack[i].toString()).append('\n');
        }
        return sb.toString();
    }

    public static String trimToSize(String s, int size) {
        int end = size;
        int firstLineBreak = s.indexOf('\n');
        if (firstLineBreak > 0 && firstLineBreak < size) {
            end = firstLineBreak - 1;
        }

        if (s.length() <= size && end == size) {
            return s;
        }
        return s.substring(0, end) + " ...";
    }

    public static String trimToSize(Collection<String> strings, String prefix, int linesCount, int lineLength) {
        StringBuilder sb = new StringBuilder();
        int size = 0;
        for (String s : strings) {
            sb.append(prefix).append(trimToSize(s, lineLength)).append('\n');
            size++;
            if (size >= linesCount) {
                sb.append("  ... ").append(strings.size() - size - 1).append(" more lines\n");
                break;
            }
        }
        return sb.toString();
    }
}
