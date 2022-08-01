package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.smali.SmaliWorker;

import java.io.IOException;
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
    public boolean smaliNeeded() {
        return true;
    }

    @Override
    public boolean xmlNeeded() {
        return false;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        SmaliWorker smaliWorker = new SmaliWorker(project, patch, this);
        smaliWorker.run();
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
