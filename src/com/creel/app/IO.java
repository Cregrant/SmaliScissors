package com.creel.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
class IO {
    private static CountDownLatch cdl;
    private static final Object lock = new Object();

    IO() {
    }

    ArrayList<String> loadRules(File home, String path) {
        Pattern patRule = Pattern.compile("(\\[.+?](?:\\RSOURCE:\\R.++)?\\RTARGET:\\R[\\s\\S]+?\\[/.+?])", Pattern.UNIX_LINES);
        if (Prefs.rules_mode == 0) {
            System.out.println();
        }
        String f = null;
        ArrayList<String> rulesListArr = new ArrayList<>();
        System.out.println("Loading rules...");
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(home + File.separator + path))){
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                FileOutputStream fout = new FileOutputStream(home + File.separator + "temp" + File.separator + entry.getName());
                int c = zip.read();
                while (c != -1) {
                    fout.write(c);
                    c = zip.read();
                }
                fout.flush();
                zip.closeEntry();
                fout.close();
            }
            f = home + File.separator + "temp" + File.separator + "patch.txt";
            if (!new File(f).exists()) {
                System.out.println("No patch.txt file in patch!");
                System.exit(1);
            }
        }
        catch (IOException e) {
            if (Prefs.arch_device.equals("android")) {
                System.out.println("Put patches in /ApkEditor/patches!");
            } else {
                System.out.println("Can`t load patches");
            }
            System.exit(1);
        }
        String content = new IO().read(f);
        Matcher ruleMatched = patRule.matcher(content);
        while (ruleMatched.find()) {
            for (int i = 0; i < ruleMatched.groupCount(); ++i) {
                rulesListArr.add(ruleMatched.group(i));
            }
        }
        System.out.println(rulesListArr.size() + " rules found\n");
        return rulesListArr;
    }

    String read(String path) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            int length;
            byte[] buffer = new byte[8192];
            assert (inputStream != null);
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    void write(String path, String content) {
        try {
            this.deleteInDirectory(new File(path));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void writeChangesInSmali() {
        for (int j = 0; j < Rules.smaliList.size(); ++j) {
            Smali tmpSmali = Rules.smaliList.get(j);
            if (!tmpSmali.isModified()) continue;
            tmpSmali.setModified(false);
            synchronized (lock) {
                Rules.smaliList.set(j, tmpSmali);
            }
            new IO().write(tmpSmali.getPath(), tmpSmali.getBody());
        }
    }

    synchronized void copy(String src, String dst) {
        File dstFolder = new File(dst).getParentFile();
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        }
        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)){
            int length;
            byte[] buffer = new byte[8192];
            while ((length = ((InputStream)is).read(buffer)) > 0) {
                ((OutputStream)os).write(buffer, 0, length);
            }
            os.flush();
        }
        catch (IOException e) {
            System.out.println("Hmm.. error during copying file...");
        }
    }

    synchronized void deleteInDirectory(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) {
                file.delete();
            } else {
                for (String child : Objects.requireNonNull(file.list())) {
                    this.deleteInDirectory(new File(file, child));
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
        if (!currentProjectPath.equals(Regex.currentProjectPathCached)) {
            Rules.smaliList.clear();
            System.out.println("\nScanning " + currentProjectPath);
            IO.scan(currentProjectPath);
            Regex.currentProjectPathCached = currentProjectPath;
        }
    }

    private static void scan(String projectPath) {
        long startTime = System.currentTimeMillis();
        ArrayList<String> smFolders = new ArrayList<>();
        for (String i : Objects.requireNonNull(new File(projectPath).list())) {
            if (!i.startsWith("smali")) continue;
            smFolders.add(i);
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
                            Rules.smaliList.add(tmp);
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
        AtomicInteger currentNum = new AtomicInteger(Rules.smaliList.size() - 1);
        int thrNum = Prefs.max_thread_num;
        cdl = new CountDownLatch(thrNum);
        for (int curThread = 1; curThread <= thrNum; ++curThread) {
            new Thread(() -> {
                int num;
                while ((num = currentNum.getAndDecrement()) >= 0) {
                    Smali tmpSmali = Rules.smaliList.get(num);
                    tmpSmali.setBody(new IO().read(tmpSmali.getPath()));
                    synchronized (lock) {
                        Rules.smaliList.set(num, tmpSmali);
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
            int totalSmaliNum = Rules.smaliList.size() - 1;
            ArrayList<Integer> positionArr = new ArrayList<>(totalSmaliNum);
            ArrayList<Integer> lengthArr = new ArrayList<>(totalSmaliNum);
            for (int i = 0; i < totalSmaliNum; ++i) {
                int len = Rules.smaliList.get(i).getBody().length();
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
                optimizedSmaliList.add(Rules.smaliList.get(positionArr.get(k)));
            }
            Rules.smaliList = optimizedSmaliList;
        }
        System.out.println(Rules.smaliList.size() + " smali files found in " + (System.currentTimeMillis() - startTime) + "ms.");
    }
}