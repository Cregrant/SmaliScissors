package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.InputMismatchException;

import static com.github.cregrant.smaliscissors.rule.RuleParser.PAT_NAME;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class Rule {

    private static final Logger logger = LoggerFactory.getLogger(Rule.class);
    protected String name;
    protected boolean smali;
    protected boolean xml;

    protected Rule(String rawString) {
        if (!rawString.isEmpty()) {
            this.name = matchSingleLine(rawString, PAT_NAME);
        }
    }

    public static Rule parseRule(String rawString) throws EnumConstantNotPresentException, InputMismatchException {
        Rule rule;
        String typeString = rawString.substring(rawString.indexOf('[') + 1, rawString.indexOf(']'));
        Rule.Type type = Rule.Type.valueOf(typeString);

        switch (type) {
            case MATCH_ASSIGN:
                rule = new Assign(rawString);
                break;
            case ADD_FILES:
                rule = new Add(rawString);
                break;
            case MATCH_REPLACE:
                rule = new Replace(rawString);
                break;
            case REMOVE_FILES:
                rule = new RemoveFiles(rawString);
                break;
            case DUMMY:
                rule = new Dummy(rawString);
                break;
            case EXECUTE_DEX:
                rule = new ExecuteDex(rawString);
                break;
            case GOTO:
                rule = new Goto(rawString);
                break;
            case MATCH_GOTO:
                rule = new MatchGoto(rawString);
                break;
            case REMOVE_CODE:
                rule = new RemoveCode(rawString);
                break;
            case REMOVE_CODE_ACTION:
                rule = new RemoveCodeAction(rawString);
                break;
            default:
                throw new InputMismatchException();
        }

        if (rule.isValid()) {
            return rule;
        } else {
            throw new InputMismatchException();
        }
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return false;
    }

    public boolean smaliNeeded() {
        return smali;
    }

    public boolean xmlNeeded() {
        return xml;
    }

    public String nextRuleName() {
        return null;
    }

    public void apply(Project project, Patch patch) throws IOException {
    }

    enum Type {
        MATCH_ASSIGN,
        MATCH_REPLACE,
        MATCH_GOTO,
        ADD_FILES,
        REMOVE_FILES,
        REMOVE_CODE,
        REMOVE_CODE_ACTION,
        DUMMY,
        GOTO,
        EXECUTE_DEX
    }
}
