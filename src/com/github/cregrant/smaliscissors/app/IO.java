package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.out;

@SuppressWarnings("ResultOfMethodCallIgnored")
class IO {

    static String currentProjectPathCached = "";

    ArrayList<String> loadRules(File patchesDir, String zipName, Patch patch) {
        Pattern patRule = null;
        if (Prefs.rules_AEmode == 1) {
            patRule = Pattern.compile("(\\[.+?](?:\\RSOURCE:\\R.++)?\\RTARGET:\\R[\\s\\S]+?\\[/.+?])", Pattern.UNIX_LINES);
        }
        out.println("Loading rules...");
        new IO().deleteAll(new File(new CompatibilityData().getPatchesDir() + File.separator + "temp"));
        File tempFolder = new File(patchesDir + File.separator + "temp");
        if (!tempFolder.exists()) {
            tempFolder.mkdir();
        }
        String txtFile = tempFolder + File.separator + "patch.txt";
        zipExtract(patchesDir + File.separator + zipName, tempFolder.toString());
        if (!new File(txtFile).exists()) {
            out.println("No patch.txt file in patch!");
            System.exit(1);
        }

        if (Objects.requireNonNull(tempFolder.list()).length == 0) {
            if (Prefs.arch_device.equals("android")) {
                out.println("Put patches in /ApkEditor/patches!");
            } else {
                out.println("Can`t load patches");
            }
            System.exit(1);
        }
        ArrayList<String> rulesListArr = new Regex().matchMultiLines(Objects.requireNonNull(patRule), read(txtFile), "rules");
        RuleParser parser = new RuleParser();
        for (String rule : rulesListArr) patch.addRule(parser.parseRule(rule));

        out.println(rulesListArr.size() + " rules found\n");
        
        //todo delete return
        return rulesListArr;
    }

    String read(String path) {
        final FileInputStream is;
        String resultString = null;
        try {
            is = new FileInputStream(path);
            byte[] buffer = new byte[is.available()];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            is.read(buffer);
            os.write(buffer);
            resultString = os.toString();
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            out.println("Exiting to prevent file corruption");
            System.exit(1);
        }
        return resultString;
    }

    void write(String path, String content) {
        try {
            this.deleteAll(new File(path));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeChangesInSmali() {
        for (int j = 0; j < ProcessRule.smaliList.size(); ++j) {
            Smali tmpSmali = ProcessRule.smaliList.get(j);
            if (tmpSmali.isNotModified()) continue;
            tmpSmali.setModified(false);
            write(tmpSmali.getPath(), tmpSmali.getBody());
        }
    }

    void copy(String src, String dst) {
        src.strip(); dst.strip();
        File dstFolder = new File(dst).getParentFile();
        if (!Objects.requireNonNull(dstFolder).exists()) {
            dstFolder.mkdirs();
        }
        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)){
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            os.write(buffer);
            os.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
            out.println("Hmm.. error during copying file...");
        }
    }

    void zipExtract(String src, String dst){
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                File filePath = mergePath(dst, zipEntry.getName().replace('/', '\\'));  //fix path with merge
                if (!zipEntry.isDirectory()) {
                    new File(filePath.getParent()).mkdirs();
                    FileOutputStream fout = new FileOutputStream(filePath);
                    int len;
                    byte[] buffer = new byte[65536];
                    while ((len = zip.read(buffer)) != -1) fout.write(buffer, 0, len);
                    fout.flush();
                    fout.close();
                }
                else filePath.mkdirs();
                zip.closeEntry();
            }
        }catch (FileNotFoundException e) {
            out.println("File not found!");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
        catch (IOException e) {
            out.println("Error during extracting zip file.");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
    }

    File mergePath(String dstFolder, String toMerge) {
        ArrayList<String> pathTree = new ArrayList<>();
        File resultFile = new File(dstFolder + File.separator + toMerge);
        File parent;
        StringBuilder sb = new StringBuilder();
        Regex tmp = new Regex();
        String dstEnd = tmp.getEndOfPath(dstFolder);
        pathTree.add(tmp.getEndOfPath(toMerge));
        File fileToMerge = new File(toMerge);
        while ((parent = fileToMerge.getParentFile()) != null) {   //get rid of wrong paths
            if (parent.getName().equals(dstEnd)) {
                sb.append(dstFolder);
                for (int k=pathTree.size(); k>0;) {
                    k--;
                    sb.append(File.separator).append(pathTree.get(k));
                }
                resultFile = new File(sb.toString());
                break;
            } else {
                pathTree.add(tmp.getEndOfPath(parent.toString()));
                fileToMerge = parent;
            }
        }
        return resultFile;
    }

    void deleteAll(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) {
                file.delete();
            } else {
                for (String child : Objects.requireNonNull(file.list())) {
                    deleteAll(new File(file, child));
                }
                if (Objects.requireNonNull(file.list()).length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }

    void checkIfScanned(String currentProjectPath) {
        if (!currentProjectPath.equals(currentProjectPathCached)) {
            ProcessRule.smaliList.clear();
            out.println("\nScanning " + currentProjectPath);
            scan(currentProjectPath);
            currentProjectPathCached = currentProjectPath;
        }
    }

    private static CountDownLatch cdl;
    private static final Object lock = new Object();

    private void scan(String projectPath) {
        long startTime = System.currentTimeMillis();
        ArrayList<String> smFolders = new ArrayList<>();
        for (String i : Objects.requireNonNull(new File(projectPath).list())) {
            if (!i.startsWith("smali")) continue;
            smFolders.add(i);
        }
        if (smFolders.isEmpty()) {
            out.println("No smali folders found inside the project folder \"" + projectPath.replaceAll(".+\\\\", "") + "\"");
            System.exit(1);
        }
        cdl = new CountDownLatch(smFolders.size());
        for (String folder : smFolders) {
            new Thread(() -> {
                Stack<File> stack = new Stack<>();
                stack.push(new File(projectPath + File.separator + folder));
                while (!stack.isEmpty()) {
                    for (File file : Objects.requireNonNull(stack.pop().listFiles())) {
                        if (file.isDirectory()) {
                            stack.push(file);
                            continue;
                        }
                        Smali tmp = new Smali();
                        tmp.setPath(file.toString());
                        synchronized (lock) {
                            ProcessRule.smaliList.add(tmp);
                        }
                    }
                }
                cdl.countDown();
            }).start();
        }
        try {
            cdl.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        AtomicInteger currentNum = new AtomicInteger(ProcessRule.smaliList.size() - 1);
        int thrNum = Prefs.max_thread_num;
        cdl = new CountDownLatch(thrNum);
        for (int curThread = 1; curThread <= thrNum; ++curThread) {
            new Thread(() -> {
                int num;
                while ((num = currentNum.getAndDecrement()) >= 0) {
                    Smali tmpSmali = ProcessRule.smaliList.get(num);
                    for (int S =0; S<10; S++){
                        tmpSmali.setBody(read(tmpSmali.getPath()));
                    }
                }
                cdl.countDown();
            }).start();
        }
        try {
            cdl.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (Prefs.bigMemoryDevice) {
            int totalSmaliNum = ProcessRule.smaliList.size() - 1;
            ArrayList<Integer> positionArr = new ArrayList<>(totalSmaliNum);
            ArrayList<Integer> lengthArr = new ArrayList<>(totalSmaliNum);
            for (int i = 0; i < totalSmaliNum; ++i) {
                int len = ProcessRule.smaliList.get(i).getBody().length();
                boolean isAdded = false;
                for (int k = 0; k < positionArr.size(); ++k) {
                    if (len <= lengthArr.get(k)) continue;
                    lengthArr.add(k, len);
                    positionArr.add(k, i);
                    isAdded = true;
                    break;
                }
                if (isAdded) continue;
                lengthArr.add(len);
                positionArr.add(i);
            }
            ArrayList<Smali> optimizedSmaliList = new ArrayList<>(totalSmaliNum+20); //+20 backup for [ADD_FILES] rules
            for (int k = 0; k < totalSmaliNum; ++k) {
                optimizedSmaliList.add(ProcessRule.smaliList.get(positionArr.get(k)));
            }
            ProcessRule.smaliList = optimizedSmaliList;
        }
        out.println(ProcessRule.smaliList.size() + " smali files found in " + (System.currentTimeMillis() - startTime) + "ms.");
    }
}