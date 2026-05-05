package com.combostrap.smtp.sasl;

public abstract class SimpleAuth implements SimpleAuthHandlerInterface {

  private final SimpleAuthMechanism authMechanism;

  public SimpleAuth(SimpleAuthMechanism simpleAuthMechanism) {

    this.authMechanism = simpleAuthMechanism;

  }

  @SuppressWarnings("unused")
  public SimpleAuthMechanism getAuthMechanism() {
    return authMechanism;
  }



}
