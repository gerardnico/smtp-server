package com.combostrap.type;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Hmac is a one way algorithm used to verify
 * * the data integrity
 * * the signature (for authentication)
 * <p>
 * We are using it to verify the data integrity. See test.
 * *
 * See doc:
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#Mac
 * <p>
 * See example:
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#HmacEx
 * <p>
 * Stackoverflow:
 * From https://stackoverflow.com/questions/3208160/how-to-generate-an-hmac-in-java-equivalent-to-a-python-example
 */
public class CryptoHmac {


  private static final String HMAC_SHA_256 = "HmacSHA256";
  private byte[] key;
  private String transformationString = HMAC_SHA_256;
  private String keyAlgorithm = "PBEWithMD5AndDES";


  private CryptoHmac(byte[] key) {
    this.key = key;
  }

  public static CryptoHmac create(String passphrase) {
    return new CryptoHmac(Cryptos.toBytes(passphrase));
  }

  public static CryptoHmac create(byte[] key) {
    return new CryptoHmac(key);
  }


  /**
   * @return the shared secret key
   * Generate a key for the HMAC-MD5 keyed-hashing algorithm; see RFC 2104
   * In practice, you would save this key.
   */
  public byte[] getKey() {
    if (key == null) {

      try {
        KeyGenerator keyGen = KeyGenerator.getInstance(transformationString);
        SecretKey secretKey = keyGen.generateKey();
        key = secretKey.getEncoded();
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }

    }
    return key;
  }

  /**
   * @param plaintext the plain text
   * @return the signed digest hash
   */
  public byte[] encrypt(String plaintext) {
    return encrypt(Cryptos.toBytes(plaintext));
  }


  /**
   * @param cipherText the text to encrypt
   * @return the signed digest hash
   */
  public byte[] encrypt(byte[] cipherText) {
    try {

      Mac mac = Mac.getInstance(transformationString);
      SecretKeySpec secretKey = new SecretKeySpec(getKey(), keyAlgorithm);
      mac.init(secretKey);
      return mac.doFinal(cipherText);
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public CryptoHmac useSha256() {
    this.transformationString = HMAC_SHA_256;
    this.keyAlgorithm = HMAC_SHA_256;
    return this;
  }

}
