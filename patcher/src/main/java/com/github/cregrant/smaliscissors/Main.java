package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static DexExecutor dex;

    public static void mainAsModule(String[] args, DexExecutor dexExecutor) {
        dex = dexExecutor;
        Args parsedArgs = new Args();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(parsedArgs.getOptions(), args);
            parsedArgs.validate(cmd);
            start(parsedArgs);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp(" ", parsedArgs.getOptions());
        } catch (Exception e) {
            logger.error("Execution interrupted:", e);
        }
    }

    public static void main(String[] args) {        //jar archive entry (broken?)
        mainAsModule(args, null);
    }

    private static void start(Args parsedArgs) {
        Worker worker = new Worker(parsedArgs.getProjectsList());
        worker.addPatches(parsedArgs.getPatchesList());
        if (!parsedArgs.getRemoveList().isEmpty()) {
            worker.addSingleRemoveCodeRules(parsedArgs.getRemoveList());
        }
        worker.run();
    }
}