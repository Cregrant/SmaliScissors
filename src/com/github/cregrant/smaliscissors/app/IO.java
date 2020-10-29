package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.out;

@SuppressWarnings("ResultOfMethodCallIgnored")
class IO {

    static String currentProjectPathCached = "";

    void loadRules(File patchesDir, String zipName, Patch patch) {
        Pattern patRule = Pattern.compile("(\\[.+?](?:\\RNAME:\\R.++)?(?:\\RGOTO:\\R.++)?(?:\\RSOURCE:\\R.++)?\\R(?:TARGET:[\\s\\S]*?)?\\[/.+?])", Pattern.UNIX_LINES);
        if (!Prefs.rules_AEmode) {
            out.println("TruePatcher mode on.");
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

        ArrayList<String> rulesListArr = new Regex().matchMultiLines(Objects.requireNonNull(patRule), read(txtFile), "rules");
        RuleParser parser = new RuleParser();
        for (String rule : rulesListArr) patch.addRule(parser.parseRule(rule));

        out.println(rulesListArr.size() + " rules found\n");
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

    void writeChanges() {
        if (Prefs.keepSmaliFilesInRAM) {
            for (int j = 0; j < ProcessRule.smaliList.size(); ++j) {
                DecompiledFile tmpSmali = ProcessRule.smaliList.get(j);
                if (tmpSmali.isNotModified()) continue;
                tmpSmali.setModified(false);
                write(tmpSmali.getPath(), tmpSmali.getBody());
            }
        }
        if (Prefs.keepXmlFilesInRAM) {
            for (int j = 0; j < ProcessRule.xmlList.size(); ++j) {
                DecompiledFile dFile = ProcessRule.xmlList.get(j);
                if (dFile.isNotModified()) continue;
                dFile.setModified(false);
                write(dFile.getPath(), dFile.getBody());
            }
        }
    }

    void copy(String src, String dst) {
        src.trim(); dst.trim();
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

    ArrayList<String> getNamesFromZip(String src, String dst) {
        ArrayList<String> outArr = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                File filePath = mergePath(dst, zipEntry.getName());  //fix path with merge
                if (!zipEntry.isDirectory())
                    outArr.add(filePath.toString());
                zip.closeEntry();
            }
        } catch (IOException e) {
            out.println("Error during extracting names from zip file.");
        }
        return outArr;
    }

    void zipExtract(String src, String dst) {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                File filePath = mergePath(dst, zipEntry.getName());  //fix path with merge
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
        String[] dstTree;
        String[] srcTree;
        if (dstFolder.contains("/"))
            dstTree = dstFolder.split("/");
        else
            dstTree = dstFolder.split("\\\\");
        if (toMerge.contains("/"))
            srcTree = toMerge.split("/");
        else
            srcTree = toMerge.split("\\\\");
        List<String> fullTree = new ArrayList<>();
        fullTree.addAll(Arrays.asList(dstTree));
        fullTree.addAll(Arrays.asList(srcTree));
        StringBuilder sb = new StringBuilder();
        String prevStr = "";
        for (String str : fullTree) {
            if (str.equals(prevStr) | str.startsWith("smali") | str.equals("res"))
                continue;
            prevStr = str;
            sb.append(str).append(File.separator);
        }
        return new File(sb.toString());
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
            ProcessRule.xmlList.clear();
            scanProject(currentProjectPath);
            currentProjectPathCached = currentProjectPath;
        }
    }

    private static CountDownLatch cdl;
    private static final Object lock = new Object();

    private void scanProject(String projectPath) {
        long startTime = System.currentTimeMillis();
        out.println("\nScanning " + projectPath);

        DecompiledFile manifest = new DecompiledFile(projectPath, true);  //add AndroidManifest.xml
        manifest.setPath("AndroidManifest.xml");
        ProcessRule.xmlList.add(manifest);

        File resFolder = new File(projectPath + File.separator + "res");
        if (Objects.requireNonNull(resFolder.list()).length == 0) {
            out.println("WARNING: no resources found inside the res folder.");
        }

        ArrayList<File> smFolders = new ArrayList<>();
        for (File i : Objects.requireNonNull(new File(projectPath).listFiles())) {
            String str = i.toString().replace(projectPath + File.separator, "");
            if (str.startsWith("smali"))
                smFolders.add(i);
        }
        if (smFolders.isEmpty()) {
            out.println("WARNING: no smali folders found inside the project folder \"" + new Regex().getEndOfPath(projectPath) + '\"');
        }

        smFolders.add(resFolder);
        cdl = new CountDownLatch(smFolders.size());
        for (File folder : smFolders) {                   //scan res & smali folders
            new Thread(() -> {
                Stack<File> stack = new Stack<>();
                stack.push(folder);
                while (!stack.isEmpty()) {
                    for (File file : Objects.requireNonNull(stack.pop().listFiles())) {
                        if (file.isDirectory())
                            stack.push(file);
                        String path = file.toString().replace(projectPath + File.separator, "");
                        DecompiledFile tmp;
                        boolean isSmali = path.endsWith(".smali");
                        if (isSmali || path.endsWith(".xml")) {
                            if (isSmali)
                                tmp = new DecompiledFile(projectPath, false);
                            else
                                tmp = new DecompiledFile(projectPath, true);
                            tmp.setPath(path);
                            synchronized (lock) {
                                if (isSmali)
                                    ProcessRule.smaliList.add(tmp);
                                else
                                    ProcessRule.xmlList.add(tmp);
                            }
                        }
                    }
                }
                cdl.countDown();
            }).start();
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int thrNum = Prefs.max_thread_num;
        if (Prefs.keepSmaliFilesInRAM) {
            int totalNum = ProcessRule.smaliList.size();
            AtomicInteger currentNum = new AtomicInteger(0);
            cdl = new CountDownLatch(thrNum);
            for (int curThread = 1; curThread <= thrNum; ++curThread) {
                new Thread(() -> {
                    int num;
                    while ((num = currentNum.getAndDecrement()) < totalNum) {
                        DecompiledFile dFile = ProcessRule.smaliList.get(num);
                        dFile.setBody(read(projectPath + File.separator + dFile.getPath()));
                    }
                    cdl.countDown();
                }).start();
            }
            try {
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (Prefs.keepXmlFilesInRAM) {
            int totalNum = ProcessRule.xmlList.size();
            AtomicInteger currentNum = new AtomicInteger(0);
            cdl = new CountDownLatch(thrNum);
            for (int curThread = 1; curThread <= thrNum; ++curThread) {
                new Thread(() -> {
                    int num;
                    while ((num = currentNum.getAndDecrement()) < totalNum) {
                        DecompiledFile dFile = ProcessRule.xmlList.get(num);
                        dFile.setBody(read(projectPath + File.separator + dFile.getPath()));
                    }
                    cdl.countDown();
                }).start();
            }
            try {
                cdl.await();
            } catch (InterruptedException e) {
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
                ArrayList<DecompiledFile> optimizedSmaliList = new ArrayList<>(totalSmaliNum + 20); //+20 backup for [ADD_FILES] rules
                for (int k = 0; k < totalSmaliNum; ++k) {
                    optimizedSmaliList.add(ProcessRule.smaliList.get(positionArr.get(k)));
                }
                ProcessRule.smaliList = optimizedSmaliList;
            }
        }
        out.println(ProcessRule.smaliList.size() + " smali & " + ProcessRule.xmlList.size() + " xml files found in " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    void scanFolder(String projectPath, String folder) {
        File targetFolder = new File(folder);
        Stack<File> stack = new Stack<>();
        stack.add(targetFolder);
        while (!stack.isEmpty()) {
            if (stack.peek().isDirectory()) {
                for (File file : Objects.requireNonNull(stack.pop().listFiles())) {
                    if (file.isDirectory()) {
                        stack.push(file);
                        continue;
                    }
                    scanFile(projectPath, file);
                }
            }
            else scanFile(projectPath, stack.pop());
        }
    }

    private void scanFile(String projectPath, File file) {
        String fullPath = projectPath + File.separator + file.toString().replace(projectPath + File.separator, "");
        if (fullPath.endsWith(".smali") | fullPath.endsWith(".xml")) {
            String shortPath = fullPath.replace(projectPath + File.separator, "");
            boolean isXml = shortPath.endsWith(".xml");
            DecompiledFile addedFile = new DecompiledFile(projectPath, isXml);
            addedFile.setPath(shortPath);
            if ((isXml && Prefs.keepXmlFilesInRAM) || (!isXml && Prefs.keepSmaliFilesInRAM))
                addedFile.setBody(new IO().read(fullPath));
            removeLoadedFile(shortPath);
            if (isXml)
                ProcessRule.xmlList.add(addedFile);
            else
                ProcessRule.smaliList.add(addedFile);
            if (Prefs.verbose_level == 0) {
                out.println(shortPath + " added.");
            }
        }
    }

    void removeLoadedFile(String shortPath) {
        if (Prefs.verbose_level == 0) {
            out.println(shortPath + " removed.");
        }
        boolean isXml = shortPath.endsWith("xml");
        int size;
        if (isXml)
            size = ProcessRule.xmlList.size();
        else
            size = ProcessRule.smaliList.size();

        for (int i = 0; i < size; i++) {
            if (isXml && ProcessRule.xmlList.get(i).getPath().equals(shortPath)) {
                ProcessRule.xmlList.remove(i);
                i--;
                size--;
            }
            else if (ProcessRule.smaliList.get(i).getPath().equals(shortPath)) {
                ProcessRule.smaliList.remove(i);
                i--;
                size--;
            }
        }
    }
}