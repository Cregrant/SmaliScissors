package com.github.cregrant.smaliscissors.console;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Args {

    private static final Logger logger = LoggerFactory.getLogger(Args.class);
    private final Options options = new Options();
    private final ArrayList<String> projects = new ArrayList<>();
    private final ArrayList<String> patches = new ArrayList<>();
    private final ArrayList<String> smaliPaths = new ArrayList<>();
    Option project;
    Option patch;
    Option remove;
    Option select;
    Option log;
    Option logFile;
    private boolean interactiveSelectMode;

    public Args() {
        String projectDescription = "Path to a decompiled project folder";
        project = new Option("i", "input", true, projectDescription);
        project.setRequired(true);
        options.addOption(project);

        String patchDescription = "Path to a patch in the common form of a .zip file";
        patch = new Option("p", "patch", true, patchDescription);
        options.addOption(patch);

        String removeDescription = "Smali path that should be removed from the project. " +
                "A quick alternative version for running the single REMOVE_CODE patch";
        remove = new Option("r", "remove", true, removeDescription);
        options.addOption(remove);

        String selectDescription = "Enables the interactive project and patch selection mode. " +
                "Note that now these paths should point to a root folder that contains projects/patches";
        select = new Option("s", "select", false, selectDescription);
        options.addOption(select);

        String logDescription = "Level of verbosity: DEBUG, INFO, WARN or ERROR (default: INFO)";
        log = new Option("l", "log", true, logDescription);
        options.addOption(log);

        String logFileDescription = "Path to save the application log as a file";
        logFile = new Option("f", "log-file", true, logFileDescription);
        options.addOption(logFile);
    }

    public static Args parseArgs(String[] args) {
        Args parsedArgs = new Args();

        try {
            CommandLine cmd = new DefaultParser().parse(parsedArgs.getOptions(), args);
            parsedArgs.validate(cmd);
            return parsedArgs;
        } catch (ParseException e) {
            logger.error(e.getMessage());
            new HelpFormatter().printHelp(" ", parsedArgs.getOptions());
            return null;
        }
    }

    public void validate(CommandLine cmd) throws ParseException {
        StringBuilder errors = new StringBuilder();

        if (cmd.hasOption(select)) {
            interactiveSelectMode = true;
        }

        if (!interactiveSelectMode) {
            for (String projectPath : cmd.getOptionValues(project)) {    //project values is required and not null
                projects.add(convertBackslashes(projectPath));

                File projectFolder = new File(projectPath);
                if (!projectFolder.exists()) {
                    errors.append("The project folder ").append(projectPath).append(" does not exist.\n");
                } else if (projectFolder.isFile()) {
                    errors.append("The project ").append(projectPath).append(" is not a folder.\n");
                }
            }
        } else {
            checkSingleRoot(cmd, errors, project);
            projects.add(cmd.getOptionValue(project));
        }

        if (cmd.hasOption(patch)) {
            if (!interactiveSelectMode) {
                for (String patchPath : cmd.getOptionValues(patch)) {
                    patches.add(convertBackslashes(patchPath));

                    File patchFile = new File(patchPath);
                    if (!patchFile.exists()) {
                        errors.append("The patch file ").append(patchPath).append(" does not exist.\n");
                    } else if (!patchPath.endsWith(".zip")) {
                        errors.append("The patch ").append(patchPath).append(" is not a zip file.\n");
                    }
                }
            } else {
                checkSingleRoot(cmd, errors, patch);
                patches.add(cmd.getOptionValue(patch));
            }
        }

        if (cmd.hasOption(remove)) {
            for (String removePath : cmd.getOptionValues(remove)) {
                smaliPaths.add(convertBackslashes(removePath));
            }
        }

        if (patches.isEmpty() && smaliPaths.isEmpty()) {
            errors.append("Both --patch and --remove parameters are empty. Use one of them to run some actions\n");
        }

        if (cmd.hasOption(log)) {
            String logValue = cmd.getOptionValue(log).toUpperCase();
            boolean valid = Arrays.asList("OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR").contains(logValue);
            if (!valid) {
                errors.append("The --log parameter value \"").append(logValue)
                        .append("\" is invalid\n");
            }
        }

        if (errors.length() > 0) {
            throw new ParseException("Execution aborted; please fix some errors:\n" + errors);
        }
    }

    private void checkSingleRoot(CommandLine cmd, StringBuilder errors, Option option) {
        String[] rootDir = cmd.getOptionValues(option);
        if (rootDir.length != 1) {
            errors.append("Multiple --").append(option.getLongOpt())
                    .append(" arguments is not allowed for a --").append(select.getLongOpt()).append(" mode");
        }
    }

    private String convertBackslashes(String s) {
        return s.replace('\\', '/');
    }

    public List<String> getProjects() {
        return projects;
    }

    public List<String> getPatches() {
        return patches;
    }

    public List<String> getSmaliPaths() {
        return smaliPaths;
    }

    public boolean isInteractiveSelectMode() {
        return interactiveSelectMode;
    }

    public Options getOptions() {
        return options;
    }
}
