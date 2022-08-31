package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.removecode.SmaliWorker;

import java.io.IOException;
import java.util.List;

public class RemoveCode implements Rule {
    private String name;
    private List<String> targets;
    private boolean internal;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getTargets() != null && !getTargets().isEmpty();
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

    public void setInternal() {
        this.internal = true;
    }

    public boolean isInternal() {
        return internal;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        if (internal) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_CODE.\n");
        if (name != null) {
            sb.append("Name: ").append(name).append('\n');
        }
        sb.append("Targets:\n");
        for (String target : getTargets()) {
            sb.append("    ").append(target).append("\n");
        }
        return sb.toString();
    }
}
