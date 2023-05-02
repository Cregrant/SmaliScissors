package com.github.cregrant.smaliscissors.rule;

import com.github.cregrant.smaliscissors.rule.types.*;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.util.HashMap;

import static com.github.cregrant.smaliscissors.rule.RuleGenerator.Parameters.*;
import static com.github.cregrant.smaliscissors.rule.types.Rule.parseRule;
import static org.junit.jupiter.api.Assertions.*;

class RuleParserTest {

    private final String name = "testName";
    private final String match = "\\.class public (.+)";
    private final String target = "smali*/TestClass.smali";
    private final String assign = "all=${GROUP0}\nnotAll=${GROUP1}";
    private final String script = "script.dex";
    private final String mainClass = "test.Main";
    private final String entrance = "main";
    private final String param = "hello?";
    private final String goTo = "end";
    private final String action = "SKIP 1";
    private final String replace = "some replacement";
    private final Boolean booleanTrue = true;

    @Rule
    private final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    void testParseInvalidRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.INVALID)
                .addParameter(NAME, name);
        Assertions.assertThrows(Exception.class, () -> parseRule(generator.toString()));
    }

    @Test
    void testParseIncompleteRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.DUMMY);
        assertThrows(Exception.class, () -> parseRule(generator.toString()));
    }

    @Test
    void testParseAddRule() {
        String source_archive = "AddTests.zip";
        String target = "smali/";

        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.ADD)
                .addParameter(NAME, name)
                .addParameter(SOURCE, source_archive)
                .addParameter(EXTRACT, booleanTrue.toString())
                .addParameter(TARGET, target);

        Add rule = (Add) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getSource(), source_archive);
        assertEquals(rule.isExtract(), booleanTrue);
        assertEquals(rule.getTarget(), target);
    }

    @Test
    void testParseAssignRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.ASSIGN)
                .addParameter(NAME, name)
                .addParameter(MATCH, match)
                .addParameter(TARGET, target)
                .addParameter(REGEX, booleanTrue.toString())
                .addParameter(ASSIGN, assign);

        Assign rule = (Assign) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getMatch(), match);
        assertEquals(rule.getTarget(), target);
        assertEquals(rule.isRegex(), booleanTrue);

        HashMap<String, String> assignments = new HashMap<>();
        for (String line : assign.split("\n")) {
            String[] arr = line.split("=");
            assignments.put(arr[0], arr[1]);
        }
        assertEquals(rule.getAssignments(), assignments);
    }

    @Test
    void testParseDummyRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.DUMMY)
                .addParameter(NAME, name);

        Dummy rule = (Dummy) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
    }

    @Test
    void testParseExecuteDexRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.EXECUTE_DEX)
                .addParameter(NAME, name)
                .addParameter(SMALI_NEEDED, booleanTrue.toString())
                .addParameter(SCRIPT, script)
                .addParameter(MAIN_CLASS, mainClass)
                .addParameter(ENTRANCE, entrance)
                .addParameter(PARAM, param);

        ExecuteDex rule = (ExecuteDex) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.smaliNeeded(), booleanTrue);
        assertEquals(rule.getScript(), script);
        assertEquals(rule.getMainClass(), mainClass);
        assertEquals(rule.getEntrance(), entrance);
        assertEquals(rule.getParam(), param);
    }

    @Test
    void testParseGotoRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.GOTO)
                .addParameter(NAME, name)
                .addParameter(GOTO, goTo);

        Goto rule = (Goto) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getGoTo(), goTo);
    }

    @Test
    void testParseMatchGotoRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.MATCH_GOTO)
                .addParameter(NAME, name)
                .addParameter(TARGET, target)
                .addParameter(MATCH, match)
                .addParameter(REGEX, booleanTrue.toString())
                .addParameter(GOTO, goTo);

        MatchGoto rule = (MatchGoto) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getTarget(), target);
        assertEquals(rule.getMatch(), match);
        assertEquals(rule.isRegex(), booleanTrue);
        assertEquals(rule.getGoTo(), goTo);
    }

    @Test
    void testParseRemoveCodeRule() {
        String target = "com/blahblah\nnet/abc";

        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.REMOVE_CODE)
                .addParameter(NAME, name)
                .addParameter(TARGET, target);

        RemoveCode rule = (RemoveCode) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertTrue(rule.getTargets().contains("com/blahblah"));
        assertTrue(rule.getTargets().contains("net/abc"));
    }

    @Test
    void testParseRemoveCodeActionRule() {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.REMOVE_CODE_ACTION)
                .addParameter(NAME, name)
                .addParameter(ACTION, action);

        RemoveCodeAction rule = (RemoveCodeAction) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getAction() + " " + rule.getActionCount(), action);
    }

    @Test
    void testParseRemoveFilesRule() {
        String target = "smali/TestClass.smali\nsmali/TestClass2.smali";

        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.REMOVE_FILES)
                .addParameter(NAME, name)
                .addParameter(TARGET, target);

        RemoveFiles rule = (RemoveFiles) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertTrue(rule.getTargets().contains("smali/TestClass.smali"));
        assertTrue(rule.getTargets().contains("smali/TestClass2.smali"));
    }

    @Test
    void testParseReplaceRule() {
        testParseReplaceRule(replace);
    }

    @Test
    void testParseReplaceRuleWithEmptyReplacement() {
        testParseReplaceRule("");
    }

    @Test
    void testParseReplaceRuleWithEmptyLineReplacement() {
        testParseReplaceRule("\n");
    }

    private void testParseReplaceRule(String replacement) {
        RuleGenerator generator = new RuleGenerator()
                .addHeader(RuleGenerator.Headers.REPLACE)
                .addParameter(NAME, name)
                .addParameter(TARGET, target)
                .addParameter(MATCH, match)
                .addParameter(REGEX, booleanTrue.toString())
                .addParameter(REPLACE, replacement);

        Replace rule = (Replace) parseRule(generator.toString());
        assertEquals(rule.getName(), name);
        assertEquals(rule.getTarget(), target);
        assertEquals(rule.getMatch(), match);
        assertEquals(rule.isRegex(), booleanTrue);
        assertEquals(rule.getReplacement(), replacement);
    }
}
