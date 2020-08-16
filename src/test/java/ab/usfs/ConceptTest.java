/*
 * Copyright 2020 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab.usfs;

import org.junit.Assert;
import org.junit.Test;

import java.security.Security;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConceptTest {

  private String testMD5(String s) {
    return Concept.toLowercaseHexadecimal(Concept.MD5.digestBit(s));
  }

  private String testSHA1(String s) {
    return Concept.toLowercaseHexadecimal(Concept.SHA1.digestBit(s));
  }

  private String testSHA256(String s) {
    return Concept.toLowercaseHexadecimal(Concept.SHA256.digestBit(s));
  }

  @Test
  public void testVectors() {
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", testMD5("")); // wikipedia/MD5
    assertEquals("9e107d9d372bb6826bd81d3542a419d6", testMD5("The quick brown fox jumps over the lazy dog"));
    assertEquals("900150983cd24fb0d6963f7d28e17f72", testMD5("abc")); // rfc1321
    assertEquals("0cc175b9c0f1b6a831c399e269772661", testMD5("a"));
    assertEquals("f96b697d7cb7938d525a2f31aaf161d0", testMD5("message digest"));
    assertEquals("c3fcd3d76192e4007dfb496cca67e13b", testMD5("abcdefghijklmnopqrstuvwxyz"));

    assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", testSHA1("")); // wikipedia/SHA-1
    assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", testSHA1("The quick brown fox jumps over the lazy dog"));
    // https://www.di-mgt.com.au/sha_testvectors.html
    assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", testSHA1("abc"));
    // NESSIE project https://www.cosic.esat.kuleuven.be/nessie/testvectors/hash/sha/Sha-1-160.test-vectors
    assertEquals("86F7E437FAA5A7FCE15D1DDCB9EAEAEA377667B8", testSHA1("a").toUpperCase());
    assertEquals("C12252CEDA8BE8994D5FA0290A47231C1D16AAE3", testSHA1("message digest").toUpperCase());
    assertEquals("32D10C7B8CF96570CA04CE37F2A19D84240D3A89", testSHA1("abcdefghijklmnopqrstuvwxyz").toUpperCase());

    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", testSHA256("")); // wikipedia/SHA-2
    assertEquals(0xe3b0, Concept.USFS.getUnsignedShort(""));
    assertEquals(58288, Concept.USFS.getUnsignedShort("")); // e3b0 give USFS short = 58288, octal 161660
    // https://www.di-mgt.com.au/sha_testvectors.html
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", testSHA256("abc"));
    assertEquals(0xba78, Concept.USFS.getUnsignedShort("abc"));
    assertEquals(47736, Concept.USFS.getUnsignedShort("abc")); // ba78 give USFS short = 47736, octal 135170
    // NESSIE project https://www.cosic.esat.kuleuven.be/nessie/testvectors/hash/sha/Sha-2-256.unverified.test-vectors
    assertEquals("CA978112CA1BBDCAFAC231B39A23DC4DA786EFF8147C4E72B9807785AFEE48BB",
        testSHA256("a").toUpperCase());
    assertEquals("F7846F55CF23E14EEBEAB5B4E1550CAD5B509E3348FBC4EFA3A1413D393CB650",
        testSHA256("message digest").toUpperCase());
    assertEquals("71C480DF93D6AE2F1EFAD1447C66C9525E316218CF51FC8D9ED832F2DAF18B73",
        testSHA256("abcdefghijklmnopqrstuvwxyz").toUpperCase());
  }

  /**
   * Message digests required to be supported are MD5, SHA-1, SHA-256. But what is the complete list of them?
   */
  @Test
  public void listDigests() {
    Arrays.stream(Security.getProviders())
        .flatMap(provider -> provider.getServices().stream().filter(service -> "MessageDigest".equals(service.getType())))
        .forEach(service -> {
          String aliases = service.toString();
          int aliasesIndex = aliases.indexOf("aliases:");
          if (aliasesIndex >= 0) {
            aliasesIndex = aliases.indexOf("[", aliasesIndex) + 1;
            aliases = " (" + aliases.substring(aliasesIndex, aliases.indexOf("]", aliasesIndex)) + ")";
          } else {
            aliases = "";
          }
          System.out.println(service.getAlgorithm() + aliases);
        });
    // Output from OpenJDK 11 Linux x64
    // MD2
    // MD5
    // SHA (SHA-1, SHA1, 1.3.14.3.2.26, OID.1.3.14.3.2.26)
    // SHA-224 (2.16.840.1.101.3.4.2.4, OID.2.16.840.1.101.3.4.2.4)
    // SHA-256 (2.16.840.1.101.3.4.2.1, OID.2.16.840.1.101.3.4.2.1)
    // SHA-384 (2.16.840.1.101.3.4.2.2, OID.2.16.840.1.101.3.4.2.2)
    // SHA-512 (2.16.840.1.101.3.4.2.3, OID.2.16.840.1.101.3.4.2.3)
    // SHA-512/224 (2.16.840.1.101.3.4.2.5, OID.2.16.840.1.101.3.4.2.5)
    // SHA-512/256 (2.16.840.1.101.3.4.2.6, OID.2.16.840.1.101.3.4.2.6)
    // SHA3-224 (2.16.840.1.101.3.4.2.7, OID.2.16.840.1.101.3.4.2.7)
    // SHA3-256 (2.16.840.1.101.3.4.2.8, OID.2.16.840.1.101.3.4.2.8)
    // SHA3-384 (2.16.840.1.101.3.4.2.9, OID.2.16.840.1.101.3.4.2.9)
    // SHA3-512 (2.16.840.1.101.3.4.2.10, OID.2.16.840.1.101.3.4.2.10)
    // Output from JDK 8 Windows x64
    // MD2
    // MD5
    // SHA (SHA-1, SHA1, 1.3.14.3.2.26, OID.1.3.14.3.2.26)
    // SHA-224 (2.16.840.1.101.3.4.2.4, OID.2.16.840.1.101.3.4.2.4)
    // SHA-256 (2.16.840.1.101.3.4.2.1, OID.2.16.840.1.101.3.4.2.1)
    // SHA-384 (2.16.840.1.101.3.4.2.2, OID.2.16.840.1.101.3.4.2.2)
    // SHA-512 (2.16.840.1.101.3.4.2.3, OID.2.16.840.1.101.3.4.2.3)
  }

  @Test
  public void testMeta() {
    assertEquals("65535", Concept.v02Meta("65534"));
    assertEquals("/000000/177777", Concept.v02Meta("/000000/177776"));
    assertNull(Concept.v02Meta(null));
    assertNull(Concept.v02Meta("/0000a000"));
    assertNull(Concept.v02Meta("12345"));
  }

  @Test
  public void instantStrings() {
    Instant rfc7231 = Instant.parse("1994-11-06T08:49:37Z"); // rfc7231 - Sun, 06 Nov 1994 08:49:37 GMT
    Assert.assertEquals(rfc7231, Instant.ofEpochMilli(784111777000L));
    Assert.assertEquals(784111777000L, rfc7231.toEpochMilli());
    //Assert.assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", Concept.fromInstantToRfc(rfc7231)); // rfc7231
    // this test failed, need to implement DateTimeFormatter.RFC_7231_DATE_TIME
    // that will provide preferred fixed length format
    Assert.assertEquals(rfc7231, Concept.fromRfcToInstant("Sun, 6 Nov 1994 08:49:37 GMT")); // failed case decoding
    Assert.assertEquals(rfc7231, Concept.fromRfcToInstant("Sun, 06 Nov 1994 08:49:37 GMT"));

    Instant rfc7232 = Instant.parse("1994-11-15T12:45:26Z"); // rfc7232 - Tue, 15 Nov 1994 12:45:26 GMT
    Assert.assertEquals(rfc7232, Instant.ofEpochMilli(784903526000L));
    Assert.assertEquals(784903526000L, rfc7232.toEpochMilli());
    Assert.assertEquals("Tue, 15 Nov 1994 12:45:26 GMT", Concept.fromInstantToRfc(rfc7232)); // rfc7232
    Assert.assertEquals(rfc7232, Concept.fromRfcToInstant("Tue, 15 Nov 1994 12:45:26 GMT"));
  }

  @Test
  public void vectors() { // poor naming
    assertEquals("170017", Concept.USFS.vector(0xF00F).getStr());
    assertEquals(2, Concept.USFS.vector(1).getBit().length);
    assertEquals("00000000000000000000000080123456", Concept.MD5.vector(0x80123456L).getStr());
    assertEquals(16, Concept.MD5.vector(1).getBit().length);
    assertEquals("0000000000000000000000000000000000000000000000000000000000000001", Concept.SHA256.vector(1).getStr());
    assertEquals(32, Concept.SHA256.vector(1).getBit().length);
  }

}
