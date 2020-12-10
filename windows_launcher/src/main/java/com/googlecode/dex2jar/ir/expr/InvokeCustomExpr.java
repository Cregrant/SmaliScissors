/*
 * Copyright (c) 2009-2017 Panxiaobo
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
package com.googlecode.dex2jar.ir.expr;

import com.googlecode.d2j.MethodHandle;
import com.googlecode.d2j.Proto;
import com.googlecode.dex2jar.ir.LabelAndLocalMapper;

public class InvokeCustomExpr extends AbstractInvokeExpr {
    public String name;
    public Proto proto;
    public MethodHandle handle;
    public Object[] bsmArgs;

    @Override
    protected void releaseMemory() {
        name = null;
        proto = null;
        handle = null;
        bsmArgs = null;
        super.releaseMemory();
    }

    @Override
    public Proto getProto() {
        return proto;
    }

    public InvokeCustomExpr(VT type, Value[] args, String methodName, Proto proto, MethodHandle handle, Object[] bsmArgs) {
        super(type, args);
        this.proto = proto;
        this.name = methodName;
        this.handle = handle;
        this.bsmArgs = bsmArgs;
    }

    @Override
    public Value clone() {
        return new InvokeCustomExpr(vt, cloneOps(), name, proto, handle, bsmArgs);
    }

    @Override
    public Value clone(LabelAndLocalMapper mapper) {
        return new InvokeCustomExpr(vt, cloneOps(mapper), name, proto, handle, bsmArgs);
    }

    @Override
    public String toString0() {

        return "InvokeCustomExpr(....)";
    }
}
