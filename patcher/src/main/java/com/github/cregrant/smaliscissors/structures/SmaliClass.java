package com.github.cregrant.smaliscissors.structures;

import com.github.cregrant.smaliscissors.Gzip;
import com.github.cregrant.smaliscissors.Prefs;

import java.io.File;

public class SmaliClass {
    private final DecompiledFile file;
    private final String path;
    private Object body;

    public SmaliClass(DecompiledFile df) {
        file = df;
        String temp = df.getPath();
        path = 'L' + temp.substring(temp.indexOf(File.separatorChar)+1);
    }

    public String getPath() {
        return path;
    }

    public DecompiledFile getFile() {
        return file;
    }

    public String getBody() {
        if (Prefs.reduceMemoryUsage)
            return ((Gzip) body).decompress();
        else
            return (String) body;
    }

    public void setBody(String newBody) {
        if (Prefs.reduceMemoryUsage)
            body = new Gzip(newBody);
        else
            body = newBody;
    }
}
