
package com.combostrap.dns;

public class IllegalStructure extends Exception {
    public IllegalStructure(String message) {
        super(message);
    }

    public IllegalStructure(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalStructure() {
    }

    public IllegalStructure(Exception e) {
        super(e);
    }
}
