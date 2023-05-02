package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.rule.RuleParser;
import com.github.cregrant.smaliscissors.util.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.rule.RuleParser.*;
import static com.github.cregrant.smaliscissors.util.Regex.matchMultiLines;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class Assign extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(Assign.class);
    private final String match;
    private final String target;
    private final boolean isRegex;
    private final Map<String, String> assignments;

    public Assign(String rawString) throws InputMismatchException {
        super(rawString);
        String match = matchSingleLine(rawString, MATCH);
        target = matchSingleLine(rawString, TARGET);
        assignments = parseAssignments(matchMultiLines(rawString, ASSIGNMENT, Regex.ResultFormat.SPLIT));
        isRegex = RuleParser.parseBoolean(rawString, REGEX);
        if (!isRegex) {
            throw new InputMismatchException("REGEX field must be true");
        }
        if (target != null) {
            smali = target.endsWith("smali");
            xml = target.endsWith("xml");
            if (target.contains("[")) {  //static replacement like [APPLICATION]
                smali = true;
            }
        }
        this.match = xml ? RuleParser.fixRegexMatchXml(match) : fixRegexMatch(match);
    }

    static HashMap<String, String> parseAssignments(ArrayList<String> assignmentsList) {
        HashMap<String, String> result = new HashMap<>();
        for (String string : assignmentsList) {
            int firstCharPos = 0;
            while (string.charAt(firstCharPos) == ' ') {
                firstCharPos++;
            }
            int dividerPos = string.indexOf('=');
            String key = string.substring(firstCharPos, dividerPos);

            int endCharPos = string.length() - 1;
            while (string.charAt(endCharPos) == ' ') {
                endCharPos--;
            }
            String trimmedValue = string.substring(dividerPos + 1, endCharPos + 1);
            result.put(key, trimmedValue);
        }
        return result;
    }

    @Override
    public boolean isValid() {
        return target != null && match != null && assignments != null && !assignments.isEmpty() && (smali || xml);
    }

    @Override
    public void apply(Project project, Patch patch) {
        String localTarget = target;
        List<? extends DecompiledFile> acceptedFiles = project.applyTargetAssignments(target);
        List<? extends DecompiledFile> files;

        if (!acceptedFiles.isEmpty()) {
            files = acceptedFiles;
            localTarget = "**";
        } else if (smali) {
            files = project.getSmaliList();
        } else if (xml) {
            files = project.getXmlList();
        } else {
            throw new IllegalStateException("Not smali nor xml rule.");
        }

        Pattern targetCompiled = Pattern.compile(Regex.globToRegex(localTarget));
        for (DecompiledFile dFile : files) {
            if (!targetCompiled.matcher(dFile.getPath()).matches()) {
                continue;
            }

            Matcher matcher = Pattern.compile(patch.applyAssign(match)).matcher(dFile.getBody());
            if (matcher.find()) {
                ArrayList<Map.Entry<String, String>> unprocessedAssignments = new ArrayList<>(assignments.entrySet());
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    String groupText = "${GROUP" + i + "}";
                    for (Map.Entry<String, String> entry : unprocessedAssignments) {
                        entry.setValue(entry.getValue().replace(groupText, matcher.group(i)));
                    }
                }

                for (Map.Entry<String, String> entry : unprocessedAssignments) {
                    patch.addAssignment(entry.getKey(), entry.getValue());
                    logger.info("Assigned \"{}\" to \"{}\"", minifyLongString(entry.getValue()), entry.getKey());
                }
                return;
            }
        }
    }

    static String minifyLongString(String str) {
        if (str.length() > 300) {
            return str.substring(0, 60) + " ... " + str.substring(str.length() - 60);
        }
        return str;
    }

    public String getTarget() {
        return target;
    }

    public String getMatch() {
        return match;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public Map<String, String> getAssignments() {
        return assignments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  MATCH_ASSIGN\n");
        if (name != null) {
            sb.append("Name:  ").append(name).append('\n');
        }
        sb.append("Target: ").append(target).append('\n');
        sb.append("Match: ").append(match).append('\n');
        sb.append("Regex: ").append(isRegex).append('\n');
        sb.append("Assignments:\n");
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            sb.append("    ").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return sb.toString();
    }
}
