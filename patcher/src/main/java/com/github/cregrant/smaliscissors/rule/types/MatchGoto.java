package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.rule.RuleParser;
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
    private final String match;
    private final boolean isRegex;
    private volatile boolean found;

    public MatchGoto(String rawString) {
        super(rawString);
        target = matchSingleLine(rawString, TARGET);
        String match = matchSingleLine(rawString, MATCH);
        goTo = matchSingleLine(rawString, GOTO);
        isRegex = RuleParser.parseBoolean(rawString, REGEX);
        if (target != null) {
            smali = target.endsWith("smali");
            xml = target.endsWith("xml");
        }

        if (isRegex) {
            this.match = xml ? fixRegexMatchXml(match) : fixRegexMatch(match);
        } else {
            this.match = match;
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

        List<DecompiledFile> files = new ArrayList<>();
        if (smali) {
            files.addAll(project.getSmaliList());
        } else if (xml) {
            files.addAll(project.getXmlList());
        } else {
            files.addAll(project.getSmaliList());
            files.addAll(project.getXmlList());
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:   MATCH_GOTO.\n");
        if (name != null) {
            sb.append("Name:   ").append(name).append('\n');
        }
        sb.append("Target: ").append(target).append("\n");
        sb.append("Match:  ").append(match).append('\n');
        sb.append("Goto:   ").append(goTo);
        return sb.toString();
    }
}
