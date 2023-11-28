package com.github.cregrant.smaliscissors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Flags {
    public static final Logger logger = LoggerFactory.getLogger(Flags.class);

    //tests
    public static boolean STOP_IF_TEST_FAILED = false;

    //debug flags
    public static boolean DEBUG_NO_MULTITHREADING = false;

    //remove code flags
    public static boolean SMALI_DEBUG_BENCHMARK = false;
    public static boolean SMALI_DEBUG_NOT_WRITE = false;
    public static boolean SMALI_PRESERVE_PARTIALLY_CLEANED_ARRAYS = false;

    static {
        if (SMALI_DEBUG_BENCHMARK || SMALI_DEBUG_NOT_WRITE || DEBUG_NO_MULTITHREADING) {
            for (int i = 0; i < 5; i++) {
                logger.warn("DEBUG FLAG ENABLED");
            }
        }
    }
}
