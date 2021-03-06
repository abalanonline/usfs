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

import ab.Rfc7231;
import lombok.RequiredArgsConstructor;
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
  abstract public byte[] load(byte[] pk, byte[] sk) throws IOException;

  /**
   * @throws FileAlreadyExistsException if already exists
   */
  abstract public void save(byte[] pk, byte[] sk, byte[] b) throws IOException;

  /**
   * @throws NoSuchFileException if not exists
   * @throws FileNotFoundException if not exists
   */
  abstract public void delete(byte[] pk, byte[] sk) throws IOException;

  abstract public List<byte[]> list(byte[] pk) throws IOException;

  public byte[] getPk(Path path) {
    return concept.digest(path.getP1());
  }

  public byte[] getSk(Path path) {
    return concept.digest(path.getP2());
  }

  public byte[] getFpk(Path path) {
    return concept.digest(path.getP3());
  }

  public byte[] loadByte(byte[] pk, byte[] sk) throws IOException {
    return concept.decrypt(load(pk, sk));
  }

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

  public void saveByte(byte[] pk, byte[] sk, byte[] b) throws IOException {
    save(pk, sk, concept.encrypt(b));
  }

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

  public void deleteByte(byte[] pk, byte[] sk) throws IOException {
    delete(pk, sk);
  }

  public List<byte[]> listByte(byte[] pk) throws IOException {
    return list(pk).stream().map(concept::decrypt).collect(Collectors.toList());
  }

  public List<Map<String, String>> listMeta(byte[] pk) throws IOException {
    return listByte(pk).stream().map(this::loadMeta).collect(Collectors.toList());
  }

  @Override
  public boolean exists(Path path) {
    try {
      return loadByte(getPk(path), getSk(path)) != null;
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean isFolder(Path path) {
    try {
      String isFolder = loadMeta(getPk(path), getSk(path)).get(META_KEY_IS_FOLDER);
      return (isFolder != null) && Boolean.parseBoolean(isFolder);
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean isFile(Path path) {
    try {
      String isFolder = loadMeta(getPk(path), getSk(path)).get(META_KEY_IS_FOLDER);
      return (isFolder != null) && !Boolean.parseBoolean(isFolder);
    } catch (NoSuchFileException | FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<Path> listFiles(Path path) throws IOException {
    List<Path> list = new ArrayList<>();

    for (Map<String, String> map : listMeta(getFpk(path))) {
      String propertyFileName = map.get(META_KEY_FILE_NAME);
      if (propertyFileName == null || propertyFileName.isEmpty()) {
        continue; // skip empty names in list, they are technical entries
      }
      list.add(new Path(path.getP3() + '/' + propertyFileName));
    }
    return list;
  }

  @Override
  public Instant getLastModifiedInstant(Path path) throws IOException {
    return Rfc7231.instant(loadMeta(getPk(path), getSk(path)).get(META_KEY_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) throws IOException {
    Map<String, String> meta = loadMeta(getPk(path), getSk(path));
    meta.put(META_KEY_LAST_MODIFIED, Rfc7231.string(instant));
    deleteByte(getPk(path), getSk(path));
    saveMeta(getPk(path), getSk(path), meta);
    return path;
  }

  public Map<String, String> newMeta(boolean isFolder, String fileName, long contentLength, Instant lastModified) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(META_KEY_IS_FOLDER, Boolean.valueOf(isFolder).toString());
    map.put(META_KEY_FILE_NAME, fileName);
    map.put(META_KEY_CONTENT_LENGTH, Long.toString(contentLength));
    map.put(META_KEY_LAST_MODIFIED, Rfc7231.string(lastModified));
    return map;
  }

  @Override
  public Path createFolder(Path path) throws IOException {
    saveMeta(getPk(path), getSk(path), newMeta(true, path.getFileName(), 0L, Instant.now()));
    return path;
  }

  @Override
  public long size(Path path) throws IOException {
    Map<String, String> meta = loadMeta(getPk(path), getSk(path));
    if (Boolean.parseBoolean(meta.get(META_KEY_IS_FOLDER))) { // not null and true
      return 0L;
    }
    return Long.parseLong(meta.get(META_KEY_CONTENT_LENGTH)); // not folder and this key is expected
  }

  @Override
  public void delete(Path path) throws IOException {
    deleteByte(getPk(path), getSk(path));
    for (int chunkCount = 0; chunkCount < Integer.MAX_VALUE; chunkCount++) { // delete file chunks, fast
      byte[] chunkCountBit = concept.digest(chunkCount);
      try {
        deleteByte(getFpk(path), chunkCountBit);
      } catch (NoSuchFileException | FileNotFoundException e) {
        break;
      }
    }
    // for (listByte(getFk(path))) // delete file chunks, fail-safe
  }

  public byte[] concat(byte[] a, byte[] b) {
    // https://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
    byte[] c = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  @Override
  public InputStream newInputStream(Path path) {
    return new GridInputStream(path);
  }

  @Override
  public OutputStream newOutputStream(Path path) {
    return new GridOutputStream(path);
  }

  private static final Logger collisionLogger = org.slf4j.LoggerFactory.getLogger("ab.usfs.Collision");
  private static Map<String, String> collision = new HashMap<>();

  public class GridOutputStream extends OutputStream {
    private final byte[] pk;
    private final Path path;
    private long fileSize;
    private long chunkCount;
    private byte[] buf = new byte[DEFAULT_CHUNKSIZE_BYTES];
    private int pos = 0; // java.io.ByteArrayInputStream naming

    public GridOutputStream(Path path) {
      this.pk = getFpk(path);
      this.path = path;
      if (collisionLogger.isDebugEnabled()) {
        String key = concept.digestStr(path.getP3());
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
      while (len > 0) {
        int bytesToCopy = Math.min(DEFAULT_CHUNKSIZE_BYTES - pos, len);
        System.arraycopy(b, off, buf, pos, bytesToCopy);
        pos += bytesToCopy;
        off += bytesToCopy;
        len -= bytesToCopy;
        fileSize += bytesToCopy;
        if (pos >= DEFAULT_CHUNKSIZE_BYTES) {
          saveByte(pk, concept.digest(chunkCount), buf);
          buf = new byte[DEFAULT_CHUNKSIZE_BYTES]; // thread-safe
          pos = 0;
          chunkCount++;
        }
      }
    }

    @Override
    public synchronized void close() throws IOException {
      super.close();
      if (buf == null) {
        return;
      }
      if (pos > 0) {
        saveByte(pk, concept.digest(chunkCount), Arrays.copyOf(buf, pos));
        pos = 0;
      }
      saveMeta(getPk(path), getSk(path), newMeta(false, path.getFileName(), fileSize, Instant.now()));
      buf = null;
    }
  }

  public class GridInputStream extends InputStream {
    private final byte[] pk;
    private long chunkCount;
    private byte[] buf;
    private int pos;

    public GridInputStream(Path path) {
      this.pk = getFpk(path);
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
      if (buf == null || pos >= buf.length) {
        try {
          buf = loadByte(pk, concept.digest(chunkCount));
        } catch (NoSuchFileException | FileNotFoundException e) {
          return -1;
        }
        chunkCount++;
        pos = 0;
      }
      int bytesToCopy = Math.min(buf.length - pos, len);
      System.arraycopy(buf, pos, b, off, bytesToCopy);
      pos += bytesToCopy;
      return bytesToCopy;
    }
  }
}
