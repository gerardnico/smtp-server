
package com.combostrap.dmarc;

public class DmarcIllegalStructure extends Exception {
    public DmarcIllegalStructure(String message) {
        super(message);
    }

    public DmarcIllegalStructure(String message, Throwable cause) {
        super(message, cause);
    }

    public DmarcIllegalStructure() {
    }

    public DmarcIllegalStructure(Exception e) {
        super(e);
    }
}
