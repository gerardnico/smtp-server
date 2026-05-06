package com.combostrap.smtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmtpMailBoxConf {

    public String type;
    public List<String> milters = new ArrayList<>();

    public Map<String, Object> props = new HashMap<>();
}
