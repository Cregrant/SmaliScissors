package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.rule.RuleParser;
import com.github.cregrant.smaliscissors.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

import static com.github.cregrant.smaliscissors.rule.RuleParser.*;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class ExecuteDex extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteDex.class);
    private final String script;
    private final String mainClass;
    private final String entrance;
    private final String param;

    public ExecuteDex(String rawString) {
        super(rawString);
        script = matchSingleLine(rawString, SCRIPT);
        mainClass = matchSingleLine(rawString, MAIN_CLASS);
        entrance = matchSingleLine(rawString, ENTRANCE);
        param = matchSingleLine(rawString, PARAM);
        smali = RuleParser.parseBoolean(rawString, SMALI_NEEDED);
    }

    @Override
    public boolean isValid() {
        return script != null && mainClass != null && entrance != null && param != null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        String apkPath = project.getApkPath();
        if (apkPath == null) {
            logger.warn("Apk file not found. Dex script may fail.");
        }
        String zipPath = patch.getFile().toString();
        String projectPath = project.getPath();
        patch.createTempDir();
        ArrayList<String> extracted = IO.extract(patch.getFile(), patch.getTempDir().getPath(), script);
        if (extracted.size() != 1) {
            logger.error("Dex script extract error.");
            return;
        }
        String dexPath = extracted.get(0);
        DexExecutor dexExecutor = project.getDexExecutor();
        if (dexExecutor != null) {
            dexExecutor.runDex(dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param, patch.getTempDir().getPath());
        } else {
            logger.error("Dex executor is not present.");
        }
        patch.deleteTempDir();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  EXECUTE_DEX.\n");
        if (name != null) {
            sb.append("Name:  ").append(name);
        }
        return sb.toString();
    }
}
