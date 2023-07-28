package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.removecode_action_count;
import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.removecode_action_type;
import static com.github.cregrant.smaliscissors.rule.RuleParser.ACTION;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class RemoveCodeAction extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(RemoveCodeAction.class);
    private Action action;
    private int actionCount;

    public RemoveCodeAction(String rawString) {
        super(rawString);
        String actionString = matchSingleLine(rawString, ACTION);
        if (actionString != null) {
            try {
                actionString = actionString.trim();
                int spacePos = actionString.lastIndexOf(' ');
                action = RemoveCodeAction.Action.valueOf(actionString.substring(0, spacePos).toUpperCase());
                actionCount = Integer.parseInt(actionString.substring(spacePos + 1));
            } catch (Exception e) {
                actionCount = -1;
            }
        }
    }

    @Override
    public boolean isValid() {
        return action != null && actionCount >= 0;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        ProjectProperties properties = project.getProperties();
        String currentAction = properties.get(removecode_action_type);
        int currentActionCount = Integer.parseInt(properties.get(removecode_action_count));

        int newActionCount = currentActionCount + actionCount;
        if (currentActionCount > 0) {   //other action already scheduled
            logger.warn("Remove code action was changed from " + currentAction + " to " + action.name());
            newActionCount = actionCount;
        }
        properties.set(removecode_action_type, action.name());
        properties.set(removecode_action_count, String.valueOf(newActionCount));
        logger.info("Pending actions: " + action.name() + " " + newActionCount + " next targets");
    }

    public Action getAction() {
        return action;
    }

    public int getActionCount() {
        return actionCount;
    }

    @Override
    public String toStringShort() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("(").append(name).append(") ");
        }
        sb.append("Removing code action:");
        sb.append("\n  ").append(action.name()).append(" next ").append(actionCount).append(" targets\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:   REMOVE_CODE_ACTION.\n");
        if (name != null) {
            sb.append("Name:   ").append(name).append('\n');
        }
        sb.append("Action: ").append(action.name()).append('\n');
        sb.append("Count:  ").append(actionCount).append('\n');
        return sb.toString();
    }

    public enum Action {
        APPLY,
        SKIP
    }
}
