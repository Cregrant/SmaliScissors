package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.structures.common.DecompiledFile;
import com.github.cregrant.smaliscissors.utils.Regex;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Assign implements IRule {
    public String name;
    public String target;
    public String match;
    public boolean isRegex;
    public boolean isSmali;
    public boolean isXml;
    public ArrayList<String> assignments;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return target != null && match != null && assignments != null && !assignments.isEmpty();
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali;
    }

    @Override
    public boolean xmlNeeded() {
        return isXml;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        ArrayList<String> keyList = new ArrayList<>();
        Pattern targetCompiled = Pattern.compile(target);
        ArrayList<String> valueList;
        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        for (DecompiledFile dFile : files) {
            if (!targetCompiled.matcher(dFile.getPath()).matches())
                continue;

            for (String variable : assignments) {
                keyList.add(variable.substring(0, variable.indexOf('=')));
            }
            valueList = Regex.matchMultiLines(dFile.getBody(), Pattern.compile(patch.applyAssign(match)), Regex.ResultFormat.FULL);
            if (keyList.size() < valueList.size())
                Main.out.println("WARNING: MATCH_ASSIGN found excess results...");
            else if (keyList.size() > valueList.size())
                Main.out.println("WARNING: MATCH_ASSIGN found not enough results...");

            int min = Math.min(keyList.size(), valueList.size());
            if (min == 0)
                return;
            for (int i = 0; i < min; ++i) {
                String key = keyList.get(i);
                String value = valueList.get(i);
                patch.addAssignment(key, value);
                if (Prefs.logLevel == Prefs.Log.DEBUG) {
                    if (value.length() > 300)
                        value = value.substring(0, 60) + " ... " + value.substring(value.length() - 60);
                    Main.out.println("Assigned \"" + value + "\" to \"" + key + "\"");
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  MATCH_ASSIGN\n");
        if (name != null)
            sb.append("Name:  ").append(name).append('\n');
        sb.append("Target: ").append(target).append("\n");
        sb.append("Assignments:\n");
        for (String assign : assignments)
            sb.append("    ").append(assign).append("\n");
        sb.append("Match: ").append(match).append('\n');
        sb.append("Regex: ").append(isRegex);
        return sb.toString();
    }
}
