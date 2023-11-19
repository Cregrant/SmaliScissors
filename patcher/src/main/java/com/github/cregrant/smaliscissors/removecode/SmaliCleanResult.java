package com.github.cregrant.smaliscissors.removecode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmaliCleanResult {
    public List<SmaliTarget> cascadeTargets = new ArrayList<>();
    public Set<SmaliTarget> fieldsCanBeNull = new HashSet<>();

    public SmaliCleanResult() {
    }

    public SmaliCleanResult(SmaliTarget target) {
        cascadeTargets.add(target);
    }

    public SmaliCleanResult(Set<SmaliTarget> fieldsCanBeNull) {
        this.fieldsCanBeNull = fieldsCanBeNull;
    }

    public SmaliCleanResult(SmaliTarget target, Set<SmaliTarget> fieldsCanBeNull) {
        cascadeTargets.add(target);
        this.fieldsCanBeNull = fieldsCanBeNull;
    }

    public SmaliCleanResult merge(SmaliCleanResult result) {
        if (result != null && (!result.cascadeTargets.isEmpty() || !result.fieldsCanBeNull.isEmpty())) {
            cascadeTargets.addAll(result.cascadeTargets);
            fieldsCanBeNull.addAll(result.fieldsCanBeNull);
        }
        return this;
    }
}
