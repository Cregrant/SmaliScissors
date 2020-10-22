package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

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
            Rule rule; new IO().loadRules(patchesDir, zipName, patch);

            while ((rule = patch.getNextRule())!=null) {
                applySingleRule(currentProjectPath, rule);
            }

            if (Prefs.verbose_level == 0) out.println("Writing..");
            new IO().writeChangesInSmali();
            new IO().deleteAll(new File(patchesDir + File.separator + "temp"));
        }
        out.println("------------------\n" + currentProjectPath + " patched in " + (currentTimeMillis() - startTime) + "ms.");
        return "ok";
    }

    private void applySingleRule(String projectPath, Rule rule) {
        if (Prefs.verbose_level == 0)
            out.println(rule.toString());

        else if (Prefs.verbose_level == 1) {
            out.println("Type - " + rule.type);
            if (rule.target != null)
                out.println("Target - " + rule.target);
            else {
                out.println("Targets:\n    ");
                for (String target : rule.targetArr) out.println(target + "\n    ");
            }
        }
        ProcessRule processRule = new ProcessRule();

        try {
            switch (rule.type) {
                case "MATCH_ASSIGN":
                    processRule.assign(rule);
                    break;
                case "MATCH_REPLACE":
                    processRule.replace(rule);
                    break;
                case "ADD_FILES":
                    processRule.add(projectPath, rule);
                    break;
                case "REMOVE_FILES":
                    processRule.remove(projectPath, rule);
                    break;
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }
        out.println();
    }
}