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
    private final Option projectOption;
    private final Option patchOption;
    private final Option removeOption;
    private final Option selectOption;
    private final Option logLevelOption;
    private final Option logFileOption;
    private boolean interactiveSelectMode;
    private File logFile;
    private String logLevel;

    public Args() {
        String projectDescription = "Path to a decompiled project folder";
        projectOption = new Option("i", "input", true, projectDescription);
        projectOption.setRequired(true);
        options.addOption(projectOption);

        String patchDescription = "Path to a patch in the common form of a .zip file";
        patchOption = new Option("p", "patch", true, patchDescription);
        options.addOption(patchOption);

        String removeDescription = "Smali path that should be removed from the project. " +
                "A quick alternative version for running the single REMOVE_CODE patch";
        removeOption = new Option("r", "remove", true, removeDescription);
        options.addOption(removeOption);

        String selectDescription = "Enables the interactive project and patch selection mode. " +
                "Note that now these paths should point to a root folder that contains projects/patches";
        selectOption = new Option("s", "select", false, selectDescription);
        options.addOption(selectOption);

        String logDescription = "Level of verbosity: DEBUG, INFO, WARN, ERROR or OFF (default: INFO)";
        logLevelOption = new Option("l", "log", true, logDescription);
        options.addOption(logLevelOption);

        String logFileDescription = "Path to save the application log as a file";
        logFileOption = new Option("w", "log-file", true, logFileDescription);
        options.addOption(logFileOption);
    }

    public static Args parseArgs(String[] args) {
        Args parsedArgs = new Args();

        try {
            CommandLine cmd = new DefaultParser().parse(parsedArgs.getOptions(), args);
            parsedArgs.parse(cmd);
            return parsedArgs;
        } catch (ParseException e) {
            logger.error(e.getMessage());
            new HelpFormatter().printHelp(" ", parsedArgs.getOptions());
            return null;
        }
    }

    private void parse(CommandLine cmd) throws ParseException {
        StringBuilder errors = new StringBuilder();

        if (cmd.hasOption(selectOption)) {
            interactiveSelectMode = true;
        }

        if (!interactiveSelectMode) {
            for (String projectPath : cmd.getOptionValues(projectOption)) {    //project values is required and not null
                projects.add(convertBackslashes(projectPath));

                File projectFolder = new File(projectPath);
                if (!projectFolder.exists()) {
                    errors.append("The project folder ").append(projectPath).append(" does not exist.\n");
                } else if (projectFolder.isFile()) {
                    errors.append("The project ").append(projectPath).append(" is not a folder.\n");
                }
            }
        } else {
            checkSingleRoot(cmd, errors, projectOption);
            projects.add(cmd.getOptionValue(projectOption));
        }
        if (cmd.hasOption(patchOption)) {
            if (!interactiveSelectMode) {
                for (String patchPath : cmd.getOptionValues(patchOption)) {
                    patches.add(convertBackslashes(patchPath));

                    File patchFile = new File(patchPath);
                    if (!patchFile.exists()) {
                        errors.append("The patch file ").append(patchPath).append(" does not exist.\n");
                    } else if (!patchPath.endsWith(".zip")) {
                        errors.append("The patch ").append(patchPath).append(" is not a zip file.\n");
                    }
                }
            } else {
                checkSingleRoot(cmd, errors, patchOption);
                patches.add(cmd.getOptionValue(patchOption));
            }
        }

        if (cmd.hasOption(removeOption)) {
            for (String removePath : cmd.getOptionValues(removeOption)) {
                smaliPaths.add(convertBackslashes(removePath));
            }
        }

        if (patches.isEmpty() && smaliPaths.isEmpty()) {
            errors.append("Both --patch and --remove parameters are empty. Use one of them to run some actions\n");
        }

        if (cmd.hasOption(logLevelOption)) {
            logLevel = cmd.getOptionValue(logLevelOption).toUpperCase();
            boolean isValid = Arrays.asList("OFF", "DEBUG", "INFO", "WARN", "ERROR").contains(logLevel);
            if (!isValid) {
                errors.append("The --log parameter value \"").append(logLevel)
                        .append("\" is invalid\n");
            }
        }

        if (cmd.hasOption(logFileOption)) {
            logFile = new File(convertBackslashes(cmd.getOptionValue(logFileOption)));
            File parentFolder = logFile.getParentFile();
            if (!parentFolder.exists() && !parentFolder.mkdirs()) {
                errors.append("The log file parent folder ").append(parentFolder).append(" could not be created.\n");
            } else if (!parentFolder.canWrite()) {
                errors.append("The log file ").append(logFile).append(" is not writable.\n");
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
                    .append(" arguments is not allowed for a --").append(selectOption.getLongOpt()).append(" mode");
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

    public String getLogLevelOption() {
        return logLevel;
    }

    public File getLogFile() {
        return logFile;
    }

    public boolean isInteractiveSelectMode() {
        return interactiveSelectMode;
    }

    public Options getOptions() {
        return options;
    }
}
