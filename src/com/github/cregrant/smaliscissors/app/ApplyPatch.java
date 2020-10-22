package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

class ApplyPatch {

    String doPatch(String currentProjectPath) {
        File patchesDir = new File(new CompatibilityData().getPatchesDir());
        ArrayList<String> zipFilesArr = new ArrayList<>();
        for (File zip : Objects.requireNonNull(patchesDir.listFiles())) {
            if (!zip.toString().endsWith(".zip")) continue;
            zipFilesArr.add(zip.getName());
        }
        new IO().checkIfScanned(currentProjectPath);

        ArrayList<String> zipArr = new Select().select(zipFilesArr, "\nNow select patch:");
        long startTime = currentTimeMillis();
        for (String zipName : zipArr) {
            if (zipName.equals("cancel")) {
                return "cancel";
            }
            out.println("\nApplyPatch - " + zipName);
            Patch patch = new Patch();
            Rule rule;
            new IO().loadRules(patchesDir, zipName, patch);
            while ((rule = patch.getNextRule())!=null) {
                applySingleRule(currentProjectPath, rule);
            }

            for (String ruleOld : new IO().loadRules(patchesDir, zipName, patch)) {
                String result = new ApplyPatch().AllRulesPrepare(currentProjectPath, ruleOld);
                if (result.equals("error")) return "error";
            }
            if (Prefs.verbose_level == 0) out.println("Writing..");
            new IO().writeChangesInSmali();
            new IO().deleteAll(new File(patchesDir + File.separator + "temp"));
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
        String ruleType = new Regex().matchSingleLine(patDetect, rule);
        ArrayList<String> targetArr = new Regex().matchMultiLines(patTarget, rule, "target");
        for (String ruleTarget : targetArr) {
            if (applySingleRule(projectPath, rule, patMatch, ruleTarget, ruleType).equals("error")) return "error";
        }
        return "ok";
    }

    private String applySingleRule(String projectPath, Rule rule) {
        if (Prefs.verbose_level == 0)
            out.println(rule.toString());

        else if (Prefs.verbose_level == 1) {
            out.println("Type - " + rule.type);
            if (rule.target != null)
                out.println("Target:\n    " + rule.target);
            else {
                out.println("Targets:\n    ");
                for (String target : rule.targetArr) out.println(target + "\n    ");
            }
        }

/*        switch (rule.type) {
            case "MATCH_ASSIGN":
                new ProcessRule().assign(rule);
                break;
            case "MATCH_REPLACE":
                if (new ProcessRule().replace(rule).equals("error"))
                    return "error";
                break;
            case "ADD_FILES":
                new ProcessRule().add(projectPath, rule);
                break;
            case "REMOVE_FILES":
                new ProcessRule().remove(projectPath);
                break;
        }*/
        out.println();
        return "";
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
                new ProcessRule().assign(rule, patMatch, patTargetRegex);
                break;
            case "MATCH_REPLACE":
                if (new ProcessRule().replace(rule, patMatch, patTargetRegex).equals("error"))
                    return "error";
                break;
            case "ADD_FILES":
                new ProcessRule().add(projectPath, rule, ruleTarget);
                break;
            case "REMOVE_FILES":
                new ProcessRule().remove(projectPath, ruleTarget);
                break;
        }
        out.println();
        return "";
    }

}