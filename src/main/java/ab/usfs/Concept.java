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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@AllArgsConstructor
public class Concept {

  public static final Concept USFS = new Concept(Short.SIZE, 3, "SHA-256");

  public static final Concept MD5 = new Concept(128, 4, "MD5");
  public static final Concept SHA1 = new Concept(160, 4, "SHA-1");
  public static final Concept SHA256 = new Concept(256, 4, "SHA-256");

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  private final int digestSize;
  private final int radixSize;
  private final String digestAlgorithm;

  /**
   * Generate meta name for USFS file name.
   * @param fileName
   * @return null if this file name cannot have meta
   */
  public static String v02Meta(String fileName) {
    if ((fileName == null) || (!fileName.matches(".*\\d\\d\\d\\d[02468]"))) return null;
    int lastDigit = Integer.parseInt(fileName.substring(fileName.length() - 1));
    return fileName.substring(0, fileName.length() - 1) + (lastDigit + 1);
  }

  public static String fromInstantToRfc(Instant instant) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
  }

  public static Instant fromRfcToInstant(String s) {
    return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
  }

  public static String toLowercaseHexadecimal(byte[] bytes) {
    return IntStream.range(0, bytes.length).map(i -> bytes[i] & 0xFF).mapToObj(b -> String.format("%02x", b))
        .collect(Collectors.joining());
  }

  public byte[] getBitHash(String s) {
    try {
      return MessageDigest.getInstance(digestAlgorithm).digest(s.getBytes(CHARSET)); // unique MessageDigest.getInstance
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e); // required to be supported
    }
  }

  public String getStringHash(String s) {
    byte[] bytes = getBitHash(s);
    int bits = bytes.length << 3;
    BigInteger bigInteger = new BigInteger(bytes);
    if ((bigInteger.compareTo(BigInteger.ZERO) < 0)) bigInteger = bigInteger.add(BigInteger.ONE.shiftLeft(bits));
    bigInteger = bigInteger.shiftRight(bits - digestSize);
    return pad(bigInteger.toString(1 << radixSize), '0');
  }

  private String pad(String s, char c) {
    int radixSymbols = ((digestSize % radixSize == 0) ? 0 : 1) + (digestSize / radixSize);
    StringBuilder stringBuilder = new StringBuilder(radixSymbols);
    for (int i = s.length(); i < radixSymbols; i++) {
      stringBuilder.append(c);
    }
    return stringBuilder.append(s).toString();
  }

  public String getFileMask() {
    return pad("0", '?').replace("0", "[1357]");
  }

  public UUID stringToUuid(String s) {
    byte[] digest = getBitHash(s);
    ByteBuffer byteBuffer = ByteBuffer.wrap(digest);
    long mostSigBits = byteBuffer.getLong();
    long leastSigBits = byteBuffer.getLong();
    return new UUID(mostSigBits, leastSigBits);
  }

  public int getUnsignedShort(String s) {
    byte[] b = getBitHash(s);
    return ((b[0] & 0xFF) << 8) | (b[1] & 0xFF); // eww, ByteBuffer was better
  }

}
