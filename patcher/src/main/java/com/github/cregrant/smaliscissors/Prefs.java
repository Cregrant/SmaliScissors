package com.github.cregrant.smaliscissors;

import java.util.Arrays;
import java.util.List;

public class Prefs {

    public enum Log {
        DEBUG(0),
        INFO(1),
        ERROR(2);

        private final int level;
        Log(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public static Log logLevel = Log.INFO;
    public static boolean allowCompression = true;
    public static boolean skipSmaliRootFolders = true;
    public static List<String> smaliFoldersToSkip = Arrays.asList("android", "androidx", "kotlin", "kotlinx");
}