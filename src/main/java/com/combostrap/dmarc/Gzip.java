package com.combostrap.dmarc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class Gzip {

    public static String decompress(byte[] compressed) throws IOException {
        try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            gzipIn.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
