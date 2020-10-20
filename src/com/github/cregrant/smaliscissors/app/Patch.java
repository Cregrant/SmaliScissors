package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

class Patch {

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        long startTime = currentTimeMillis();
        for (String zipName : zipArr) {
            if (zipName.equals("cancel")) {
                return "cancel";
            }
            out.println("\nPatch - " + zipName);
            for (String rule : new IO().loadRules(patchesDirFile, zipName)) {
                String result = new Patch().AllRulesPrepare(currentProjectPath, rule);
                if (result.equals("error")) return "error";
            }
            if (Prefs.verbose_level == 0) out.println("Writing..");
            new IO().writeChangesInSmali();
            new IO().deleteAll(tempFolder);
        }
        out.println("------------------\n" + currentProjectPath + " patched in " + (currentTimeMillis() - startTime) + "ms.");
        return "ok";
    }

    private String AllRulesPrepare(String projectPath, String rule) {
        Pattern patDetect = Pattern.compile("\\[(.+?)][\\S\\s]*?\\[/.+?]");
        Pattern patTarget = Pattern.compile("TARGET:\\R([\\s\\S]*?)(?:(?:MATCH|EXTRACT):|\\[/)");
        Pattern patMatch = Pattern.compile("MATCH:\\R(.+)");
        if (Prefs.rules_AEmode == 0) {
            out.println("TruePatcher mode on.");
        }
        String ruleType = new Regex().match(patDetect, rule, "").get(0);
        ArrayList<String> targetArr = new Regex().match(patTarget, rule, "target");
        for (int j = 0; j < targetArr.size(); ++j) {
            targetArr.set(j, targetArr.get(j).replace("smali*/*.smali", ".*smali"));
        }
        for (String ruleTarget : targetArr) {
            if (applySingleRule(projectPath, rule, patMatch, ruleTarget, ruleType).equals("error")) return "error";
        }
        return "ok";
    }

    private String applySingleRule(String projectPath, String rule, Pattern patMatch, String ruleTarget, String ruleType) {
        if (Prefs.verbose_level <= 1) {
            out.println("Rule - " + ruleType);
            out.println("Target - " + ruleTarget);
            if (Prefs.verbose_level == 0) {
                out.println(rule);
            }
        }
        if (ruleTarget.contains(".xml") && !ruleType.equals("ADD_FILES") && !ruleType.equals("REMOVE_FILES")) {
            out.println("Sorry, .xml patch is not supported.");
            ruleType = "";
        }
        String patTargetRegex = ruleTarget.replaceAll("([^.])\\*", "$1.*").replace('/', '\\');
        switch (ruleType) {
            case "MATCH_ASSIGN":
                new Rules().assign(rule, patMatch, patTargetRegex);
                break;
            case "MATCH_REPLACE":
                if (new Rules().replace(rule, patMatch, patTargetRegex).equals("error"))
                    return "error";
                break;
            case "ADD_FILES":
                new Rules().add(projectPath, rule, ruleTarget);
                break;
            case "REMOVE_FILES":
                new Rules().remove(projectPath, ruleTarget);
                break;
        }
        out.println();
        return "";
    }

}