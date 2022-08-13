package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.rule.types.RemoveCode;

import java.util.ArrayList;

public class RemoveCodePatch extends Patch {

    public RemoveCodePatch(String path, String[] removeCodeTargets) {
        super(path);
        rules = new ArrayList<>(1);
        smaliNeeded = true;
        RemoveCode rule = new RemoveCode();
        rule.setTargets(new ArrayList<String>(removeCodeTargets.length));
        for (String s : removeCodeTargets) {
            rule.getTargets().add(s.trim());
        }
        rules.add(rule);
    }
}
