package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.common.DecompiledFile;
import com.github.cregrant.smaliscissors.utils.Regex;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Replace implements IRule {
    public String name;
    public String target;
    public String match;
    public String replacement;
    public boolean isRegex;
    public boolean isSmali;
    public boolean isXml;
    public ArrayList<IRule> mergedRules;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return target != null && match != null && replacement != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali;
    }

    @Override
    public boolean xmlNeeded() {
        return isXml;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        AtomicInteger patchedFilesNum = new AtomicInteger();
        Pattern targetCompiled = Pattern.compile(Regex.globToRegex(target));
        Pattern localMatchCompiled = Pattern.compile(patch.applyAssign(match));
        String localReplacement = patch.applyAssign(replacement);
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());

        for (DecompiledFile dFile : files) {
            Runnable r = () -> {
                try {
                    boolean replaced = replace(dFile, targetCompiled, localMatchCompiled, localReplacement);
                    if (replaced) {
                        if (Prefs.logLevel == Prefs.Log.DEBUG)
                            Main.out.println(dFile.getPath() + " patched.");
                        patchedFilesNum.getAndIncrement();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            };
            futures.add(BackgroundWorker.submit(r));
        }
        BackgroundWorker.compute(futures);

        if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
            if (isSmali)
                Main.out.println(patchedFilesNum + " smali files patched.");
            else
                Main.out.println(patchedFilesNum + " xml files patched.");
        }
    }

    private boolean replace(DecompiledFile dFile, Pattern targetCompiled, Pattern match, String replacement) {
        boolean replaced = false;
        if (!targetCompiled.matcher(dFile.getPath()).matches())
            return false;
        String smaliBody = dFile.getBody();
        String newBody;
        if (isRegex)
            newBody = Regex.replaceAll(smaliBody, match, replacement);
        else
            newBody = smaliBody.replace(match.pattern(), replacement);

        if (!smaliBody.equals(newBody)) {
            replaced = true;
            dFile.setBody(newBody);
        }
        return replaced;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    MATCH_REPLACE.\n");
        if (name != null)
            sb.append("Name:  ").append(name).append('\n');
        sb.append("Target:  ").append(target).append("\n");
        sb.append("Match:   ").append(match).append('\n');
        sb.append("Regex:   ").append(isRegex).append('\n');
        sb.append("Replace: ").append(replacement);
        return sb.toString();
    }
}
