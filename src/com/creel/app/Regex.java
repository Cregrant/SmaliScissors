package com.creel.app;

import com.creel.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Regex {
    static String currentProjectPathCached = "";

    Regex() {
    }

    private String AllRulesPrepare(String projectPath, String rule) {
        Pattern patDetect = Pattern.compile("\\[(.+?)][\\S\\s]*?\\[/.+?]");
        Pattern patTarget = Pattern.compile("TARGET:\\R([\\s\\S]*?)(?:MATCH:|\\[/)");
        Pattern patMatch = Pattern.compile("MATCH:\\R(.+)");
        if (Prefs.rules_mode == 0) {
            System.out.println("TruePatcher mode on.");
        }
        String ruleTarget = "multi-target";
        String ruleType = new Regex().match(patDetect, rule, "").get(0);
        ArrayList<String> targetArr = new Regex().match(patTarget, rule, "target");
        for (int j = 0; j < targetArr.size(); ++j) {
            targetArr.set(j, targetArr.get(j).replace("smali*/*.smali", ".*smali"));
        }
        if (targetArr.size() == 1) {
            ruleTarget = targetArr.get(0);
        }
        if (Prefs.verbose_level <= 1) {
            System.out.println("Rule - " + ruleType);
            System.out.println("Target - " + ruleTarget);
            if (Prefs.verbose_level == 0) {
                System.out.println(rule);
            }
        }
        if (ruleTarget.contains(".xml") && !ruleType.equals("ADD_FILES") && !ruleType.equals("REMOVE_FILES")) {
            System.out.println("Sorry, .xml patch is not supported.");
            ruleType = "";
        }
        String patTargetRegex = ruleTarget.replaceAll("([^.])\\*", "$1.*").replace('/', '\\');
        switch (ruleType) {
            case "MATCH_ASSIGN" -> new Rules().assign(rule, patMatch, patTargetRegex);
            case "MATCH_REPLACE" -> {
                if (new Rules().replace(rule, patMatch, patTargetRegex)) break;
                return "error";
            }
            case "ADD_FILES" -> new Rules().add(projectPath, rule, ruleTarget);
            case "REMOVE_FILES" -> new Rules().remove(projectPath, targetArr);
        }
        System.out.println();
        return "ok";
    }

    ArrayList<String> match(Pattern readyPattern, String content, String mode) {
        Matcher matcher = readyPattern.matcher(content);
        ArrayList<String> matchedArr = new ArrayList<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                String textMatched = matcher.group(i);
                switch (mode) {
                    case "replace" -> {
                        if (!textMatched.isEmpty()) {
                            matchedArr.add(textMatched);
                            continue;
                        }
                        if (!content.contains("REPLACE:\n[/")) continue;
                        matchedArr.add("");
                    }
                    case "target", "" -> matchedArr.addAll(Arrays.asList(textMatched.split("\\R")));
                }
            }
        }
        return matchedArr;
    }

    static String doPatch(String currentProjectPath) {
        File patchesDirFile = new File(new CompatibilityData().getPatchesDir());
        ArrayList<String> zipFilesArr = new ArrayList<>();
        for (File zip : Objects.requireNonNull(patchesDirFile.listFiles())) {
            if (!zip.toString().endsWith(".zip")) continue;
            zipFilesArr.add(zip.getName());
        }
        new IO().checkIfScanned(currentProjectPath);
        File tempFolder = new File(patchesDirFile + File.separator + "temp");
        if (!tempFolder.exists()) {
            tempFolder.mkdir();
        }
        ArrayList<String> zipArr = new Select().select(zipFilesArr, "\nNow select patch:");
        long startTime = System.currentTimeMillis();
        for (String zipName : zipArr) {
            if (zipName.equals("cancel")) {
                return "cancel";
            }
            System.out.println("\nPatch - " + zipName);
            for (String rule : new IO().loadRules(patchesDirFile, zipName)) {
                String result = new Regex().AllRulesPrepare(currentProjectPath, rule);
                if (!result.equals("error")) continue;
                System.out.println("Probably bug detected...");
                return "error";
            }
            new IO().writeChangesInSmali();
            new IO().deleteInDirectory(tempFolder);
        }
        System.out.println(currentProjectPath + " patched in " + (System.currentTimeMillis() - startTime) + "ms.");
        return "ok";
    }
}