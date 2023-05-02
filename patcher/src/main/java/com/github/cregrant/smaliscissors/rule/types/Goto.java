package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

import static com.github.cregrant.smaliscissors.rule.RuleParser.GOTO;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class Goto extends Rule {

    private final String goTo;

    public Goto(String rawString) {
        super(rawString);
        goTo = matchSingleLine(rawString, GOTO);
    }

    @Override
    public boolean isValid() {
        return goTo != null;
    }

    @Override
    public String nextRuleName() {
        return goTo;
    }

    @Override
    public void apply(Project project, Patch patch) {
    }

    public String getGoTo() {
        return goTo;
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
