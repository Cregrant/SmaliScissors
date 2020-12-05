/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2012 Panxiaobo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.dex2jar.tools;

import com.googlecode.d2j.dex.Dex2jar;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Dex2jarCmd extends BaseCmd {

    public static void main(String... args) {
        new Dex2jarCmd().doMain(args);
    }

    @Override
    protected void doCommandLine() throws Exception {
        Path dexPath = new File(remainingArgs[0]).toPath();
        Path outJar = new File(remainingArgs[0].replace(".dex", ".jar")).toPath();

        BaseDexFileReader reader = MultiDexFileReader.open(Files.readAllBytes(dexPath));
        BaksmaliBaseDexExceptionHandler handler = new BaksmaliBaseDexExceptionHandler();
        Dex2jar.from(reader)
                .withExceptionHandler(null)
                .reUseReg(false)
                .topoLogicalSort()
                .skipDebug(true)
                .optimizeSynchronized(false)
                .printIR(false)
                .noCode(false)
                .skipExceptions(false)
                .to(outJar);

        if (handler.hasException()) {
            Path errorFile = dexPath.getParent().resolve("error.zip");
            System.err.println("Detail Error Information in File " + errorFile);
            System.err.println(BaksmaliBaseDexExceptionHandler.REPORT_MESSAGE);
            handler.dump(errorFile, orginalArgs);
        }
    }
}
