package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

public class Goto implements Rule {

    public String name;
    public String goTo;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return goTo != null;
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
        return goTo;
    }

    @Override
    public void apply(Project project, Patch patch) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: GOTO.\n");
        if (name != null) {
            sb.append("Name: ").append(name).append('\n');
        }
        sb.append("Goto: ").append(goTo);
        return sb.toString();
    }
}
