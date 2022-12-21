package com.github.cregrant.smaliscissors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Args {

    private final Options options = new Options();
    private final ArrayList<String> projectsList = new ArrayList<>();
    private final ArrayList<String> patchesList = new ArrayList<>();
    private final ArrayList<String> removeList = new ArrayList<>();
    Option project;
    Option patch;
    Option remove;
    Option log;
    Option logFile;

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

        String logDescription = "Level of verbosity: DEBUG, INFO or ERROR (default: INFO)";
        log = new Option("l", "log", true, logDescription);
        options.addOption(log);

        String logFolderDescription = "Path to a file for a debug log";
        logFile = new Option("f", "log-file", true, logFolderDescription);
        options.addOption(logFile);
    }

    public void validate(CommandLine cmd) {
        StringBuilder errors = new StringBuilder();

        for (String projectPath : cmd.getOptionValues(project)) {    //project values is required and not null
            projectsList.add(projectPath.replace('\\', '/'));
            File projectFolder = new File(projectPath);
            if (!projectFolder.exists()) {
                errors.append("The project folder ").append(projectPath).append(" does not exist.\n");
            } else if (projectFolder.isFile()) {
                errors.append("The project ").append(projectPath).append(" is not a folder.\n");
            }
        }

        if (cmd.hasOption(patch)) {
            for (String patchPath : cmd.getOptionValues(patch)) {
                patchesList.add(patchPath.replace('\\', '/'));
                File patchFile = new File(patchPath);
                if (!patchFile.exists()) {
                    errors.append("The patch file ").append(patchPath).append(" does not exist.\n");
                } else if (!patchPath.endsWith(".zip")) {
                    errors.append("The patch ").append(patchPath).append(" is not a zip file.\n");
                }
            }
        }

        if (cmd.hasOption(remove)) {
            for (String removePath : cmd.getOptionValues(remove)) {
                removeList.add(removePath.replace('\\', '/'));
            }
        }

        if (patchesList.isEmpty() && removeList.isEmpty()) {
            errors.append("Both -patch and -remove parameters are empty. Use one of them to run some actions\n");
        }

        if (cmd.hasOption(log)) {
            String logValue = cmd.getOptionValue(log);
            try {
                Prefs.Log.valueOf(logValue);
            } catch (Exception e) {
                errors.append("The ").append(log.getLongOpt()).append(" parameter value \"")
                        .append(logValue).append("\" is invalid\n");
            }
        }

        if (errors.length() > 0) {
            Main.out.println("Execution aborted, please fix some errors:\n" + errors);
            System.exit(1);
        }
    }

    public List<String> getProjectsList() {
        return projectsList;
    }

    public List<String> getPatchesList() {
        return patchesList;
    }

    public List<String> getRemoveList() {
        return removeList;
    }

    public Options getOptions() {
        return options;
    }
}
