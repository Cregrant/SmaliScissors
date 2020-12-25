package com.googlecode.d2j.util.zip;

/**
 * Do not add constants to this interface! It's implemented by the classes in this package whose names start "Zip", and
 * the constants are thereby public API.
 */
interface ZipConstants {

    long LOCSIG = 0x4034b50, EXTSIG = 0x8074b50, CENSIG = 0x2014b50, ENDSIG = 0x6054b50;

    int LOCHDR = 30, EXTHDR = 16, CENHDR = 46, ENDHDR = 22, LOCVER = 4, LOCFLG = 6, LOCHOW = 8,
            LOCTIM = 10, LOCCRC = 14, LOCSIZ = 18, LOCLEN = 22, LOCNAM = 26, LOCEXT = 28, EXTCRC = 4, EXTSIZ = 8,
            EXTLEN = 12, CENVEM = 4, CENVER = 6, CENFLG = 8, CENHOW = 10, CENTIM = 12, CENCRC = 16, CENSIZ = 20,
            CENLEN = 24, CENNAM = 28, CENEXT = 30, CENCOM = 32, CENDSK = 34, CENATT = 36, CENATX = 38, CENOFF = 42,
            ENDSUB = 8, ENDTOT = 10, ENDSIZ = 12, ENDOFF = 16, ENDCOM = 20;
}
