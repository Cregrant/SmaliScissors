package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.util.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Assign implements Rule {
    private String name;
    private String target;
    private String match;
    private boolean isRegex;
    private boolean isSmali;
    private boolean isXml;
    private ArrayList<String> assignments;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getTarget() != null && getMatch() != null && getAssignments() != null && !getAssignments().isEmpty();
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali();
    }

    @Override
    public boolean xmlNeeded() {
        return isXml();
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        ArrayList<String> keyList = new ArrayList<>();
        Pattern targetCompiled = Pattern.compile(getTarget());
        ArrayList<String> valueList;
        List<? extends DecompiledFile> files;
        if (isSmali()) {
            files = project.getSmaliList();
        } else if (isXml()) {
            files = project.getXmlList();
        } else {
            throw new IllegalStateException("Not smali nor xml rule.");
        }

        for (DecompiledFile dFile : files) {
            if (!targetCompiled.matcher(dFile.getPath()).matches()) {
                continue;
            }

            for (String variable : getAssignments()) {
                keyList.add(variable.substring(0, variable.indexOf('=')));
            }
            valueList = Regex.matchMultiLines(dFile.getBody(), Pattern.compile(patch.applyAssign(getMatch())), Regex.ResultFormat.FULL);
            if (keyList.size() < valueList.size()) {
                Main.out.println("WARNING: MATCH_ASSIGN found excess results...");
            } else if (keyList.size() > valueList.size()) {
                Main.out.println("WARNING: MATCH_ASSIGN found not enough results...");
            }

            int min = Math.min(keyList.size(), valueList.size());
            if (min == 0) {
                return;
            }
            for (int i = 0; i < min; ++i) {
                String key = keyList.get(i);
                String value = valueList.get(i);
                patch.addAssignment(key, value);
                if (Prefs.logLevel == Prefs.Log.DEBUG) {
                    if (value.length() > 300) {
                        value = value.substring(0, 60) + " ... " + value.substring(value.length() - 60);
                    }
                    Main.out.println("Assigned \"" + value + "\" to \"" + key + "\"");
                }
            }
        }
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public void setRegex(boolean regex) {
        isRegex = regex;
    }

    public boolean isSmali() {
        return isSmali;
    }

    public void setSmali(boolean smali) {
        isSmali = smali;
    }

    public boolean isXml() {
        return isXml;
    }

    public void setXml(boolean xml) {
        isXml = xml;
    }

    public ArrayList<String> getAssignments() {
        return assignments;
    }

    public void setAssignments(ArrayList<String> assignments) {
        this.assignments = assignments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  MATCH_ASSIGN\n");
        if (name != null) {
            sb.append("Name:  ").append(name).append('\n');
        }
        sb.append("Target: ").append(target).append("\n");
        sb.append("Assignments:\n");
        for (String assign : assignments) {
            sb.append("    ").append(assign).append("\n");
        }
        sb.append("Match: ").append(match).append('\n');
        sb.append("Regex: ").append(isRegex);
        return sb.toString();
    }
}
