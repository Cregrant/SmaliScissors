package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.Rule;

public class XmlWorker {
    private final Project project;
    private final Rule rule;

    public XmlWorker(Project project, RemoveCode rule) {
        this.project = project;
        this.rule = rule;
    }


}
