package com.github.cregrant.smaliscissors.app;

public class OutStream {
    public static void println(Object x) {
        if (!Prefs.run_type.equals("module")) {
            System.out.println(x);
            return;
        }
        //add custom logger here
    }
}
