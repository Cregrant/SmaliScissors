package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.DecompiledFile;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Add implements IRule {
    public String name;
    public String source;
    public boolean extract = false;
    public ArrayList<String> targets;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null && targets.size() == 1 && source != null;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public boolean canBeMerged(IRule otherRule) {
        return false;
    }

    @Override
    public void apply(Project project, Patch patch) {
        ArrayList<String> extractedPathList;
        String dstLocation = project.getPath() + File.separator + targets.get(0);
        if (extract) {
            File tempDirFile = new File(patch.getFile().getParent() + File.separator + "temp");
            IO.delete(tempDirFile);
            tempDirFile.mkdirs();
            extract(patch.getFile(), tempDirFile.getPath(), source);
            File[] files = tempDirFile.listFiles();
            if (files == null || files.length == 0) {
                Main.out.println("What? Zip extract failed???");
                return;
            }
            extractedPathList = new ArrayList<>();
            for (File tempFile : files) {
                String oldPath = tempFile.getPath();
                String newPath = oldPath.replace(tempDirFile.getPath(), dstLocation);
                IO.copy(oldPath, newPath);
                extractedPathList.add(newPath);
            }
            tempDirFile.delete();
        }
        else
            extractedPathList = extract(patch.getFile(), dstLocation, source);

        Scanner scanner = new Scanner(project);
        Iterable<DecompiledFile> newFiles = scanner.scanFiles(extractedPathList);

        for (DecompiledFile df : newFiles) {
            Scanner.removeLoadedFile(project, df.getPath(), false);
            if (df.isXML())
                project.getXmlList().add(df);
            else
                project.getSmaliList().add(df);
        }
    }

    private ArrayList<String> extract(File zipFile, String dstPath, String filename) {
        ArrayList<String> extractedPathList = new ArrayList<>();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().equals("patch.txt") || !zipEntry.getName().startsWith(filename))
                    continue;

                File filePath = new File(dstPath + File.separator + filename);
                if (zipEntry.isDirectory())
                    filePath.mkdirs();
                else {
                    filePath.getParentFile().mkdirs();
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    bos.flush();
                    bos.close();
                }
                extractedPathList.add(filePath.getPath());
                zis.closeEntry();
            }
        } catch (FileNotFoundException e) {
            Main.out.println("Temp zip file not found!");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        } catch (IOException e) {
            Main.out.println("Error during extracting zip file.");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
        return extractedPathList;
    }

    private void extractFull(File zipFile, String dstPath) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith("patch.txt"))
                    continue;

                File filePath = mergePath(dstPath, zipEntry.getName());
                if (zipEntry.isDirectory())
                    filePath.mkdirs();
                else {
                    filePath.getParentFile().mkdirs();
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    bos.flush();
                    bos.close();
                }

                zis.closeEntry();
            }
        } catch (FileNotFoundException e) {
            Main.out.println("Temp zip file not found!");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        } catch (IOException e) {
            Main.out.println("Error during extracting zip file.");
            if (Prefs.verbose_level == 0) e.printStackTrace();
        }
    }

    public static void zipExtract(String src, String dst) {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                File filePath = mergePath(dst, zipEntry.getName());  //fix path with merge
                if (zipEntry.isDirectory())
                    filePath.mkdirs();
                else {
                    filePath.getParentFile().mkdirs();
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    int len;
                    byte[] buffer = new byte[65536];
                    while ((len = zip.read(buffer)) != -1) bos.write(buffer, 0, len);
                    bos.flush();
                    bos.close();
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
        Path dstPath = new File(dstFolder).toPath();
        return dstPath.resolve(toMerge).toFile();
    }

    private static File mergePathOld(String dstFolder, String toMerge) {
        String[] dstPath = dstFolder.replace('\\', '/').split("/");
        String[] newPath = toMerge.replace('\\', '/').split("/");
        String[] fullPath = Arrays.copyOf(dstPath, dstPath.length + newPath.length);
        System.arraycopy(newPath, 0, fullPath, dstPath.length, newPath.length);

        StringBuilder sb = new StringBuilder(dstFolder.length() + toMerge.length());
        String prevStr = "";
        for (String str : fullPath) {
            if (str.equals(prevStr))
                continue;
            sb.append(str).append(File.separator);
            prevStr = str;
        }
        return new File(sb.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    ADD_FILES\n");
        if (name != null)
            sb.append("Name:    ").append(name).append('\n');
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        sb.append("Source:  ").append(source).append('\n');
        sb.append("Extract: ").append(extract).append('\n');
        return sb.toString();
    }
}
