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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractStorage implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_CONTENT_LENGTH = "Content-Length";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";

  private final Concept concept;
  protected int DEFAULT_CHUNKSIZE_BYTES = 255 * 1024; // com.mongodb.client.gridfs.GridFSBucketImpl

  /**
   * @throws NoSuchFileException if not exists
   * @throws FileNotFoundException if not exists
   */
  abstract public byte[] loadByte(byte[] pk, byte[] sk) throws IOException;

  public Map<String, String> loadMeta(byte[] pk, byte[] sk) throws IOException {
    return loadMeta(loadByte(pk, sk));
  }

  public Map<String, String> loadMeta(byte[] b) {
    Properties properties = new Properties();
    try (InputStream stream = new ByteArrayInputStream(b)) {
      properties.load(stream);
    } catch (IOException e) {
      throw new UncheckedIOException(e); // IOException is not expected for ByteArrayInputStream
    }
    Map<String, String> map = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
    }
    return map;
  }

  /**
   * @throws FileAlreadyExistsException if already exists
   */
  abstract public void saveByte(byte[] pk, byte[] sk, byte[] b) throws IOException;

  public void saveMeta(byte[] pk, byte[] sk, Map<String, String> meta) throws IOException {
    Properties properties = new Properties();
    for (Map.Entry<String, String> entry : meta.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      properties.store(stream, null);
      saveByte(pk, sk, stream.toByteArray());
    }
  }

  /**
   * @throws NoSuchFileException if not exists
   * @throws FileNotFoundException if not exists
   */
  abstract public void deleteByte(byte[] pk, byte[] sk) throws IOException;

  abstract public List<byte[]> listByte(byte[] pk) throws IOException;

  public List<Map<String, String>> listMeta(byte[] pk) throws IOException {
    return listByte(pk).stream().map(this::loadMeta).collect(Collectors.toList());
  }

  @SneakyThrows
  @Override
  public boolean exists(Path path) {
    try {
      return loadByte(path.getV1().getBit(), path.getV2().getBit()) != null;
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    }
  }

  @SneakyThrows
  @Override
  public boolean isFolder(Path path) {
    try {
      String isFolder = loadMeta(path.getV1().getBit(), path.getV2().getBit()).get(META_KEY_IS_FOLDER);
      return (isFolder != null) && Boolean.parseBoolean(isFolder);
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    }
  }

  @SneakyThrows
  @Override
  public boolean isFile(Path path) {
    try {
      String isFolder = loadMeta(path.getV1().getBit(), path.getV2().getBit()).get(META_KEY_IS_FOLDER);
      return (isFolder != null) && !Boolean.parseBoolean(isFolder);
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    }
  }

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String usfsFolder = path.toString().equals("/") ? path.toString() : (path.toString() + '/');

    for (Map<String, String> map : listMeta(path.getV3().getBit())) {
      String propertyFileName = map.get(META_KEY_FILE_NAME);
      if (propertyFileName == null || propertyFileName.isEmpty()) {
        continue; // skip empty names in list, they are technical entries
      }
      list.add(new Path(usfsFolder + propertyFileName, concept));
    }
    return list;
  }

  @Override
  public Instant getLastModifiedInstant(Path path) throws IOException {
    return Concept.fromRfcToInstant(
        loadMeta(path.getV1().getBit(), path.getV2().getBit())
            .get(META_KEY_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    return path;
  }

  public Map<String, String> newMeta(boolean isFolder, String fileName, long contentLength, Instant lastModified) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(META_KEY_IS_FOLDER, Boolean.valueOf(isFolder).toString());
    map.put(META_KEY_FILE_NAME, fileName);
    map.put(META_KEY_CONTENT_LENGTH, Long.toString(contentLength));
    map.put(META_KEY_LAST_MODIFIED, Concept.fromInstantToRfc(lastModified));
    return map;
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    saveMeta(path.getV1().getBit(), path.getV2().getBit(), newMeta(true, path.getFileName(), 0L, Instant.now()));
    return path;
  }

  @Override
  public long size(Path path) throws IOException {
    Map<String, String> meta = loadMeta(path.getV1().getBit(), path.getV2().getBit());
    if (Boolean.parseBoolean(meta.get(META_KEY_IS_FOLDER))) { // not null and true
      return 0L;
    }
    return Long.parseLong(meta.get(META_KEY_CONTENT_LENGTH)); // not folder and this key is expected
  }

  @SneakyThrows
  @Override
  public void delete(Path path) {
    deleteByte(path.getV1().getBit(), path.getV2().getBit());
    for (int chunkCount = 0; chunkCount < Integer.MAX_VALUE; chunkCount++) { // delete file chunks, fast
      byte[] chunkCountBit = concept.vector(chunkCount).getBit();
      try {
        deleteByte(path.getV3().getBit(), chunkCountBit);
      } catch (NoSuchFileException | FileNotFoundException e) {
        break;
      }
    }
    // for (listByte(path.getV3().getBit())) // delete file chunks, fail-safe
  }

  public byte[] concat(byte[] a, byte[] b) {
    // https://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
    byte[] c = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return new GridInputStream(path);
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    return new GridOutputStream(path);
  }

  private static final Logger collisionLogger = org.slf4j.LoggerFactory.getLogger("ab.usfs.Collision");
  private static Map<String, String> collision = new HashMap<>();

  public class GridOutputStream extends OutputStream {
    private final byte[] pk;
    private final Path path;
    private long count;
    private long chunkCount;
    private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    public GridOutputStream(Path path) {
      this.pk = path.getV3().getBit();
      this.path = path;
      if (collisionLogger.isDebugEnabled()) {
        String key = path.getV3().getStr();
        if (collision.containsKey(key)) {
          collisionLogger.debug(key + " - " + collision.get(key));
          collisionLogger.debug(key + " - " + path);
        }
        collision.put(key, path.toString());
      }
    }

    @Override
    public synchronized void write(int b) throws IOException {
      // I really want to throw exception here
      // Legacy write not supported, something like that
      byte[] b1 = new byte[1];
      b1[0] = (byte)b;
      this.write(b1);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
      byteStream.write(b, off, len);
      count += len;
      while (byteStream.size() > DEFAULT_CHUNKSIZE_BYTES) {
        byte[] bytes = byteStream.toByteArray();
        saveByte(pk, concept.vector(chunkCount).getBit(), Arrays.copyOf(bytes, DEFAULT_CHUNKSIZE_BYTES));
        chunkCount++;
        bytes = Arrays.copyOfRange(bytes, DEFAULT_CHUNKSIZE_BYTES, bytes.length);
        byteStream = new ByteArrayOutputStream(bytes.length);
        byteStream.write(bytes);
      }
    }

    @Override
    public synchronized void close() throws IOException {
      super.close();
      if (byteStream == null) {
        return; // TODO: 2020-08-23 get rid of ByteStream
      }
      if (byteStream.size() > 0) {
        saveByte(pk, concept.vector(chunkCount).getBit(), byteStream.toByteArray());
      }
      saveMeta(path.getV1().getBit(), path.getV2().getBit(), newMeta(false, path.getFileName(), count, Instant.now()));
      byteStream = null;
    }
  }

  public class GridInputStream extends InputStream {
    private final byte[] pk;
    private int count;
    private long chunkCount;
    private byte[] bytes;

    public GridInputStream(Path path) {
      this.pk = path.getV3().getBit();
    }

    @Override
    public synchronized int read() throws IOException {
      // Legacy read not supported
      byte[] b1 = new byte[1];
      int n = this.read(b1);
      if (n == 1)
        return b1[0] & 0xff;
      return -1;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
      if (bytes == null || bytes.length <= count) {
        try {
          bytes = loadByte(pk, concept.vector(chunkCount).getBit());
        } catch (NoSuchFileException | FileNotFoundException e) {
          return -1;
        }
        chunkCount++;
        count = 0;
      }
      if (len > bytes.length - count) {
        len = bytes.length - count;
      }
      System.arraycopy(bytes, count, b, off, len);
      count += len;
      return len;
    }
  }
}
