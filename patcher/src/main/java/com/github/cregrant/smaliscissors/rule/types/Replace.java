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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.github.cregrant.smaliscissors.rule.RuleParser.*;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class Replace extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(Replace.class);
    public ArrayList<Rule> mergedRules;
    private String target;
    private String match;
    private String replacement;
    private boolean regex;

    public Replace(String rawString) {
        super(rawString);
        target = matchSingleLine(rawString, TARGET);
        match = matchSingleLine(rawString, MATCH);
        String replacement = matchSingleLine(rawString, REPLACEMENT);
        // a fix for regexp
        if (replacement != null && replacement.length() > 1) {
            this.replacement = replacement.substring(0, replacement.length() - 1);
        } else {
            this.replacement = replacement;
        }
        regex = RuleParser.parseBoolean(rawString, REGEX);
        if (target != null) {
            smali = target.endsWith("smali");
            xml = target.endsWith("xml");
            if (target.contains("[")) {
                smali = true;
            }
        }

        if (regex) {
            match = xml ? fixRegexMatchXml(match) : fixRegexMatch(match);
            replacement = xml ? replacement : fixRegexReplacement(replacement);
        }
        if (replacement != null && xml) {
            replacement = replacement.replace("><", ">\n<");
        }
    }

    public Replace() {
        super("");
    }

    @Override
    public boolean isValid() {
        return target != null && match != null && replacement != null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        String localTarget = target;
        List<DecompiledFile> providedFiles = project.applyTargetAssignments(target);
        List<DecompiledFile> files = new ArrayList<>();

        if (!providedFiles.isEmpty()) {
            files = providedFiles;
            localTarget = "**";
        } else if (smali) {
            files.addAll(project.getSmaliList());
        } else if (xml) {
            files.addAll(project.getXmlList());
        } else {
            files.addAll(project.getSmaliList());
            files.addAll(project.getXmlList());
        }

        final AtomicInteger patchedFilesNum = new AtomicInteger();
        final Pattern targetCompiled = Pattern.compile(Regex.globToRegex(localTarget));
        final Pattern localMatchCompiled = Pattern.compile(patch.applyAssign(match));
        final String localReplacement = patch.applyAssign(replacement);
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());

        for (final DecompiledFile dFile : files) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean replaced = Replace.this.replace(dFile, targetCompiled, localMatchCompiled, localReplacement);
                        if (replaced) {
                            logger.debug("{} patched", dFile.getPath());
                            patchedFilesNum.getAndIncrement();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().waitForFinish(futures);
        if (smali) {
            logger.info(patchedFilesNum + " smali files patched.");
        } else {
            logger.info(patchedFilesNum + " xml files patched.");
        }
    }

    private boolean replace(DecompiledFile dFile, Pattern targetCompiled, Pattern match, String replacement) {
        boolean replaced = false;
        if (!targetCompiled.matcher(dFile.getPath()).matches()) {
            return false;
        }
        String smaliBody = dFile.getBody();
        String newBody;
        if (regex) {
            newBody = Regex.replaceAll(smaliBody, match, replacement);
        } else {
            newBody = smaliBody.replace(match.pattern(), replacement);
        }

        if (!smaliBody.equals(newBody)) {
            replaced = true;
            dFile.setBody(newBody);
        }
        return replaced;
    }

    private boolean canBeMerged(Rule rule) {
        if (!(rule instanceof Replace)) {
            return false;
        }
        Replace replace = ((Replace) rule);
        return xml == replace.xml
                && smali == replace.smali
                && target.equals(replace.target)
                && replacement.equals(replace.replacement);
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public String getTarget() {
        return target;
    }

    public String getMatch() {
        return match;
    }

    public String getReplacement() {
        return replacement;
    }

    public boolean isRegex() {
        return regex;
    }

    public void setRegex(boolean regex) {
        this.regex = regex;
    }

    public boolean isSmali() {
        return smali;
    }

    public void setSmali(boolean smali) {
        this.smali = smali;
    }

    public boolean isXml() {
        return xml;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    MATCH_REPLACE.\n");
        if (name != null) {
            sb.append("Name:    ").append(name).append('\n');
        }
        sb.append("Target:  ").append(target).append("\n");
        sb.append("Match:   ").append(match).append('\n');
        sb.append("Regex:   ").append(regex).append('\n');
        String verboseReplacement;
        if (replacement.equals("")) {
            verboseReplacement = "'none' (this means delete matched result)";
        } else if (replacement.equals("\n")) {
            verboseReplacement = "'\\n' (the new line character)";
        } else {
            verboseReplacement = replacement;
        }
        sb.append("Replace: ").append(verboseReplacement).append('\n');
        return sb.toString();
    }
}
