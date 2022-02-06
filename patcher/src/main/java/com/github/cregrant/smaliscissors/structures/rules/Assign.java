package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.DecompiledFile;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Assign implements IRule {
    public String name;
    public String match;
    public ArrayList<String> targets;
    public boolean isRegex = false;
    public boolean isSmali = false;
    public boolean isXml = false;
    public ArrayList<String> assignments;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null && !targets.isEmpty() && match != null && assignments != null && !assignments.isEmpty();
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public boolean canBeMerged(IRule otherRule) {
        return false;
    }

    @Override
    public void apply(Project project, Patch patch) {
        ArrayList<String> keyList = new ArrayList<>();
        ArrayList<String> valueList;
        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        for (DecompiledFile dFile : files) {
            if (!dFile.getPath().matches(targets.get(0)))
                continue;

            for (String variable : assignments) {
                keyList.add(variable.substring(0, variable.indexOf('=')));
            }
            valueList = Regex.matchMultiLines(dFile.getBody(), Pattern.compile(patch.applyAssign(match)), Regex.MatchType.FULL);
            if (keyList.size() < valueList.size())
                Main.out.println("WARNING: MATCH_ASSIGN found excess results...");
            else if (keyList.size() > valueList.size())
                Main.out.println("WARNING: MATCH_ASSIGN found not enough results...");

            int max = Math.max(keyList.size(), valueList.size());
            for (int i = 0; i < max; ++i) {
                String key = keyList.get(i);
                String value = valueList.get(i);
                patch.addAssignment(key, value);
                if (Prefs.verbose_level <= 1) {
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
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        sb.append("Assignments:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        sb.append("Match: ").append(match).append('\n');
        sb.append("Regex: ").append(isRegex).append('\n');
        return sb.toString();
    }
}
