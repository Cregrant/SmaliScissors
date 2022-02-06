package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.DecompiledFile;
import com.github.cregrant.smaliscissors.structures.rules.IRule;

import java.io.File;
import java.util.List;

public class Project {
    private final String path;
    private final String name;
    List<DecompiledFile> smaliList;
    List<DecompiledFile> xmlList;

    public Project(String path) {
        this.path = path;
        name = Regex.getFilename(path);
    }

    void scan(boolean scanSmali, boolean scanXml) {
        Scanner scanner = new Scanner(this);
        scanner.scanProject(scanSmali && smaliList == null, scanXml && smaliList == null);
        smaliList = scanner.getSmaliList();
        xmlList = scanner.getXmlList();
    }

    void applyPatch(Patch patch) {
        while (true) {
            IRule rule = patch.getNextRule();
            if (rule == null)
                break;
            Main.out.println(rule.toString());
            rule.apply(this, patch);
            patch.jumpToRuleName(rule.nextRuleName());
        }
        patch.reset();
    }

    void writeChanges() {
        if (Prefs.keepSmaliFilesInRAM) {
            for (DecompiledFile dFile : smaliList) {
                if (!dFile.isModified())
                    continue;
                IO.write(path + File.separator + dFile.getPath(), dFile.getBody());
            }
        }
        if (Prefs.keepXmlFilesInRAM) {
            for (DecompiledFile dFile : xmlList) {
                if (!dFile.isModified())
                    continue;
                IO.write(path + File.separator + dFile.getPath(), dFile.getBody());
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<DecompiledFile> getSmaliList() {
        return smaliList;
    }

    public List<DecompiledFile> getXmlList() {
        return xmlList;
    }
}
