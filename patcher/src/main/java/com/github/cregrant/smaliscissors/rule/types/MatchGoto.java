package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.rule.RuleParser;
import com.github.cregrant.smaliscissors.util.Misc;
import com.github.cregrant.smaliscissors.util.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.rule.RuleParser.*;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class MatchGoto extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(MatchGoto.class);
    private final String target;
    private final String goTo;
    private final String originalMatch;
    private String match;
    private final boolean isRegex;
    private volatile boolean found;

    public MatchGoto(String rawString) {
        super(rawString);
        target = matchSingleLine(rawString, TARGET);
        originalMatch = matchSingleLine(rawString, MATCH);
        goTo = matchSingleLine(rawString, GOTO);
        isRegex = RuleParser.parseBoolean(rawString, REGEX);
        if (target == null) {
            targetType = TargetType.UNKNOWN;
        } else if (target.startsWith("smali") || target.endsWith("smali") || target.contains("[")) {  // '[' means static replacement like [APPLICATION]
            targetType = TargetType.SMALI;
        } else if (target.startsWith("res") || target.endsWith("xml")) {
            targetType = TargetType.XML;
        }

        match = originalMatch;
        boolean xml = targetType == TargetType.XML;
        if (isRegex) {
            match = xml ? fixRegexMatchXml(match) : fixRegexMatch(match);
        }
    }

    @Override
    public boolean isValid() {
        return target != null && match != null && goTo != null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        final Pattern matchPattern = Pattern.compile(patch.applyAssign(match));
        final Pattern targetPattern = Pattern.compile(Regex.globToRegex(target));

        List<DecompiledFile> providedFiles = project.applyTargetAssignments(target);
        List<DecompiledFile> files;
        if (!providedFiles.isEmpty()) {
            files = providedFiles;
        } else {
            files = getFilteredDecompiledFiles(project);
        }

        try {
            ArrayList<Future<?>> futures = new ArrayList<>(files.size());
            for (final DecompiledFile df : files) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (!found) {
                            if (!targetPattern.matcher(df.getPath()).matches()) {
                                return;
                            }

                            String body = df.getBody();
                            if (isRegex && Regex.matchSingleLine(body, matchPattern) != null || body.contains(match)) {
                                found = true;
                                logger.info("Match found!");
                            }
                        }
                    }
                };
                futures.add(project.getExecutor().submit(r));
            }
            project.getExecutor().waitForFinish(futures);
        } catch (Exception e) {
            logger.error("MatchGoto search failed", e);
        }
    }

    @Override
    public String nextRuleName() {
        if (found) {
            found = false;
            return goTo;
        }
        return null;
    }

    public String getTarget() {
        return target;
    }

    public String getGoTo() {
        return goTo;
    }

    public String getMatch() {
        return match;
    }

    public boolean isRegex() {
        return isRegex;
    }

    @Override

    public String toStringShort() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("(").append(name).append(") ");
        }
        sb.append("Jumping to the rule named ");
        sb.append("\n  (").append(goTo).append(")");
        sb.append("\nIF FOUND:");
        sb.append("\n  ").append(Misc.trimToSize(originalMatch, 35));
        sb.append("\nIN:");
        sb.append("\n  ").append(Misc.trimToSize(target, 35)).append('\n');
        return sb.toString();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:   MATCH_GOTO.\n");
        if (name != null) {
            sb.append("Name:   ").append(name).append('\n');
        }
        sb.append("Target: ").append(target).append("\n");
        sb.append("Match:  ").append(originalMatch).append('\n');
        sb.append("Goto:   ").append(goTo);
        return sb.toString();
    }
}
