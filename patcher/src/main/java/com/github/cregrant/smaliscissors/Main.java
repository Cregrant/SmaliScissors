package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.SimpleOutStream;
import org.apache.commons.cli.*;



public class Main {
    public static SimpleOutStream out;
    public static DexExecutor dex;

    public static void mainAsModule(String[] args, SimpleOutStream logger, DexExecutor dexExecutor) {
        out = logger == null ? getDefaultOutStream() : logger;
        dex = dexExecutor;
        Args parsedArgs = new Args();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(parsedArgs.getOptions(), args);
            parsedArgs.validate(cmd);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp(" ", parsedArgs.getOptions());
            System.exit(1);
        }

        Worker worker = new Worker(parsedArgs.getProjectsList());
        worker.addPatches(parsedArgs.getPatchesList());
        if (!parsedArgs.getRemoveList().isEmpty()) {
            worker.addSingleRemoveCodeRules(parsedArgs.getRemoveList());
        }
        worker.run();
    }

    public static void main(String[] args) {
        mainAsModule(args, null, null);
    }

    private static SimpleOutStream getDefaultOutStream() {
        return new SimpleOutStream() {
            @Override
            public void println(Object x) {
                System.out.println(x);
            }
        };
    }
}