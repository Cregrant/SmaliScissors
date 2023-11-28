package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.SmaliGenerator;
import com.github.cregrant.smaliscissors.rule.RuleParser;
import com.github.cregrant.smaliscissors.util.IO;
import com.github.cregrant.smaliscissors.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;

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
        targetType = RuleParser.parseBoolean(rawString, SMALI_NEEDED) ? TargetType.SMALI : TargetType.UNKNOWN;
    }

    @Override
    public boolean isValid() {
        return script != null && mainClass != null && entrance != null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        String apkPath = project.getApkPath();
        if (apkPath == null) {
            logger.warn("Apk file not found. The dex script may fail.");
        }
        if (targetType == TargetType.SMALI && project.getSmaliList().isEmpty()) {
            generateSmaliFiles(project);
        }
        String zipPath = patch.getFile().toString();
        String projectPath = project.getPath();
        patch.createTempDir();
        ArrayList<String> extracted = IO.extract(patch.getFile(), patch.getTempDir().getPath(), script);
        if (extracted.size() != 1) {
            throw new IOException("Dex script extract error.");
        }
        String dexPath = extracted.get(0);
        DexExecutor dexExecutor = project.getDexExecutor();
        if (dexExecutor != null) {
            try {
                dexExecutor.runDex(dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param, patch.getTempDir().getPath());
            } catch (Throwable e) {
                logger.error("Error executing the dex script\n");
                if (e instanceof NoClassDefFoundError || e.getCause() instanceof NoClassDefFoundError) {
                    logger.debug("", e);
                    throw new InputMismatchException("This dex script supports only the Android platform!\n");
                }
                throw new RuntimeException(e);
            }
        } else {
            throw new IOException("The dex executor is not present. Contact your app developer.");
        }
        patch.deleteTempDir();
    }

    private void generateSmaliFiles(Project project) throws FileNotFoundException {
        logger.info("Smali files are not found! Generating smali files...");
        SmaliGenerator smaliGenerator = project.getSmaliGenerator();
        if (smaliGenerator != null) {
            smaliGenerator.generateSmaliFiles(project.getPath().replace('\\', '/'));
            project.rescanSmali();
            if (project.getSmaliList().isEmpty()) {
                throw new FileNotFoundException("Smali files were not generated");
            }
        } else {
            throw new FileNotFoundException("Smali file generation canceled: no smali generator provided.");
        }

    }

    public String getScript() {
        return script;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getEntrance() {
        return entrance;
    }

    public String getParam() {
        return param;
    }

    @Override

    public String toStringShort() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("(").append(name).append(") ");
        }
        sb.append("Executing dex script");
        sb.append("\n  ").append(Misc.trimToSize(script, 35)).append('\n');
        return sb.toString();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  EXECUTE_DEX.\n");
        if (name != null) {
            sb.append("Name:      ").append(name);
        }
        sb.append("Script:    ").append(script).append('\n');
        sb.append("MainClass: ").append(mainClass).append('\n');
        sb.append("Entrance:  ").append(entrance).append('\n');
        sb.append("Param:     ").append(param).append('\n');
        sb.append("Smali:     ").append(targetType == TargetType.SMALI).append('\n');
        return sb.toString();
    }
}
