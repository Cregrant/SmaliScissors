package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static java.lang.System.out;

class ProcessRule {
    static ArrayList<decompiledFile> smaliList = new ArrayList<>();
    static ArrayList<decompiledFile> xmlList = new ArrayList<>();
    static int patchedFilesNum = 0;
    static HashMap<String, String> assignMap = new HashMap<>();

    @SuppressWarnings("RegExpRedundantEscape")
    void matchReplace(Rule rule) {
        applyAssign(rule);
        //important escape for android
        rule.replacement = rule.replacement.replaceAll("\\$\\{GROUP(\\d{1,2})\\}", "\\$$1");
        CountDownLatch cdl = new CountDownLatch(Prefs.max_thread_num);

        Object lock = new Object();
        AtomicBoolean error = new AtomicBoolean(false);
        AtomicInteger currentNum = new AtomicInteger(0);
        int thrNum = Prefs.max_thread_num;
        int totalNum;
        for (int cycle = 1; cycle <= thrNum; ++cycle) {
            if (rule.isXml)
                totalNum = xmlList.size();
            else
                totalNum = smaliList.size();
            int finalTotalNum = totalNum;
            new Thread(() -> {
                try {
                    int num;
                    decompiledFile dFile;
                    while ((num = currentNum.getAndIncrement()) < finalTotalNum) {

                        if (rule.isXml)
                            dFile = xmlList.get(num);
                        else
                            dFile = smaliList.get(num);
                        replace(dFile, rule);

                        if (dFile.isNotModified()) continue;
                        if (Prefs.verbose_level == 0) {
                            if (rule.isSmali)
                                out.println(dFile.getPath().replaceAll(".+/smali", "smali") + " patched.");
                            else
                                out.println(dFile.getPath().replaceAll(".+/res/", "") + " patched.");
                        }
                        synchronized (lock) {
                            ++patchedFilesNum;
                        }
                    }
                    cdl.countDown();
                } catch (Exception e) {
                    if (!error.get()) {
                        error.set(true);
                        out.println(e.getMessage());
                    }
                    System.exit(1);
                }
            }).start();
        }
        try {
            cdl.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (Prefs.verbose_level <= 2) {
            if (rule.isSmali)
                out.println(patchedFilesNum + " smali files patched.");
            else
                out.println(patchedFilesNum + " xml files patched.");
        }
        patchedFilesNum = 0;
    }

    private void replace(decompiledFile dFile, Rule rule) {
        String smaliPath = dFile.getPath();
        if (smaliPath.contains(rule.target) || smaliPath.matches(rule.target)) {
            String smaliBody = dFile.getBody();
            String smaliBodyNew;
            if (rule.isRegex)
                smaliBodyNew = smaliBody.replaceAll(rule.match, rule.replacement);
            else
                smaliBodyNew = smaliBody.replace(rule.match, rule.replacement);
            if (!smaliBodyNew.equals(smaliBody)) {
                dFile.setBody(smaliBodyNew);
                dFile.setModified(true);
            }
        }
    }

    void assign(Rule rule) {
        ArrayList<String> assignArr = new ArrayList<>();
        decompiledFile dFile;
        int end;
        if (rule.isXml)
            end = xmlList.size();
        else end = smaliList.size();
        for (int k=0; k<end; k++) {
            if (rule.isXml)
                dFile = xmlList.get(k);
            else dFile = smaliList.get(k);
            String path = dFile.getPath();
            if (!(path.contains(rule.target) || path.matches(rule.target))) continue;
            for (String variable : rule.assignments) {
                for (String str : variable.split("=")) {
                    if (str.contains("${GROUP")) continue;
                    assignArr.add(str);
                }
            }
            ArrayList<String> valuesArr = new Regex().matchMultiLines(Pattern.compile(rule.match), dFile.getBody(), "replace");
            for (int j = 0; j < valuesArr.size(); ++j) {
                if (Prefs.verbose_level <= 1) {
                    out.println("assigned " + valuesArr.get(j) + " to \"" + assignArr.get(j) + "\"");
                }
                assignMap.put(assignArr.get(j), valuesArr.get(j));
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
            decompiledFile newSmali = new decompiledFile(false);
            newSmali.setPath(projectPath + File.separator + rule.target.replace('/', File.separatorChar));
            newSmali.setBody(new IO().read(projectPath + File.separator + rule.target));
            for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
                decompiledFile sm = smaliList.get(i);
                if (sm.getPath().equals(newSmali.getPath())) smaliList.remove(i);
            }
            smaliList.add(newSmali);
        }
    }

    void remove(String projectPath, Rule rule) {
        new IO().deleteAll(new File(projectPath + File.separator + rule.target));
        for (int i = 0, smaliListSize = smaliList.size(); i < smaliListSize; i++) {
            decompiledFile sm = smaliList.get(i);
            if (sm.getPath().equals(projectPath + File.separator + rule.target)) {
                smaliList.remove(i);
                i--;
            }
        }
        if (Prefs.verbose_level <= 1 && !rule.target.contains("/temp")) {
            out.println("Removed.");
        }
    }

    void matchGoto(Rule rule, Patch patch) {
        applyAssign(rule);
        CountDownLatch cdl = new CountDownLatch(Prefs.max_thread_num);
        AtomicInteger currentNum = new AtomicInteger(0);
        int thrNum = Prefs.max_thread_num;
        AtomicBoolean running = new AtomicBoolean(true);
        Regex regex = new Regex();
        Pattern pattern = Pattern.compile(rule.match);
        for (int cycle = 1; cycle <= thrNum; ++cycle) {
            int totalSmaliNum = smaliList.size() - 1;
            new Thread(() -> {
                int num;
                while ((num = currentNum.getAndIncrement()) <= totalSmaliNum & running.get()) {
                    decompiledFile smali = smaliList.get(num);
                    if (regex.matchSingleLine(pattern, smali.getBody())!=null) {
                        patch.setRuleName(rule.goTo);
                        running.set(false);
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

    private void applyAssign(Rule rule) {
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
    }

    public void dex() {
        out.println("HAHa very fun");
    }
}