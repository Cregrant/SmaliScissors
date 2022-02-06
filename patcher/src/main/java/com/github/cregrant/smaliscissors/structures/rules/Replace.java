package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.DecompiledFile;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Replace implements IRule {
    public String name;
    public String match;
    public String replacement;
    public boolean isRegex = false;
    public boolean isSmali = false;
    public boolean isXml = false;
    public ArrayList<String> targets;
    public ArrayList<IRule> mergedRules;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null && !targets.isEmpty() && match != null && replacement != null;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public boolean canBeMerged(IRule otherRule) {
        return false;   //todo
    }

    @Override
    public void apply(Project project, Patch patch) {   //fixme assign in match and replacement
        ArrayList<Pattern> patterns = new ArrayList<>(targets.size());
        for (String target : targets)
            patterns.add(Pattern.compile(Regex.globToRegex(target)));

        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        AtomicInteger patchedFilesNum = new AtomicInteger();
        String localMatch = patch.applyAssign(match);
        String localReplacement = patch.applyAssign(replacement);
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());
        for (DecompiledFile dFile : files) {
            Runnable r = () -> {
                try {
                    if (isRegex)
                        replaceRegex(dFile, patterns, Pattern.compile(localMatch), localReplacement);
                    else
                        replace(dFile, patterns, localMatch, localReplacement);
                    if (dFile.isModified()) {
                        dFile.setModified(false);
                        if (Prefs.verbose_level == 0)
                            Main.out.println(dFile.getPath() + " patched.");
                        patchedFilesNum.getAndIncrement();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            };
            futures.add(BackgroundWorker.executor.submit(r));
        }
        BackgroundWorker.compute(futures);

        if (Prefs.verbose_level <= 2) {
            if (isSmali)
                Main.out.println(patchedFilesNum + " smali files patched.");
            else
                Main.out.println(patchedFilesNum + " xml files patched.");
        }
    }

    private void replaceRegex(DecompiledFile dFile, ArrayList<Pattern> patterns, Pattern match, String replacement) {
        for (Pattern pattern : patterns) {
            if (!pattern.matcher(dFile.getPath()).matches())
                return;

            String smaliBody = dFile.getBody();
            String newBody = Regex.replaceAll(smaliBody, replacement, match.matcher(smaliBody));

            if (!smaliBody.equals(newBody)) {
                dFile.setModified(true);
                dFile.setBody(newBody);
            }
        }
    }

    private void replace(DecompiledFile dFile, ArrayList<Pattern> patterns, String match, String replacement) {
        for (Pattern pattern : patterns) {
            if (!pattern.matcher(dFile.getPath()).matches())
                return;

            String smaliBody = dFile.getBody();
            String newBody = smaliBody.replace(match, replacement);
            if (!smaliBody.equals(newBody)) {
                dFile.setModified(true);
                dFile.setBody(newBody);
            }
        }
    }

/*    ArrayList<Rule> mergedRules = new ArrayList<>();
        mergedRules.add(replaceRule);
        if (!replaceRule.mergedRules.isEmpty())
            mergedRules.addAll(replaceRule.mergedRules);

        ArrayList<Batch> batchLoad = new ArrayList<>(mergedRules.size());
        Pattern path = Pattern.compile(Regex.globToRegex(mergedRules.get(0).targets.get(0)));
        for (Rule rule : mergedRules) {
            applyAssign(rule);
            Batch batch = new Batch();
            batch.pattern = Pattern.compile(patch.applyAssign(rule.match));
            batch.replacement = patch.applyAssign(rule.replacement);
            batch.isRegex = rule.isRegex;
            batchLoad.add(batch);
        }

        patchedFilesNum = 0;
        Object lock = new Object();
        ArrayList<DecompiledFile> files = replaceRule.isXml ? Scanner.xmlList : Scanner.smaliList;
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());
        for (DecompiledFile dFile : files) {
            Runnable r = () -> {
                try {
                    replace(dFile, path, batchLoad);
                    if (dFile.isModified()) {
                        dFile.setModified(false);
                        if (Prefs.verbose_level == 0)
                            Main.out.println(dFile.getPath() + " patched.");
                        synchronized (lock) {
                            patchedFilesNum++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }

            };
            futures.add(BackgroundWorker.executor.submit(r));
        }
        BackgroundWorker.compute(futures);

        if (Prefs.verbose_level <= 2) {
            if (replaceRule.isSmali)
                Main.out.println(patchedFilesNum + " smali files patched.");
            else
                Main.out.println(patchedFilesNum + " xml files patched.");
        }
    }

    private static void replace(DecompiledFile dFile, Pattern pattern, ArrayList<Batch> batchLoad) {
        if (pattern.matcher(dFile.getPath()).matches()) {
            String smaliBody = dFile.getBody();
            int oldHashcode = smaliBody.hashCode();

            for (Batch batch : batchLoad) {
                if (batch.match.get() == null)
                    batch.match.set(batch.pattern.matcher(""));

                if (batch.isRegex) {
                    smaliBody = Regex.replaceAll(smaliBody, batch.replacement, batch.match.get());
                } else
                    smaliBody = smaliBody.replace(batch.pattern.pattern(), batch.replacement);
            }

            if (smaliBody.hashCode() != oldHashcode) {
                dFile.setModified(true);
                dFile.setBody(smaliBody);
            }
        }
    }*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    MATCH_REPLACE.\n");
        if (name != null)
            sb.append("Name:  ").append(name).append('\n');
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");

        sb.append("Regex:   ").append(isRegex).append('\n');
        sb.append("Replacement: ").append(replacement).append('\n');
        return sb.toString();
    }


    static class Batch {
        ThreadLocal<Matcher> match = new ThreadLocal<>();
        Pattern pattern;
        String replacement;
        boolean isRegex;
    }
}
