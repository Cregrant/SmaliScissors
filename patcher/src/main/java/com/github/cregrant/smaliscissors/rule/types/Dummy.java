package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

public class Dummy implements Rule {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getName() != null;
    }

    @Override
    public boolean smaliNeeded() {
        return false;
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
    public void apply(Project project, Patch patch) {
    }

    @Override
    public String toString() {
        return "Type: DUMMY\nName: " + name + '\n';
    }
}
