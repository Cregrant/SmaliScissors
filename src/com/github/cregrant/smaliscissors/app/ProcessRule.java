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
    String replace(Rule rule) {
        if (!assignMap.isEmpty()) {
            Set<Map.Entry<String, String>> set = assignMap.entrySet();      //replace ${GROUP}
            if (Prefs.verbose_level == 0) {
                out.println("Replacing variables to text...\n" + set);
            }
            for (Map.Entry<String, String> entry : set) {
                String key = "${" + entry.getKey() + "}";
                if (!rule.match.contains(key)) continue;
                String value = entry.getValue();
                if (Prefs.verbose_level == 0) {
                    out.println(key + " -> " + value);
                }
                rule.match = rule.match.replace(key, value);
            }
        }
        //important escape for android
        rule.replacement = rule.replacement.replaceAll("\\$\\{GROUP(\\d{1,2})\\}", "\\$$1");
        CountDownLatch cdl = new CountDownLatch(Prefs.max_thread_num);

        try {
            Object lock = new Object();
            AtomicInteger currentNum = new AtomicInteger(0);
            int thrNum = Prefs.max_thread_num;
            for (int cycle = 1; cycle <= thrNum; ++cycle) {
                int totalSmaliNum = smaliList.size() - 1;
                new Thread(() -> {
                    int num;
                    while ((num = currentNum.getAndIncrement()) <= totalSmaliNum) {
                        Smali smali = smaliList.get(num);
                        new ProcessRule().simpleReplace(smali, rule);
                        if (smali.isNotModified()) continue;
                        if (Prefs.verbose_level == 0) {
                            out.println(smali.getPath().replaceAll(".+/smali", "smali") + " patched.");
                        }
                        synchronized (lock) {
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

    private void simpleReplace(Smali tmpSmali, Rule rule) {
        String smaliPath = tmpSmali.getPath();
        if (smaliPath.contains(rule.target) | smaliPath.matches(rule.target)) {
            String smaliBody = tmpSmali.getBody();
            String smaliBodyNew;
            if (rule.isRegex)
                smaliBodyNew = smaliBody.replaceAll(rule.match, rule.replacement);
            else
                smaliBodyNew = smaliBody.replace(rule.match, rule.replacement);
            if (!smaliBodyNew.equals(smaliBody)) {
                tmpSmali.setBody(smaliBodyNew);
                tmpSmali.setModified(true);
            }
        }
    }

    void assign(Rule rule) {
        ArrayList<String> assignArr = new ArrayList<>();
        for (Smali tmpSmali : smaliList) {
            if (!(tmpSmali.getPath().contains(rule.target) | tmpSmali.getPath().matches(rule.target))) continue;
            for (String variable : rule.assignments) {
                for (String str : variable.split("=")) {
                    if (str.contains("${GROUP")) continue;
                    assignArr.add(str);
                }
            }
            ArrayList<String> valuesArr = new Regex().matchMultiLines(Pattern.compile(rule.match), tmpSmali.getBody(), "replace");
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

    void add(String projectPath, Rule rule) {
        String src = new CompatibilityData().getTempDir() + File.separator + rule.source;
        String dst = projectPath + File.separator + rule.target;
        File file = new File(dst);
        if (file.exists()) {
            new IO().deleteAll(file);
        }
        if (rule.extract) new IO().zipExtract(src, dst);
        else new IO().copy(src, dst);
        if (rule.source.contains(".smali")) {
            if (Prefs.verbose_level <= 1) {
                out.println("Added.");
            }
            Smali newSmali = new Smali();
            newSmali.setPath(projectPath + File.separator + rule.target.replace('/', File.separatorChar));
            newSmali.setBody(new IO().read(projectPath + File.separator + rule.target));
            for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
                Smali sm = smaliList.get(i);
                if (sm.getPath().equals(newSmali.getPath())) smaliList.remove(i);
            }
            smaliList.add(newSmali);
        }
    }

    void remove(String projectPath, Rule rule) {
        new IO().deleteAll(new File(projectPath + File.separator + rule.target));
        for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
            Smali sm = smaliList.get(i);
            if (sm.getPath().equals(projectPath + File.separator + rule.target)) {
                smaliList.remove(i);
                i--;
            }
        }
        if (Prefs.verbose_level <= 1 && !rule.target.contains("/temp")) {
            out.println("Removed.");
        }
    }
}