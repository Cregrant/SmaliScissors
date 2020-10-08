package com.creel.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import static java.lang.System.out;

class Rules {
    static ArrayList<Smali> smaliList = new ArrayList<>();
    static int patchedFilesNum = 0;
    static HashMap<String, String> assignMap = new HashMap<>();

    boolean replace(String rule, Pattern patMatch, String ruleTarget) {
        Pattern patReplacement = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
        String ruleTargetRegex = ruleTarget.replace("\\", "\\\\").replace(".smali", "\\.smali");
        String ruleMatch = new Regex().match(patMatch, rule, "").get(0);
        String ruleReplacement = new Regex().match(patReplacement, rule, "replace").get(0);
        if (!assignMap.isEmpty()) {
            Set<Map.Entry<String, String>> set = assignMap.entrySet();
            if (Prefs.verbose_level == 0) {
                System.out.println("Replacing numbers to text...\n" + set);
            }
            for (Map.Entry<String, String> entry : set) {
                String key = "${" + entry.getKey() + "}";
                if (!ruleMatch.contains(key)) continue;
                String value = entry.getValue();
                if (Prefs.verbose_level == 0) {
                    System.out.println(key + " -> " + value);
                }
                ruleMatch = ruleMatch.replace(key, value);
            }
        }
        CountDownLatch cdl = new CountDownLatch(Prefs.max_thread_num);
        try {
            AtomicInteger currentNum = new AtomicInteger(0);
            int thrNum = Prefs.max_thread_num;
            for (int cycle = 1; cycle <= thrNum; ++cycle) {
                int totalSmaliNum = smaliList.size() - 1;
                String finalRuleMatch = ruleMatch;
                String finalRuleReplacement = ruleReplacement.replaceAll("\\$\\{GROUP(\\d{1,2})}", "\\$$1");
                new Thread(() -> {
                    int num;
                    while ((num = currentNum.getAndIncrement()) <= totalSmaliNum) {
                        Smali patchedSmaliFile = new Rules().simpleReplace(smaliList.get(num), finalRuleMatch, ruleTarget, ruleTargetRegex, finalRuleReplacement);
                        if (!patchedSmaliFile.isModified()) continue;
                        ++patchedFilesNum;
                        System.out.println(patchedSmaliFile.getBody()+"\n"+smaliList.get(num).getBody());
                        System.out.println(patchedSmaliFile.getBody().equals(smaliList.get(num).getBody()));
                        if (Prefs.verbose_level == 0) {
                            System.out.println(patchedSmaliFile.getPath().replaceAll(".+/smali", "smali") + " patched.");
                        }
                        synchronized (patReplacement) {
                            smaliList.set(num, patchedSmaliFile);
                        }
                    }
                    cdl.countDown();
                }).start();
            }
            try {
                cdl.await();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        catch (PatternSyntaxException e) {
            e.printStackTrace();
            System.out.println("You can try again. It was (assign rule bug) or (your rule error). Sorry =(");
            return false;
        }
        if (Prefs.verbose_level <= 2) {
            System.out.println(patchedFilesNum + " files patched.");
        }
        patchedFilesNum = 0;
        return true;
    }

    Smali simpleReplace(Smali tmpSmali, String ruleMatch, String ruleTarget, String ruleTargetRegex, String ruleReplacement) {
        String smaliPath = tmpSmali.getPath();
        if (smaliPath.contains(ruleTarget) | smaliPath.matches(ruleTargetRegex)) {
            String smaliBody = tmpSmali.getBody();
            String smaliBodyNew = smaliBody.replaceAll(ruleMatch, ruleReplacement);
            if (!smaliBodyNew.equals(smaliBody)) {
                tmpSmali.setBody(smaliBodyNew);
                tmpSmali.setModified(true);
            }
        }
        return tmpSmali;
    }

    void assign(String rule, Pattern patMatch, String ruleTarget) {
        Pattern patAssign = Pattern.compile("\\n(.+?=\\$\\{GROUP\\d})");
        String ruleTargetRegex = ruleTarget.replace("\\", "\\\\").replace(".smali", "\\.smali");
        String ruleMatch = new Regex().match(patMatch, rule, "").get(0);
        if (Prefs.verbose_level == 0) {
            System.out.println("Match - " + ruleMatch);
        }
        ArrayList<String> assignArr = new ArrayList<>();
        for (Smali tmpSmali : smaliList) {
            if (!(tmpSmali.getPath().contains(ruleTarget) | tmpSmali.getPath().matches(ruleTargetRegex))) continue;
            for (String variable : new Regex().match(patAssign, rule, "replace")) {
                for (String str : variable.strip().split("=")) {
                    if (str.contains("${GROUP")) continue;
                    assignArr.add(str);
                }
            }
            ArrayList<String> valuesArr = new Regex().match(Pattern.compile(ruleMatch), tmpSmali.getBody(), "replace");
            for (int k = 0; k < valuesArr.size(); ++k) {
                if (Prefs.verbose_level <= 1) {
                    System.out.println("assigned " + valuesArr.get(k) + " to \"" + assignArr.get(k) + "\"");
                }
                assignMap.put(assignArr.get(k), valuesArr.get(k));
            }
        }
        if (assignMap.isEmpty()) {
            System.out.println("Nothing found in assign rule??");
        }
    }

    void add(String projectPath, String rule, String ruleTarget) {
        Pattern patSource = Pattern.compile("SOURCE:\\n(.+)");
        String ruleSource = new Regex().match(patSource, rule, "").get(0);
        if (Prefs.verbose_level == 0) {
            System.out.println("Source - " + ruleSource);
        }
        String src = System.getProperty("user.dir") + File.separator + "patches" + File.separator + "temp" + File.separator + ruleSource;
        String dst = projectPath + File.separator + ruleTarget;
        File file = new File(dst);
        if (file.exists()) {
            new IO().deleteInDirectory(file);
        }
        new IO().copy(src, dst);
        if (ruleSource.contains(".smali")) {
            if (Prefs.verbose_level <= 1) {
                System.out.println("Added.");
            }
            Smali newSmali = new Smali();
            newSmali.setPath(projectPath + File.separator + ruleTarget.replace('/', File.separatorChar));
            //if (Prefs.arch_device.equals("android")) newSmali.setBody((new IO().read(projectPath + File.separator + ruleTarget)).replace("\r\n", "\n"));
            //else newSmali.setBody((new IO().read(projectPath + File.separator + ruleTarget)).replace("\n", "\r\n"));
            newSmali.setBody(new IO().read(projectPath + File.separator + ruleTarget));
            smaliList.removeIf(smali -> smali.getPath().equals(newSmali.getPath()));
            smaliList.add(newSmali);
        }
    }

    void remove(String projectPath, ArrayList<String> targetArr) {
        if (Prefs.verbose_level == 0) {
            System.out.println("Dst - " + targetArr);
        }
        for (String ruleTarget : targetArr) {
            new IO().deleteInDirectory(new File(projectPath + File.separator + ruleTarget));
            smaliList.removeIf(smali -> smali.getPath().equals(projectPath + File.separator + ruleTarget));
        }
        if (Prefs.verbose_level <= 1 && !targetArr.get(0).contains("/temp")) {
            System.out.println("Removed.");
        }
    }
}