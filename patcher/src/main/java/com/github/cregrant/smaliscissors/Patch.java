package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.rules.IRule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Patch {
    private final File file;
    private final String name;
    private ArrayList<IRule> rules;
    private int currentRuleNum;
    public boolean smaliNeeded = false;
    public boolean xmlNeeded = false;
    private static Map<String, String> assignMap = new HashMap<>();

    public Patch(String path) {
        this.file = new File(path);
        name = file.getName();
        parseRules();
    }

    public void addRule(IRule rule) {
        rules.add(rule);
    }

    public void jumpToRuleName(String someName) {
        if (someName == null)
            return;
        for (int i = 0; i < rules.size(); i++) {
            String ruleName = rules.get(i).getName();
            if (someName.equalsIgnoreCase(ruleName)) {
                currentRuleNum = i;
                break;
            }
        }
    }

    public IRule getNextRule() {
        if (currentRuleNum == rules.size())
            return null;
        else {
            IRule rule = rules.get(currentRuleNum);
            currentRuleNum++;
            return rule;
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public int getRulesCount() {
        return rules.size();
    }

    public void addAssignment(String key, String value) {
        assignMap.put(key, value);
    }

    public String applyAssign(String string) {        //replace ${blah-blah} to some text
        if (assignMap.isEmpty() || string.isEmpty())
            return string;

        Set<Map.Entry<String, String>> set = assignMap.entrySet();
        if (Prefs.verbose_level == 0)
            Main.out.println("Replacing variables to text:\n" + set);

        for (Map.Entry<String, String> entry : set) {
            String key = "${" + entry.getKey() + "}";
            if (string.contains(key)) {
                String value = entry.getValue();
                string = string.replace(key, value);
                if (Prefs.verbose_level == 0)
                    Main.out.println(key + " -> " + value);
            }
        }
        return string;
    }

    private void parseRules() {
        String patchString = loadRules();
        if (patchString == null) {
            Main.out.println("patch.txt not found inside " + file + "!");
            rules = new ArrayList<>(0);
            return;
        }

        RuleParser parser = new RuleParser(patchString);
        smaliNeeded = parser.isSmaliNeeded();
        xmlNeeded = parser.isXmlNeeded();
        rules = parser.getRules();
    }

    private String loadRules() {
        String result = null;
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.getName().equals("patch.txt"))
                    continue;

                byte[] buffer = new byte[8192];
                while (zis.read(buffer) != -1)
                    buffer = Arrays.copyOf(buffer, buffer.length * 2);

                result = new String(buffer, StandardCharsets.UTF_8);
                zis.close();
                break;
            }
        } catch (FileNotFoundException e) {
            Main.out.println("File not found!");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        } catch (IOException e) {
            Main.out.println("Error during extracting zip file.");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
        return result;
    }

    void reset() {
        currentRuleNum = 0;
        assignMap = new HashMap<>();
    }
}
