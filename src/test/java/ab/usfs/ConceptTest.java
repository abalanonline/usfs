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
