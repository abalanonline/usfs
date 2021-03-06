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

package ab.cryptography;

import org.junit.Test;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class DigestTest {

  private String toHexString(byte[] bytes) {
    return IntStream.range(0, bytes.length).map(i -> bytes[i] & 0xFF).mapToObj(b -> String.format("%02x", b))
        .collect(Collectors.joining());
  }

  private String testMD5(String s) {
    return toHexString(Digest.MD5.digest(s.getBytes()));
  }

  private String testSHA1(String s) {
    return toHexString(Digest.SHA1.digest(s.getBytes()));
  }

  private String testSHA256(String s) {
    return toHexString(Digest.SHA256.digest(s.getBytes()));
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
    assertEquals(0xe3b0, new BigInteger(Digest.SHA256.digest("".getBytes(), 16)).intValue() + 0x10000);
    // https://www.di-mgt.com.au/sha_testvectors.html
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", testSHA256("abc"));
    assertEquals(0xba78, new BigInteger(Digest.SHA256.digest("abc".getBytes(), 16)).intValue() + 0x10000);
    // NESSIE project https://www.cosic.esat.kuleuven.be/nessie/testvectors/hash/sha/Sha-2-256.unverified.test-vectors
    assertEquals("CA978112CA1BBDCAFAC231B39A23DC4DA786EFF8147C4E72B9807785AFEE48BB",
        testSHA256("a").toUpperCase());
    assertEquals("F7846F55CF23E14EEBEAB5B4E1550CAD5B509E3348FBC4EFA3A1413D393CB650",
        testSHA256("message digest").toUpperCase());
    assertEquals("71C480DF93D6AE2F1EFAD1447C66C9525E316218CF51FC8D9ED832F2DAF18B73",
        testSHA256("abcdefghijklmnopqrstuvwxyz").toUpperCase());
  }
}
