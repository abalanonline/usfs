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

import org.junit.Test;

import java.security.Security;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

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
  public void digestPersistence() {
    String password = UUID.randomUUID().toString();
    String fileName = UUID.randomUUID().toString();
    assertEquals(Concept.USFS.withPassword(password).digestStr(fileName),
        Concept.USFS.withPassword(password).digestStr(fileName));
  }

  @Test
  public void digestLong() {
    assertEquals("170017", Concept.USFS.radixStr(Concept.USFS.digest(0xF00F)));
    assertEquals(2, Concept.USFS.digest(1).length);
    assertEquals("00000000000000000000000080123456", Concept.MD5.radixStr(Concept.MD5.digest(0x80123456L)));
    assertEquals(16, Concept.MD5.digest(1).length);
    assertEquals("0000000000000000000000000000000000000000000000000000000000000001", Concept.SHA256.radixStr(Concept.SHA256.digest(1)));
    assertEquals(32, Concept.SHA256.digest(1).length);
  }

}
