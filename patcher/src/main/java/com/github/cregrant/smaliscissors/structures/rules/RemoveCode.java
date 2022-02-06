package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.smali.SmaliAnalyzer;

import java.util.ArrayList;

public class RemoveCode implements IRule {
    public String name;
    public ArrayList<String> targets;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null;
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
        RemoveFiles removeFiles = new RemoveFiles();
        removeFiles.setTargets(targets);
        removeFiles.apply(project, patch);

        new SmaliAnalyzer().clear(project.getSmaliList(), this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_CODE.\n");
        if (name != null)
            sb.append("Name: ").append(name).append('\n');
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        return sb.toString();
    }
}
