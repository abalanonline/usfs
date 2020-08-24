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

import lombok.RequiredArgsConstructor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
public enum Digest {
  // https://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
  // required to support MD5, SHA-1, SHA-256
  MD5("MD5"), SHA1("SHA-1"), SHA256("SHA-256");
  private final String algorithm;

  public byte[] digest(byte[] input) {
    try {
      return MessageDigest.getInstance(algorithm).digest(input); // unique MessageDigest.getInstance
    } catch (NoSuchAlgorithmException e) {
      // full stop here, required to be supported (╯°□°)╯︵ ┻━┻
      throw new Error("Every implementation of the Java platform is required to support " + algorithm, e);
    }
  }

  public byte[] digest(byte[] input, int bitSize) {
    if ((bitSize & 0b00000111) != 0) {
      throw new IllegalStateException("Unsupported digest size " + bitSize);
    }
    byte[] digest = digest(input);
    byte[] result = new byte[bitSize >> 3];
    System.arraycopy(digest, 0, result, 0, Math.min(digest.length, result.length));
    return result;
  }
}
