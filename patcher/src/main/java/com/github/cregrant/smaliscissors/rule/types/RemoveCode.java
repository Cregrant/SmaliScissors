package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.ProjectProperties;
import com.github.cregrant.smaliscissors.removecode.SmaliWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.*;

public class RemoveCode implements Rule {

    private static final Logger logger = LoggerFactory.getLogger(RemoveCode.class);
    private String name;
    private List<String> targets;
    private boolean internal;

    public int getLastTargetIndex(Project project) {
        ProjectProperties properties = project.getProperties();
        String lastTarget = properties.get(last_removecode_target);

        if (!lastTarget.isEmpty() && targets.hashCode() == Integer.parseInt(properties.get(targets_hash))) {
            setLastTarget(project, "");
            int pos = targets.indexOf(lastTarget);
            if (pos == -1) {
                logger.error("Hashcode collision happened? Patch won't be resumed from the last target");
            }
            return pos;
        }

        return -1;
    }

    public void setLastTarget(Project project, String target) {
        ProjectProperties properties = project.getProperties();
        properties.set(last_removecode_target, target);
        properties.set(targets_hash, String.valueOf(targets.hashCode()));
    }

    public int getSkipCount(Project project) {
        String storedAction = project.getProperties().get(removecode_action_type);
        if (RemoveCodeAction.Action.SKIP.name().equals(storedAction)) {
            return Integer.parseInt(project.getProperties().get(removecode_action_count));
        }
        return 0;
    }

    public int getApplyCount(Project project) {
        String storedAction = project.getProperties().get(removecode_action_type);
        if (RemoveCodeAction.Action.APPLY.name().equals(storedAction)) {
            return Integer.parseInt(project.getProperties().get(removecode_action_count));
        }
        return 0;
    }

    public void removeActionCount(Project project) {
        project.getProperties().set(removecode_action_count, "0");
    }

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
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            sb.append("    ").append(target).append("\n");
            if (i >= 30 && !logger.isDebugEnabled()) {
                sb.append("    ... + ").append(targets.size() - i - 1).append(" more lines\n");
                break;
            }
        }
        return sb.toString();
    }
}
