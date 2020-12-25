package com.github.cregrant.smaliscissors.engine;

import java.util.ArrayList;

import static java.lang.System.currentTimeMillis;

class Executor {
    String executePatches(ArrayList<String> zipArr) {
        long startTime = currentTimeMillis();
        for (String zipFile : zipArr) {
            if (zipFile.equals("cancel"))
                return "cancel";
            String zipName = Regex.getEndOfPath(zipFile);
            Prefs.zipName = zipName;
            Main.out.println("\nPatch - " + zipName);

            Patch patch = IO.loadRules(zipFile);
            boolean scanXml = patch.xmlNeeded && ProcessRule.xmlList.isEmpty();
            boolean scanSmali = patch.smaliNeeded && ProcessRule.smaliList.isEmpty();
            IO.loadProjectFiles(scanXml, scanSmali);

            Rule rule;
            while ((rule = patch.getNextRule())!=null) {
                preProcessRule(rule, patch);
            }

            if (Prefs.verbose_level == 0) Main.out.println("Writing..");
            IO.writeChanges();
            IO.deleteAll(Prefs.tempDir);
        }
        Main.out.println("------------------\n" + Regex.getEndOfPath(Prefs.projectPath) + " patched in " + (currentTimeMillis() - startTime) + "ms.");
        return "ok";
    }

    private void preProcessRule(Rule rule, Patch patch) {
        if (Prefs.verbose_level == 0)
            Main.out.println(rule.toString());

        else if (Prefs.verbose_level == 1) {
            Main.out.println("Type - " + rule.type);
            if (!rule.type.equals("EXECUTE_DEX") && !rule.type.equals("DUMMY") ) {
                if (rule.target != null)
                    Main.out.println("Target - " + rule.target);
                else {
                    Main.out.println("Targets:");
                    if (rule.targetArr.size() < 100)
                        for (String target : rule.targetArr) Main.out.println("    " + target);
                    else Main.out.println("    " + rule.targetArr.size() + " items");
                }
            }
        }

        try {
            switch (rule.type) {
                case "MATCH_ASSIGN":
                    ProcessRule.assign(rule);
                    break;
                case "MATCH_REPLACE":
                    ProcessRule.matchReplace(rule);
                    break;
                case "ADD_FILES":
                    ProcessRule.add(rule);
                    break;
                case "REMOVE_FILES":
                    ProcessRule.remove(rule);
                    break;
                case "EXECUTE_DEX":
                    Main.out.println("Executing dex...");
                    ProcessRule.dex(rule);
                    break;
                case "GOTO":
                    patch.setRuleName(rule.goTo);
                    break;
                case "MATCH_GOTO":
                    ProcessRule.matchGoto(rule, patch);
                    break;
            }
            Main.out.println("");
        } catch (Exception e) {
            Main.out.println("ERROR: " + e.toString());
        }
    }
}