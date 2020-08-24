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

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {

  // https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
  // required to support AES/ECB/PKCS5Padding (128)
  public static final String ALGORITHM = "AES";
  public static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
  public static final int KEY_SIZE = 128;
  public static final Digest DIGEST = Digest.SHA256;

  private final Cipher encryptCipher;
  private final Cipher decryptCipher;

  @SneakyThrows
  public Encryption(byte[] password) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(DIGEST.digest(password, KEY_SIZE), ALGORITHM);
    encryptCipher = Cipher.getInstance(TRANSFORMATION);
    encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
    decryptCipher = Cipher.getInstance(TRANSFORMATION);
    decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
  }

  @SneakyThrows
  public byte[] encrypt(byte[] b) {
    return encryptCipher.doFinal(b);
  }

  @SneakyThrows
  public byte[] decrypt(byte[] b) {
    return decryptCipher.doFinal(b);
  }
}
