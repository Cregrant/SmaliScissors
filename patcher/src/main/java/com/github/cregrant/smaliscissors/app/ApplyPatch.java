package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static java.lang.System.currentTimeMillis;

class ApplyPatch {

    String doPatch(ArrayList<String> zipArr) {
        new IO().checkIfScanned();
        if (zipArr.isEmpty()) {
            ArrayList<String> zipFilesArr = new ArrayList<>();
            for (File zip : Objects.requireNonNull(Prefs.patchesDir.listFiles())) {
                if (zip.toString().endsWith(".zip"))
                    zipFilesArr.add(zip.getName());
            }
            zipArr = new Select().select(zipFilesArr, "\nNow select patch:", "No patches detected");
        }

        long startTime = currentTimeMillis();
        for (String zipFile : zipArr) {
            if (zipFile.equals("cancel")) {
                return "cancel";
            }
            OutStream.println("\nApplyPatch - " + zipFile);
            Patch patch = new Patch();
            Rule rule; new IO().loadRules(Prefs.patchesDir + File.separator + zipFile, patch);

            while ((rule = patch.getNextRule())!=null) {
                preProcessRule(rule, patch);
            }

            if (Prefs.verbose_level == 0) OutStream.println("Writing..");
            new IO().writeChanges();
            new IO().deleteAll(Prefs.tempDir);
        }
        OutStream.println("------------------\n" + Prefs.projectPath + " patched in " + (currentTimeMillis() - startTime) + "ms.");
        return "ok";
    }

    private void preProcessRule(Rule rule, Patch patch) {
        if (Prefs.verbose_level == 0)
            OutStream.println(rule.toString());

        else if (Prefs.verbose_level == 1) {
            OutStream.println("Type - " + rule.type);
            if (rule.target != null)
                OutStream.println("Target - " + rule.target);
            else {
                OutStream.println("Targets:");
                for (String target : rule.targetArr) OutStream.println("\n    " + target);
            }
        }
        ProcessRule processRule = new ProcessRule();

        try {
            switch (rule.type) {
                case "MATCH_ASSIGN":
                    processRule.assign(rule);
                    break;
                case "MATCH_REPLACE":
                    processRule.matchReplace(rule);
                    break;
                case "ADD_FILES":
                    processRule.add(rule);
                    break;
                case "REMOVE_FILES":
                    processRule.remove(rule);
                    break;
                case "EXECUTE_DEX":
                    processRule.dex();
                    break;
                case "GOTO":
                    patch.setRuleName(rule.goTo);
                    break;
                case "MATCH_GOTO":
                    processRule.matchGoto(rule, patch);
                    break;
            }
            OutStream.println("");
        } catch (Exception e) {
            OutStream.println("ERROR:");
            OutStream.println(e.getMessage());
        }
    }
}