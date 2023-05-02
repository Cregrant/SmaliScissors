package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;
import com.github.cregrant.smaliscissors.util.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static com.github.cregrant.smaliscissors.rule.RuleParser.TARGET;
import static com.github.cregrant.smaliscissors.util.Regex.matchMultiLines;

public class RemoveFiles extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(RemoveFiles.class);
    private final List<String> targets;

    public RemoveFiles(String rawString) {
        super(rawString);
        targets = matchMultiLines(rawString, TARGET, Regex.ResultFormat.SPLIT_TRIM);
    }

    public RemoveFiles(List<String> targets) {
        super("");
        this.targets = targets;
    }

    @Override
    public boolean isValid() {
        return targets != null && !targets.isEmpty();
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        HashSet<File> possibleEmptyFolders = new HashSet<>();
        int deletedCount = 0;

        for (String target : targets) {
            List<String> removed = project.removeLoadedFile(project, target);
            deletedCount += removed.size();
            for (String str : removed) {
                File file = new File(project.getPath() + File.separator + str);
                IO.delete(file);
                possibleEmptyFolders.add(file.getParentFile());
                logger.debug("{} deleted", file);
            }
        }

        for (File file : possibleEmptyFolders) {
            String[] subs = file.list();
            while (subs != null && subs.length == 0) {
                file.delete();
                file = file.getParentFile();     //delete parent folder if it is empty
                subs = file.list();
            }
        }
        logger.info(deletedCount + " files deleted");
    }

    public List<String> getTargets() {
        return targets;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_FILES.\n");
        if (name != null) {
            sb.append("Name: ").append(name).append('\n');
        }
        sb.append("Targets:\n");
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            sb.append("    ").append(target).append("\n");
            if (i >= 30 && logger.isDebugEnabled()) {
                sb.append("    ... + ").append(targets.size() - i - 1).append(" more lines\n");
                break;
            }
        }
        return sb.toString();
    }
}
