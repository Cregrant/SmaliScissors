package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.rules.IRule;
import com.github.cregrant.smaliscissors.utils.IO;
import com.github.cregrant.smaliscissors.utils.RuleParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Patch {
    private final File file;
    private final File tempDir;
    private final String name;
    private ArrayList<IRule> rules;
    private int currentRuleNum;
    public boolean smaliNeeded = false;
    public boolean xmlNeeded = false;
    private Map<String, String> assignMap = new HashMap<>();

    public Patch(String path) {
        file = new File(path);
        name = file.getName();
        tempDir = new File(file.getParentFile() + File.separator + "temp");
        parseRules();
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

    public void addAssignment(String key, String value) {
        assignMap.put(key, value);
    }

    public void createTempDir() {
        tempDir.delete();
        tempDir.mkdirs();
    }

    public File getTempDir() {
        return tempDir;
    }

    public void deleteTempDir() throws IOException {
        try {
            IO.delete(tempDir);
        } catch (IOException e) {
            throw new IOException(e.getCause() + " Temp directory is not deleted.");
        }
    }

    public String applyAssign(String string) {        //replace ${blah-blah} to some text
        if (assignMap.isEmpty() || string.isEmpty())
            return string;

        Set<Map.Entry<String, String>> set = assignMap.entrySet();
        if (Prefs.logLevel == Prefs.Log.DEBUG)
            Main.out.println("Replacing variables to text:\n" + set);

        for (Map.Entry<String, String> entry : set) {
            String key = "${" + entry.getKey() + "}";
            if (string.contains(key)) {
                String value = entry.getValue();
                string = string.replace(key, value);
                if (Prefs.logLevel == Prefs.Log.DEBUG)
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

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int b;
                while ((b = zis.read()) != -1) {
                    bos.write(b);
                }

                result = bos.toString();
                zis.close();
                break;
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    void reset() {
        currentRuleNum = 0;
        assignMap = new HashMap<>();
    }
}
