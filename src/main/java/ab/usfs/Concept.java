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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@AllArgsConstructor
public class Concept {
  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final Concept MD5 = new Concept(128, 4, "MD5");
  public static final Concept SHA1 = new Concept(160, 4, "SHA-1");
  public static final Concept SHA256 = new Concept(256, 4, "SHA-256");
  public static final Concept USFS = new Concept(16, 3, SHA256.digestAlgorithm);
  private final int digestSize;
  private final int stringBaseSize;
  private final String digestAlgorithm;

  public static String toLowercaseHexadecimal(byte[] bytes) {
    return IntStream.range(0, bytes.length).map(i -> bytes[i] & 0xFF).mapToObj(b -> String.format("%02x", b))
        .collect(Collectors.joining());
  }

  public byte[] getBitHash(String s) {
    try {
      return MessageDigest.getInstance(digestAlgorithm).digest(s.getBytes(CHARSET));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e); // required to be supported
    }
  }

  public BigInteger getStringHash(String s) {
    byte[] bytes = getBitHash(s);
    int bits = bytes.length << 3;
    BigInteger bigInteger = new BigInteger(bytes);
    if ((bigInteger.compareTo(BigInteger.ZERO) < 0)) bigInteger = bigInteger.add(BigInteger.ONE.shiftLeft(bits));
    if (bits > digestSize) {
      bigInteger = bigInteger.shiftRight(bits - digestSize);
    }
    return bigInteger;
  }

  public int getUnsignedShort(String s) {
    return getStringHash(s).intValue();
  }

  public UUID stringToUuid(String s) {
    final MessageDigest messageDigestInstance;
    try {
      messageDigestInstance = MessageDigest.getInstance(digestAlgorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e); // required to be supported
    }
    byte[] digest = messageDigestInstance.digest(s.getBytes(CHARSET));
    ByteBuffer byteBuffer = ByteBuffer.wrap(digest);
    long mostSigBits = byteBuffer.getLong();
    long leastSigBits = byteBuffer.getLong();
    return new UUID(mostSigBits, leastSigBits);
  }

}
