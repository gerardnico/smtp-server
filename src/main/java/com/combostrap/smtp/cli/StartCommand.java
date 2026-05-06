package com.combostrap.smtp.cli;

import com.combostrap.smtp.SmtpVerticle;
import com.combostrap.vertx.MainLauncher;

public class StartCommand {

    public static void main(String[] args) {


        new MainLauncher().dispatch(new String[]{"run", SmtpVerticle.class.getName()});

    }

}
