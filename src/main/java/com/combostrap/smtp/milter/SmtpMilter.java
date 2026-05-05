package com.combostrap.smtp.milter;



import com.combostrap.smtp.SmtpMessage;

import java.util.function.UnaryOperator;

public interface SmtpMilter extends UnaryOperator<SmtpMessage> {

}
