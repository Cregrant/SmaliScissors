package com.github.cregrant.smaliscissors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ALL")
public class Flags {
    public static final Logger logger = LoggerFactory.getLogger(Flags.class);

    //tests
    public static boolean STOP_IF_TEST_FAILED = false;

    //debug flags
    public static boolean DEBUG_NO_MULTITHREADING = false;

    //remove code flags
    public static boolean SMALI_DEBUG_BENCHMARK = false;
    public static boolean SMALI_DEBUG_DO_NOT_WRITE = false || SMALI_DEBUG_BENCHMARK;
    public static boolean SMALI_PRESERVE_PARTIALLY_CLEANED_ARRAYS = false;
    public static boolean SMALI_ALLOW_METHOD_ARGUMENTS_CLEANUP = false;
    public static boolean SMALI_USE_CAST_FOR_STUB = false;      //true to use class casts that produce helpful ClassCastException

    static {
        if (SMALI_DEBUG_DO_NOT_WRITE || DEBUG_NO_MULTITHREADING) {
            for (int i = 0; i < 5; i++) {
                logger.warn("DEBUG FLAG ENABLED");
            }
        }
    }
}
