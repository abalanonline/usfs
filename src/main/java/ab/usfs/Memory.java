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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memory extends AbstractStorage {

  public static final Map<BigInteger, byte[]> EMPTY_SK = Collections.emptyMap();

  private final Map<BigInteger, Map<BigInteger, byte[]>> memory;

  public Memory(Map<BigInteger, Map<BigInteger, byte[]>> memory, Concept concept) throws IOException {
    super(concept);
    this.memory = memory;
    Path root = new Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  @Override
  public byte[] load(byte[] pk, byte[] sk) throws IOException {
    byte[] bytes = memory.getOrDefault(new BigInteger(pk), EMPTY_SK).get(new BigInteger(sk));
    if (bytes == null) {
      throw new NoSuchFileException(null); // null is documented
    }
    return bytes;
  }

  @Override
  public void save(byte[] pk, byte[] sk, byte[] b) throws IOException {
    if (memory.computeIfAbsent(new BigInteger(pk), k -> new HashMap<>()).putIfAbsent(new BigInteger(sk), b) != null) {
      throw new FileAlreadyExistsException(null); // null is documented
    }
  }

  @Override
  public void delete(byte[] pk, byte[] sk) throws IOException {
    if (memory.getOrDefault(new BigInteger(pk), EMPTY_SK).remove(new BigInteger(sk)) == null) {
      throw new NoSuchFileException(null); // null is documented
    }
  }

  @Override
  public List<byte[]> list(byte[] pk) throws IOException {
    return new ArrayList<>(memory.getOrDefault(new BigInteger(pk), EMPTY_SK).values());
  }
}
