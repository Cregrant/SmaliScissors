package com.github.cregrant.smaliscissors.util;

public class Misc {

    public static String stacktraceToString(Exception e) {
        StackTraceElement[] stack = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("\nUnexpected error occured:\n\n");
        int limit = Math.min(stack.length, 6);
        for (int i = 0; i < limit; i++) {
            sb.append(stack[i].toString()).append('\n');
        }
        return sb.toString();
    }
}
