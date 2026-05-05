package com.combostrap.smtp;

import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertEquals(mailBox, path.getMailBox());
    Assert.assertEquals(route, path.getRoute());

  }

  @Test
  public void validPathWithoutHostTest() throws SmtpException {

    String mailBox = "user@site";
    SmtpPath path = SmtpPath.of("<" + mailBox + ">");
    Assert.assertEquals(mailBox, path.getMailBox());

  }

  @Test
  public void emptyPathTest() throws SmtpException {

    /**
     * An empty path is used when returning a bounce <>
     */
    SmtpPath path = SmtpPath.empty();
    Assert.assertEquals("", path.getMailBox());
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
    Assert.assertEquals(SmtpPostMaster.POSTMASTER, smtpPath.getMailBox());

  }

}
