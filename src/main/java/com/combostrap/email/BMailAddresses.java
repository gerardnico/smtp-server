package com.combostrap.email;

import jakarta.mail.internet.AddressException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a valid list of email addresses found in an email
 */
public class BMailAddresses {
  public static final String SEP = ";";
  private final List<BMailInternetAddress> bMailInternetAddresses = new ArrayList<>();

  public static BMailAddresses of(String addresses) throws AddressException {
    BMailAddresses bMailAddresses = new BMailAddresses();
    for (String address : addresses.split(SEP)) {
      bMailAddresses.add(BMailInternetAddress.of(address));
    }

    return bMailAddresses;
  }

  public static BMailAddresses of(List<BMailInternetAddress> addresses) throws AddressException {
    BMailAddresses bMailAddresses = new BMailAddresses();
    bMailAddresses.addAll(addresses);
    return bMailAddresses;
  }

  public BMailAddresses addAll(Collection<BMailInternetAddress> bMailInternetAddresses) {
    this.bMailInternetAddresses.addAll(bMailInternetAddresses);
    return this;
  }

  private BMailAddresses add(BMailInternetAddress of) {
    this.bMailInternetAddresses.add(of);
    return this;
  }

  public List<String> toAddresses() {
    return this.bMailInternetAddresses
      .stream()
      .map(BMailInternetAddress::getAddress)
      .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return this.bMailInternetAddresses.stream()
      .map(BMailInternetAddress::toString)
      .collect(Collectors.joining(SEP));
  }
}
