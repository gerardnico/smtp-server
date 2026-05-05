package com.combostrap.type;


import static java.nio.charset.StandardCharsets.UTF_8;

public class Cryptos {


    /**
     * Byte to UTF-8 String
     *
     * @param bytes
     * @return an UTF-8 String
     */
    static public String toString(byte[] bytes) {
        return new String(bytes, UTF_8);
    }


    public static byte[] toBytes(String s) {
        return s.getBytes(UTF_8);
    }


}
