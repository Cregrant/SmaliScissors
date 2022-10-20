package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.util.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Replace implements Rule {
    public ArrayList<Rule> mergedRules;
    private String name;
    private String target;
    private String match;
    private String replacement;
    private boolean isRegex;
    private boolean isSmali;
    private boolean isXml;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getTarget() != null && getMatch() != null && getReplacement() != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali();
    }

    @Override
    public boolean xmlNeeded() {
        return isXml();
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        List<? extends DecompiledFile> files;
        if (isSmali()) {
            files = project.getSmaliList();
        } else if (isXml()) {
            files = project.getXmlList();
        } else {
            throw new IllegalStateException("Not smali nor xml rule.");
        }

        final AtomicInteger patchedFilesNum = new AtomicInteger();
        final Pattern targetCompiled = Pattern.compile(Regex.globToRegex(getTarget()));
        final Pattern localMatchCompiled = Pattern.compile(patch.applyAssign(getMatch()));
        final String localReplacement = patch.applyAssign(getReplacement());
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());

        for (final DecompiledFile dFile : files) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean replaced = Replace.this.replace(dFile, targetCompiled, localMatchCompiled, localReplacement);
                        if (replaced) {
                            if (Prefs.logLevel == Prefs.Log.DEBUG) {
                                Main.out.println(dFile.getPath() + " patched.");
                            }
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

        if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
            if (isSmali()) {
                Main.out.println(patchedFilesNum + " smali files patched.");
            } else {
                Main.out.println(patchedFilesNum + " xml files patched.");
            }
        }
    }

    private boolean replace(DecompiledFile dFile, Pattern targetCompiled, Pattern match, String replacement) {
        boolean replaced = false;
        if (!targetCompiled.matcher(dFile.getPath()).matches()) {
            return false;
        }
        String smaliBody = dFile.getBody();
        String newBody;
        if (isRegex()) {
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
        return isXml() == replace.isXml()
                && isSmali() == replace.isSmali()
                && getTarget().equals(replace.getTarget())
                && getReplacement().equals(replace.getReplacement());
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public void setRegex(boolean regex) {
        isRegex = regex;
    }

    public boolean isSmali() {
        return isSmali;
    }

    public void setSmali(boolean smali) {
        isSmali = smali;
    }

    public boolean isXml() {
        return isXml;
    }

    public void setXml(boolean xml) {
        isXml = xml;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    MATCH_REPLACE.\n");
        if (getName() != null) {
            sb.append("Name:  ").append(name).append('\n');
        }
        sb.append("Target:  ").append(target).append("\n");
        sb.append("Match:   ").append(match).append('\n');
        sb.append("Regex:   ").append(isRegex).append('\n');
        sb.append("Replace: ").append(replacement);
        return sb.toString();
    }
}
