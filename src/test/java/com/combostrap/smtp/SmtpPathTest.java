package com.combostrap.smtp;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmtpPathTest {

  @Test
  public void validaPathWithHostTest() throws SmtpException {
    /**
     * MAIL FROM:<@a,@b:user@d>
     */
    String mailBox = "user@site";
    String route = "@hosta,@hostb";
    String actualPath = route + ":" + mailBox;
    SmtpPath path = SmtpPath.of("<" + actualPath + ">");
    Assertions.assertEquals(mailBox, path.getMailBox());
    Assertions.assertEquals(route, path.getRoute());

  }

  @Test
  public void validPathWithoutHostTest() throws SmtpException {

    String mailBox = "user@site";
    SmtpPath path = SmtpPath.of("<" + mailBox + ">");
    Assertions.assertEquals(mailBox, path.getMailBox());

  }

  @Test
  public void emptyPathTest() throws SmtpException {

    /**
     * An empty path is used when returning a bounce <>
     */
    SmtpPath path = SmtpPath.empty();
    Assertions.assertEquals("", path.getMailBox());
  }

  @Test
  public void invalidPathEmptyMailboxTest() {

    try {
      SmtpPath.of("<@host1:>");
    } catch (SmtpException e) {
      // no empty mailbox
    }
  }

  @Test
  public void validPathPostmasterTest() throws SmtpException {

    // should not throw
    SmtpPath smtpPath = SmtpPath.of("<" + SmtpPostMaster.POSTMASTER + ">");
    Assertions.assertEquals(SmtpPostMaster.POSTMASTER, smtpPath.getMailBox());

  }

}
