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

import ab.cryptography.Encryption;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class EncryptionTest {

  public static final String[] STRINGS = new String[]{"", "The quick brown fox jumps over the lazy dog",
      "abc", "a", "message digest", "abcdefghijklmnopqrstuvwxyz"};

  private String toHexString(byte[] bytes) {
    return IntStream.range(0, bytes.length).map(i -> bytes[i] & 0xFF).mapToObj(b -> String.format("%02x", b))
        .collect(Collectors.joining());
  }

  @Test
  public void testEncryptionDecryption() {
    String password = UUID.randomUUID().toString();
    Encryption c1 = new Encryption(password.getBytes());
    Encryption c2 = new Encryption(password.getBytes()); // different object with the same password
    for (String test : STRINGS) {
      byte[] e = c1.encrypt(test.getBytes(StandardCharsets.UTF_8));
      assertThat(e.length, greaterThan(test.length()));
      assertThat(new String(c2.decrypt(e), StandardCharsets.UTF_8), equalTo(test));
    }

    assertEquals("fe92a677656fd0167381483f78477fd7", toHexString(new Encryption(new byte[0]).encrypt(new byte[0])));
  }
}
