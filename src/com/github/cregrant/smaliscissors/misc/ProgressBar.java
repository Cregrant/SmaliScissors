package com.github.cregrant.smaliscissors.misc;

public class ProgressBar {
    static int curValue = 0;

    static void watch() {
        new Thread(() -> {
            while (curValue < 100) {
                ProgressBar.display(curValue);
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void display(int per) {
        String format = "Processing: " + per + "% \r";
        System.out.print(format);
    }
}