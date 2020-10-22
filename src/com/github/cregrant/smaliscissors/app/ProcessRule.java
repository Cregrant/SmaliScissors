package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

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

class ProcessRule {
    static ArrayList<Smali> smaliList = new ArrayList<>();
    static int patchedFilesNum = 0;
    static HashMap<String, String> assignMap = new HashMap<>();

    @SuppressWarnings("RegExpRedundantEscape")
    String replace(String rule, Pattern patMatch, String ruleTarget) {
        Pattern patReplacement = Pattern.compile("REPLACE:\\R([\\S\\s]*?)\\R?\\[/MATCH_REPLACE]");
        String ruleTargetRegex = ruleTarget.replace("\\", "\\\\").replace(".smali", "\\.smali");
        String ruleMatch = new Regex().matchSingleLine(patMatch, rule);
        String ruleReplacement = new Regex().matchSingleLine(patReplacement, rule);
        if (!assignMap.isEmpty()) {
            Set<Map.Entry<String, String>> set = assignMap.entrySet();
            if (Prefs.verbose_level == 0) {
                out.println("Replacing variables to text...\n" + set);
            }
            for (Map.Entry<String, String> entry : set) {
                String key = "${" + entry.getKey() + "}";
                if (!ruleMatch.contains(key)) continue;
                String value = entry.getValue();
                if (Prefs.verbose_level == 0) {
                    out.println(key + " -> " + value);
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
                //important escape for android
                String finalRuleReplacement = ruleReplacement.replaceAll("\\$\\{GROUP(\\d{1,2})\\}", "\\$$1");
                new Thread(() -> {
                    int num;
                    while ((num = currentNum.getAndIncrement()) <= totalSmaliNum) {
                        Smali smali = smaliList.get(num);
                        new ProcessRule().simpleReplace(smali, finalRuleMatch, ruleTarget, ruleTargetRegex, finalRuleReplacement);
                        if (smali.isNotModified()) continue;
                        if (Prefs.verbose_level == 0) {
                            out.println(smali.getPath().replaceAll(".+/smali", "smali") + " patched.");
                        }
                        synchronized (patReplacement) {
                            ++patchedFilesNum;
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
            //FIXME CATCH ME
            out.println("Error - some [MATCH_ASSIGN] rule was processed with an error");
            if (Prefs.verbose_level == 0) e.printStackTrace();
            return "error";
        }
        if (Prefs.verbose_level <= 2) {
            out.println(patchedFilesNum + " files patched.");
        }
        patchedFilesNum = 0;
        return "";
    }

    private void simpleReplace(Smali tmpSmali, String ruleMatch, String ruleTarget, String ruleTargetRegex, String ruleReplacement) {
        String smaliPath = tmpSmali.getPath();
        if (smaliPath.contains(ruleTarget) | smaliPath.matches(ruleTargetRegex)) {
            String smaliBody = tmpSmali.getBody();
            String smaliBodyNew = smaliBody.replaceAll(ruleMatch, ruleReplacement);
            if (!smaliBodyNew.equals(smaliBody)) {
                tmpSmali.setBody(smaliBodyNew);
                tmpSmali.setModified(true);
            }
        }
    }

    void assign(String rule, Pattern patMatch, String ruleTarget) {
        Pattern patAssign = Pattern.compile("\\n(.+?=\\$\\{GROUP\\d})");
        String ruleTargetRegex = ruleTarget.replace("\\", "\\\\").replace("/", "\\").replace(".smali", "\\.smali");
        String ruleMatch = new Regex().matchSingleLine(patMatch, rule);
        if (Prefs.verbose_level == 0) {
            out.println("Match - " + ruleMatch);
        }
        ArrayList<String> assignArr = new ArrayList<>();
        for (Smali tmpSmali : smaliList) {
            if (!(tmpSmali.getPath().contains(ruleTarget) | tmpSmali.getPath().matches(ruleTargetRegex))) continue;
            for (String variable : new Regex().matchMultiLines(patAssign, rule, "replace")) {
                for (String str : variable.split("=")) {
                    if (str.contains("${GROUP")) continue;
                    assignArr.add(str);
                }
            }
            ArrayList<String> valuesArr = new Regex().matchMultiLines(Pattern.compile(ruleMatch), tmpSmali.getBody(), "replace");
            for (int k = 0; k < valuesArr.size(); ++k) {
                if (Prefs.verbose_level <= 1) {
                    out.println("assigned " + valuesArr.get(k) + " to \"" + assignArr.get(k) + "\"");
                }
                assignMap.put(assignArr.get(k), valuesArr.get(k));
            }
        }
        if (assignMap.isEmpty()) {
            out.println("Nothing found in assign rule??");
        }
    }

    void add(String projectPath, String rule, String ruleTarget) {
        Pattern patSource = Pattern.compile("SOURCE:\\n(.+)");
        Pattern patExtract = Pattern.compile("EXTRACT:\\R(.+)");
        boolean extractZip = Boolean.parseBoolean(new Regex().matchSingleLine(patExtract, rule));
        String ruleSource = new Regex().matchSingleLine(patSource, rule);

        if (Prefs.verbose_level == 0) {
            out.println("Source - " + ruleSource);
            if (extractZip) out.println("Extracting zip..");
        }
        String src = new CompatibilityData().getTempDir() + File.separator + ruleSource;
        String dst = projectPath + File.separator + ruleTarget;
        File file = new File(dst);
        if (file.exists()) {
            new IO().deleteAll(file);
        }
        if (extractZip) new IO().zipExtract(src, dst);
        else new IO().copy(src, dst);
        if (ruleSource.contains(".smali")) {
            if (Prefs.verbose_level <= 1) {
                out.println("Added.");
            }
            Smali newSmali = new Smali();
            newSmali.setPath(projectPath + File.separator + ruleTarget.replace('/', File.separatorChar));
            newSmali.setBody(new IO().read(projectPath + File.separator + ruleTarget));
            for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
                Smali sm = smaliList.get(i);
                if (sm.getPath().equals(newSmali.getPath())) smaliList.remove(i);
            }
            smaliList.add(newSmali);
        }
    }

    void remove(String projectPath, String target) {
        if (Prefs.verbose_level == 0) {
            out.println("Dst - " + target);
        }
        new IO().deleteAll(new File(projectPath + File.separator + target));
        for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
            Smali sm = smaliList.get(i);
            if (sm.getPath().equals(projectPath + File.separator + target)) {
                smaliList.remove(i);
                i--;
            }
        }
        if (Prefs.verbose_level <= 1 && !target.contains("/temp")) {
            out.println("Removed.");
        }
    }
}