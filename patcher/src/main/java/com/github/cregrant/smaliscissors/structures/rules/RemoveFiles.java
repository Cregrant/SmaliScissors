package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;

import java.io.File;
import java.util.ArrayList;

public class RemoveFiles implements IRule {
    private String name;
    private ArrayList<String> targets;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null && !targets.isEmpty();
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
        for (String target : targets) {
            if (target.startsWith("L"))
                target = target.substring(1);

            ArrayList<String> deleted = Scanner.removeLoadedFile(project, target, true);
            for (String str : deleted)
                IO.delete(new File(project.getPath() + File.separator + str));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_FILES.\n");
        if (name != null)
            sb.append("Name: ").append(name).append('\n');
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        return sb.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }
}
