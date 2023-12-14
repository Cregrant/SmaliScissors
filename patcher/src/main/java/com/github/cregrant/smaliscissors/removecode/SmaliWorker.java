package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.RemoveFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SmaliWorker {

    private static final Logger logger = LoggerFactory.getLogger(SmaliWorker.class);
    private final Project project;
    private final Patch patch;
    private final RemoveCode rule;

    public SmaliWorker(Project project, Patch patch, RemoveCode rule) {
        this.project = project;
        this.patch = patch;
        this.rule = rule;
    }

    public void run() {
        State state;
        if (Flags.SMALI_DEBUG_BENCHMARK) {
            state = infiniteApplyRule();
        } else {
            state = applyRule();
        }

        if (!Flags.SMALI_DEBUG_DO_NOT_WRITE) {
            writeChanges(state);
        }

        rule.removePendingActions(project);
    }

    private State infiniteApplyRule() {
        int bestLoopTime = Integer.MAX_VALUE;
        while (true) {
            long l = System.currentTimeMillis();

            applyRule();

            int loopTime = (int) (System.currentTimeMillis() - l);
            bestLoopTime = Math.min(loopTime, bestLoopTime);
            logger.info("\rLoop takes " + loopTime + " ms (" + bestLoopTime + " ms best)\n");
        }
    }

    private State applyRule() {
        int errorsNum = 0;
        int patchedNum = 0;
        int crashReportersNum = 0;

        State currentState = new State(project.getSmaliList());
        State newState = new State(currentState);
        ClassesPool pool = new ClassesPool(project);
        TargetController controller = new TargetController();
        project.getSmaliKeeper().changeTargets(patch, rule);
        List<String> targets = rule.getTargets();

        for (int i = controller.getStartNumber(targets); i < targets.size(); i++) {
            String path = targets.get(i);
            SmaliTarget target = new SmaliTarget().setSkipPath(path);

            SmaliRemoveJob job = new SmaliRemoveJob(project, pool, patch, rule);

            if (controller.canSkip() && !new SmaliFilter(project, pool, newState).separateDoNotChangeState(target).isEmpty()) {
                if (controller.skipAndCheckEnd(target)) {
                    rule.setLastTarget(project, path);
                    break;
                }
                continue;
            }

            try {
                job.remove(target, newState);
            } catch (Exception e) {
                errorsNum++;
                logger.warn("Skipped " + target + " (" + e.getMessage() + ")");
                newState = new State(currentState);
                if (controller.canApply() && controller.applyAndCheckEnd(target)) {
                    rule.setLastTarget(project, path);
                    break;
                }
                continue;
            }

            if (job.isStateModified()) {
                currentState = newState;
                newState = new State(currentState);

                patchedNum++;
                logger.info("Removed " + target);
                String result = currentState.files.size() + " kept, " + currentState.patchedClasses.size() + " patched, " + currentState.deletedFiles.size() + " deleted, " + currentState.removedTargets.size() + " refs removed";
                if (Flags.SMALI_DEBUG_DO_NOT_WRITE) {
                    logger.info(result);
                } else {
                    logger.debug(result);
                }

                if (controller.canApply() && controller.applyAndCheckEnd(target)) {
                    rule.setLastTarget(project, path);
                    break;
                }
                if (rule.isInternal()) {
                    crashReportersNum++;
                }
            }
        }
        project.getSmaliKeeper().keepClasses(currentState);

        if (rule.isInternal()) {
            logger.info(crashReportersNum + " crash reporters deleted.");
        } else {
            logger.info(patchedNum + " targets patched and " + errorsNum + " failed.");
        }
        return currentState;
    }

    private void writeChanges(State state) {
        ArrayList<String> deletedList = new ArrayList<>(state.removedTargets.size());
        if (!state.deletedFiles.isEmpty()) {
            for (SmaliFile file : state.deletedFiles) {
                deletedList.add(file.getPath());
            }

            RemoveFiles removeFiles = new RemoveFiles(deletedList);
            try {
                removeFiles.apply(project, patch);
            } catch (IOException e) {
                logger.error("Failed to delete files", e);
            }
        }

        if (!rule.isInternal()) {
            logger.debug("Writing {} classes", state.patchedClasses.size());
        }
        for (SmaliClass smaliClass : state.patchedClasses) {
            smaliClass.getFile().setBody(smaliClass.getNewBody());
        }
    }

    private class TargetController {
        int skipCount;
        int applyCount;

        public TargetController() {
            skipCount = rule.getSkipCount(project);
            applyCount = rule.getApplyCount(project);
        }

        int getStartNumber(List<String> targets) {
            if (skipCount > 0 || applyCount > 0) {
                return Math.min(rule.getLastTargetIndex(project) + 1, targets.size());
            }
            return 0;
        }

        boolean canSkip() {
            return skipCount > 0 && !rule.isInternal();
        }

        boolean skipAndCheckEnd(SmaliTarget target) {
            skipCount--;
            if (skipCount == 0) {
                logger.info("Skipped " + target + " as set by REMOVE_CODE_ACTION");
                return true;
            } else {
                return false;
            }
        }

        boolean canApply() {
            return applyCount > 0 && !rule.isInternal();
        }

        boolean applyAndCheckEnd(SmaliTarget target) {
            applyCount--;
            if (applyCount == 0) {
                logger.info("Patch stopped after " + target + " as set by REMOVE_CODE_ACTION");
                return true;
            } else {
                return false;
            }
        }
    }
}
