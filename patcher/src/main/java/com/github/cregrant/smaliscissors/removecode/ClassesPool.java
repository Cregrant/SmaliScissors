package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

class ClassesPool {

    private static final Logger logger = LoggerFactory.getLogger(ClassesPool.class);
    private final Project project;
    private final HashSet<String> rootFolders = new HashSet<>();
    private Map.Entry<String, ArrayList<SmaliFile>>[] array;
    private Map<String, ArrayList<SmaliFile>> map;
    private Map<String, String> hierarchy;

    public ClassesPool(Project project) {
        this.project = project;
        scan();
    }

    private void scan() {
        List<SmaliFile> smaliFiles = project.getSmaliList();
        final Map<String, Set<SmaliFile>> cMap = new ConcurrentHashMap<>(smaliFiles.size());
        for (SmaliFile file : smaliFiles) {
            String path = SmaliTarget.removePathObfuscation(file.getPath());
            int start = path.indexOf('/') + 1;
            int rootFolderEnd = path.indexOf('/', start);
            if (rootFolderEnd != -1) {
                rootFolders.add(path.substring(start, rootFolderEnd));
            }
            String ref = "L" + path.substring(start, path.length() - 6) + ";";
            cMap.put(ref, Collections.synchronizedSet(new HashSet<SmaliFile>()));
        }

        final Map<String, String> hierarchySyncronized = Collections.synchronizedMap(new HashMap<String, String>());
        ArrayList<Future<?>> futures = new ArrayList<>(smaliFiles.size());
        for (final SmaliFile file : smaliFiles) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    List<String> classRefs = extractClassRefs(file);
                    if (classRefs.size() < 2) {
                        return;
                    }
                    hierarchySyncronized.put(classRefs.get(0), classRefs.get(1));
                    for (String ref : classRefs) {
                        Set<SmaliFile> list = cMap.get(ref);
                        if (list != null) {
                            list.add(file);
                        }
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().waitForFinish(futures);

        buildUsagesMap(cMap);
        buildHierarchy(hierarchySyncronized);
    }

    private void buildUsagesMap(Map<String, Set<SmaliFile>> cMap) {
        map = new HashMap<>(cMap.size());
        for (Map.Entry<String, Set<SmaliFile>> entry : cMap.entrySet()) {
            ArrayList<SmaliFile> list = new ArrayList<>(entry.getValue());
            map.put(entry.getKey(), list);
        }
        //noinspection unchecked
        array = (Map.Entry<String, ArrayList<SmaliFile>>[]) map.entrySet().toArray(new Map.Entry[0]);
        if (array.length == 0) {
            throw new RuntimeException("Classes pool is empty");
        }
    }

    private void buildHierarchy(Map<String, String> hierarchySyncronized) {
        hierarchy = new HashMap<>(hierarchySyncronized);
    }

    private List<String> extractClassRefs(SmaliFile file) {
        String body = file.getBody();
        ArrayList<String> strings = new ArrayList<>();
        int pos = 0;
        int nextLinePos = 0;

        while ((pos = body.indexOf('L', pos)) != -1) {
            int endPos = body.indexOf(';', pos);
            if (endPos == -1) {
                break;
            }

            boolean accept = endPos < nextLinePos;  //no need to check twice the same line
            if (!accept && body.charAt(body.lastIndexOf('\n', pos) + 1) != '#') {   //line is not commented out
                nextLinePos = body.indexOf('\n', pos);
                if (nextLinePos < endPos) {         //not a ref
                    pos = nextLinePos;
                    continue;
                }
                accept = true;
            }
            boolean isSuperclass = strings.size() == 1;
            if (accept && (isSuperclass || (!body.startsWith("Ljava/", pos) && !body.startsWith("Ldalvik/", pos)))) {     //skip internal classes
                String ref = body.substring(pos, endPos + 1);
                if (isSuperclass && ref.equals(ClassHeader.OBJECT_REF)) {
                    ref = ref.intern();
                }
                strings.add(ref);
            }
            pos = endPos;
        }

        return strings;
    }

    public Map<String, ArrayList<SmaliFile>> getMap() {
        return map;
    }

    public Map<String, String> getHierarchy() {
        return hierarchy;
    }

    Map.Entry<String, ArrayList<SmaliFile>>[] getArray() {
        return array;
    }

    public HashSet<String> getRootFolders() {
        return rootFolders;
    }
}