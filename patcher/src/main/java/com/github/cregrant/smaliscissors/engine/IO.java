package com.github.cregrant.smaliscissors.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
class IO {

    private static String loadedProject = "";
    private static final Pattern patRule = Pattern.compile("(\\[.+?](?:\\R(?:NAME|GOTO|SOURCE|SCRIPT|TARGET):)[\\s\\S]*?\\[/.+?])");

    static Patch loadRules(String zipFile) {
        deleteAll(Prefs.tempDir);
        //noinspection ResultOfMethodCallIgnored
        Prefs.tempDir.mkdirs();
        zipExtract(zipFile, Prefs.tempDir.toString());
        Patch patch = new Patch();
        String txtFile = Prefs.tempDir + File.separator + "patch.txt";
        if (!new File(txtFile).exists()) {
            Main.out.println("No patch.txt file in patch!");
            return patch;
        }

        ArrayList<String> rulesListArr = Regex.matchMultiLines(Objects.requireNonNull(patRule), read(txtFile), "rules");
        RuleParser parser = new RuleParser();
        for (String singleRule : rulesListArr) {
            Rule rule = parser.parseRule(singleRule);
            if (rule.isSmali)
                patch.smaliNeeded = true;
            else if (rule.isXml)
                patch.xmlNeeded = true;
            patch.addRule(rule);
        }

        Main.out.println(rulesListArr.size() + " rules found\n");
        return patch;
    }

    static String read(String path) {
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
        } catch (FileNotFoundException e) {
            Main.out.println(path + ':' + " not found.");
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            Main.out.println("Exiting to prevent file corruption");
            System.exit(1);
        }
        return resultString;
    }

    static void write(String path, String content) {
        try {
            deleteAll(new File(path));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void writeChanges() {
        if (Prefs.keepSmaliFilesInRAM) {
            for (int j = 0; j < ProcessRule.smaliList.size(); ++j) {
                DecompiledFile tmpSmali = ProcessRule.smaliList.get(j);
                if (!tmpSmali.isModified()) continue;
                tmpSmali.setModified(false);
                write(Prefs.projectPath + File.separator + tmpSmali.getPath(), tmpSmali.getBody());
            }
        }
        if (Prefs.keepXmlFilesInRAM) {
            for (int j = 0; j < ProcessRule.xmlList.size(); ++j) {
                DecompiledFile dFile = ProcessRule.xmlList.get(j);
                if (!dFile.isModified()) continue;
                dFile.setModified(false);
                write(Prefs.projectPath + File.separator + dFile.getPath(), dFile.getBody());
            }
        }
    }

    static void copy(String src, String dst) {
        File dstFile = new File(dst);
        if (dstFile.exists())
            dstFile.delete();
        src = src.trim(); dst = dst.trim();
        File dstFolder = dstFile.getParentFile();
        dstFolder.mkdirs();

        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)){
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            os.write(buffer);
            os.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
            Main.out.println("Error during copying file...");
        }
    }

    static void zipExtract(String src, String dst) {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                File filePath = mergePath(dst, zipEntry.getName());  //fix path with merge
                if (zipEntry.isDirectory())
                    filePath.mkdirs();
                else {  //file
                    filePath.getParentFile().mkdirs();
                    FileOutputStream fout = new FileOutputStream(filePath);
                    int len;
                    byte[] buffer = new byte[65536];
                    while ((len = zip.read(buffer)) != -1) fout.write(buffer, 0, len);
                    fout.flush();
                    fout.close();
                }
                zip.closeEntry();
            }
        }catch (FileNotFoundException e) {
            Main.out.println("File not found!");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
        catch (IOException e) {
            Main.out.println("Error during extracting zip file.");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
    }

    private static File mergePath(String dstFolder, String toMerge) {
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
        Collection<String> fullTree = new ArrayList<>();
        fullTree.addAll(Arrays.asList(dstTree));
        fullTree.addAll(Arrays.asList(srcTree));
        StringBuilder sb = new StringBuilder();
        String prevStr = "";
        for (String str : fullTree) {
            if (str.equals(prevStr))
                continue;
            sb.append(str).append(File.separator);
            prevStr = str;
        }
        return new File(sb.toString());
    }

    static void deleteAll(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) {
                file.delete();
            }
            else {
                for (File child : Objects.requireNonNull(file.listFiles())) {
                    deleteAll(child);
                }
                if (Objects.requireNonNull(file.list()).length == 0)
                    file.delete();
            }
        }
        else
            file.delete();
    }

    static void loadProjectFiles(boolean xmlNeeded, boolean smaliNeeded) {
        if (Prefs.projectPath.equals(loadedProject) && (xmlNeeded || smaliNeeded)) {
            //new patch requires smali or xml
            Scan.scanProject(xmlNeeded, smaliNeeded);
        }
        else if (smaliNeeded || xmlNeeded) {
            //other project (multiple patching available on pc) or empty files arrays
            ProcessRule.smaliList.clear();
            ProcessRule.xmlList.clear();
            Scan.scanProject(xmlNeeded, smaliNeeded);
            loadedProject = Prefs.projectPath;
        }
    }
}