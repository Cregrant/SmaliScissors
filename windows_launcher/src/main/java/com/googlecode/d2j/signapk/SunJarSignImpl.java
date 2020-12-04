package com.googlecode.d2j.signapk;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SunJarSignImpl extends AbstractJarSign {
    final protected X509Certificate cert;

    public SunJarSignImpl(X509Certificate cert, PrivateKey privateKey) {
        super(privateKey);
        this.cert = cert;
    }

    /** Write a .RSA file with a digital signature. */
    @SuppressWarnings("all")
    protected void writeSignatureBlock(byte[] signature, OutputStream out) throws IOException {

    }
}
