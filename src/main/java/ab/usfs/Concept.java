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

import ab.cryptography.Digest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Getter
@AllArgsConstructor
public class Concept {

  public static final Concept USFS = new Concept(Short.SIZE, 3, Digest.SHA256);

  public static final Concept MD5 = new Concept(128, 4, Digest.MD5);
  public static final Concept SHA1 = new Concept(160, 4, Digest.SHA1);
  public static final Concept SHA256 = new Concept(256, 4, Digest.SHA256);

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  private final int digestSize;
  private final int radixSize;
  private final Digest digest;

  public byte[] digest(String s) {
    return digest.digest(s.getBytes(CHARSET), digestSize);
  }

  public String radixStr(byte[] bytes) {
    BigInteger bigInteger = new BigInteger(bytes);
    if ((bigInteger.compareTo(BigInteger.ZERO) < 0)) {
      bigInteger = bigInteger.add(BigInteger.ONE.shiftLeft(bytes.length << 3));
    }
    // bigInteger initiated with unsigned bytes
    String s = bigInteger.toString(1 << radixSize);
    // string
    int radixSymbols = ((digestSize % radixSize == 0) ? 0 : 1) + (digestSize / radixSize);
    StringBuilder stringBuilder = new StringBuilder(radixSymbols);
    for (int i = s.length(); i < radixSymbols; i++) {
      stringBuilder.append('0');
    }
    // padded with 0 if shorter than digestSize
    return stringBuilder.append(s).toString();
  }

  public String digestStr(String s) {
    return radixStr(digest(s));
  }

  public byte[] digest(long l) {
    byte[] digest = ByteBuffer.allocate(Long.BYTES).putLong(l).array();
    byte[] result = new byte[digestSize >> 3];
    int min = Math.min(digest.length, result.length);
    System.arraycopy(digest, digest.length - min, result, result.length - min, min);
    return result;
  }

}
