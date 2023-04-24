package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

public class Dummy extends Rule {

    public Dummy(String rawString) {
        super(rawString);
    }

    @Override
    public boolean isValid() {
        return name != null;
    }

    @Override
    public void apply(Project project, Patch patch) {
    }

    @Override
    public String toString() {
        return "Type: DUMMY\nName: " + name + '\n';
    }
}
