package net.iryndin.jdbf.core;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexandr Zernov
 */
public class DbfCharset {
    private static final Map<Integer, Integer> CHARSETS = new HashMap<Integer, Integer>() {
        {
            put(0x01, 437);  // US MS-DOS
            put(0x02, 850);  // International MS-DOS
            put(0x03, 1252);  // Windows ANSI Latin I
            put(0x04, 10000);  // Standard Macintosh
            put(0x08, 865);  // Danish OEM
            put(0x09, 437);  // Dutch OEM
            put(0x0A, 850);  // Dutch OEM*
            put(0x0B, 437);  // Finnish OEM
            put(0x0D, 437);  // French OEM
            put(0x0E, 850);  // French OEM*
            put(0x0F, 437);  // German OEM
            put(0x10, 850);  // German OEM*
            put(0x11, 437);  // Italian OEM
            put(0x12, 850);  // Italian OEM*
            put(0x13, 932);  // Japanese Shift-JIS
            put(0x14, 850);  // Spanish OEM*
            put(0x15, 437);  // Swedish OEM
            put(0x16, 850);  // Swedish OEM*
            put(0x17, 865);  // Norwegian OEM
            put(0x18, 437);  // Spanish OEM
            put(0x19, 437);  // English OEM (Great Britain)
            put(0x1A, 850);  // English OEM (Great Britain)*
            put(0x1B, 437);  // English OEM (US)
            put(0x1C, 863);  // French OEM (Canada)
            put(0x1D, 850);  // French OEM*
            put(0x1F, 852);  // Czech OEM
            put(0x22, 852);  // Hungarian OEM
            put(0x23, 852);  // Polish OEM
            put(0x24, 860);  // Portuguese OEM
            put(0x25, 850);  // Portuguese OEM*
            put(0x26, 866);  // Russian OEM
            put(0x37, 850);  // English OEM (US)*
            put(0x40, 852);  // Romanian OEM
            put(0x4D, 936);  // Chinese GBK (PRC)
            put(0x4E, 949);  // Korean (ANSI/OEM)
            put(0x4F, 950);  // Chinese Big5 (Taiwan)
            put(0x50, 874);  // Thai (ANSI/OEM)
            put(0x57, -1);  // Current ANSI CP ANSI
            put(0x58, 1252);  // Western European ANSI
            put(0x59, 1252);  // Spanish ANSI
            put(0x64, 852);  // Eastern European MS-DOS
            put(0x65, 866);  // Russian MS-DOS
            put(0x66, 865);  // Nordic MS-DOS
            put(0x67, 861);  // Icelandic MS-DOS
            put(0x68, 895);  // Kamenicky (Czech) MS-DOS
            put(0x69, 620);  // Mazovia (Polish) MS-DOS
            put(0x6A, 737);  // Greek MS-DOS (437G)
            put(0x6B, 857);  // Turkish MS-DOS
            put(0x6C, 863);  // French-Canadian MS-DOS
            put(0x78, 950);  // Taiwan Big 5
            put(0x79, 949);  // Hangul (Wansung)
            put(0x7A, 936);  // PRC GBK
            put(0x7B, 932);  // Japanese Shift-JIS
            put(0x7C, 874);  // Thai Windows/MS–DOS
            put(0x86, 737);  // Greek OEM
            put(0x87, 852);  // Slovenian OEM
            put(0x88, 857);  // Turkish OEM
            put(0x96, 10007);  // Russian Macintosh
            put(0x97, 10029);  // Eastern European Macintosh
            put(0x98, 10006);  // Greek Macintosh
            put(0xC8, 1250);  // Eastern European Windows
            put(0xC9, 1251);  // Russian Windows
            put(0xCA, 1254);  // Turkish Windows
            put(0xCB, 1253);  // Greek Windows
            put(0xCC, 1257);  // Baltic Windows
        }
    };

    public static Charset fromInt(byte bCharset) {
        int iCharset = 0xFF & bCharset;
        if (iCharset != 0x57) {
            final Integer cp = CHARSETS.get(iCharset);
            if (cp != null) {
                try {
                    return Charset.forName("cp" + cp);
                } catch (UnsupportedCharsetException e) {
                    // todo Log
                }
            }
        }

        return Charset.defaultCharset();
    }
}
