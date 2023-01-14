package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.removecode_action_count;
import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.removecode_action_type;

public class RemoveCodeAction implements Rule {

    private static final Logger logger = LoggerFactory.getLogger(RemoveCodeAction.class);
    private String name;
    private Action action;
    private int actionCount;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return action != null && actionCount >= 0;
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
        ProjectProperties properties = project.getProperties();
        String currentAction = properties.get(removecode_action_type);
        int currentActionCount = Integer.parseInt(properties.get(removecode_action_count));

        if (currentAction.equals(action.name())) {     //the same action already scheduled
            properties.set(removecode_action_count, String.valueOf(currentActionCount + actionCount));
        } else {
            if (currentActionCount > 0 && !currentAction.equals(action.name())) {   //other action already scheduled
                logger.warn("Remove code action was changed from " + currentAction + " to " + action.name());
            }
            properties.set(removecode_action_type, action.name());
            properties.set(removecode_action_count, String.valueOf(currentActionCount + actionCount));
        }

        currentAction = properties.get(removecode_action_type);
        currentActionCount = Integer.parseInt(properties.get(removecode_action_count));
        logger.info("Pending actions: " + currentAction + " " + currentActionCount + " next targets");
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setActionCount(int actionCount) {
        this.actionCount = actionCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_CODE_ACTION.\n");
        if (name != null) {
            sb.append("Name: ").append(name).append('\n');
        }
        sb.append("Action: ").append(action.name()).append('\n');
        sb.append("Count: ").append(actionCount).append('\n');
        return sb.toString();
    }

    public enum Action {
        APPLY,
        SKIP
    }
}
