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

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import lombok.SneakyThrows;
import org.bson.BsonBinary;
import org.bson.BsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamoDb implements Storage {

  private static final int DEFAULT_CHUNKSIZE_BYTES = 255 * 1024;
  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_CONTENT_LENGTH = "Content-Length";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";
  public static final String META_KEY_FILE_ID = "FileId";
  public static final String META_KEY_PATH_ID = "PathId";
  public static final String META_KEY_PK = "pk";
  public static final String META_KEY_SK = "sk";
  public static final String META_KEY_BINARY = "b";
  public static final InputStream EMPTY = new ByteArrayInputStream(new byte[0]);

  private final Table table;
  private final Concept concept;

  @SneakyThrows
  public DynamoDb(Table table, Concept concept) {
    this.concept = concept;
    this.table = table;
    Path root = new Path("/", concept);
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public static DynamoDb mount(Table table) {
    return new DynamoDb(table, Concept.USFS);
  }

  @SneakyThrows
  @Override
  public boolean exists(Path path) {
    return getFile(path) != null;
  }

  @Override
  public boolean isFolder(Path path) {
    return getFile(path).getBoolean(META_KEY_IS_FOLDER);
  }

  @Override
  public boolean isFile(Path path) {
    return !getFile(path).getBoolean(META_KEY_IS_FOLDER);
  }

  public Item getFile(Path path) {
    GetItemSpec spec = new GetItemSpec().withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit());
    try {
      return table.getItem(spec);
    } catch (Error e) {
      return null;
    }
  }

  public List<Item> listItems(byte[] pk) {
    QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#yr = :yyyy")
        .withNameMap(new NameMap().with("#yr", META_KEY_PK))
        .withValueMap(new ValueMap().withBinary(":yyyy", pk));
    ItemCollection<QueryOutcome> items = table.query(querySpec);
    IteratorSupport<Item, QueryOutcome> iterator = items.iterator();
    List<Item> list = new ArrayList<>();
    while (iterator.hasNext()) {
      list.add(iterator.next());
    }
    return list;
  }

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String usfsFolder = path.toString().equals("/") ? path.toString() : (path.toString() + '/');

    for (Item item : listItems(path.getV3().getBit())) {
      String propertyFileName = item.getString(META_KEY_FILE_NAME);
      if (propertyFileName.isEmpty()) {
        continue; // skip empty names in list, they are technical entries
      }
      list.add(new Path(usfsFolder + propertyFileName, this.concept));
    }
    return list;
  }

  @Override
  public Instant getLastModifiedInstant(Path path) {
    return Concept.fromRfcToInstant(getFile(path).getString(META_KEY_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    return path;
  }

  BsonValue gridFileId(Path path) {
    return new BsonBinary(path.getB12());
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    table.putItem(new Item()
        .withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit())
        .withBoolean(META_KEY_IS_FOLDER, true)
        .withString(META_KEY_FILE_NAME, path.getFileName())
        .withLong(META_KEY_CONTENT_LENGTH, 0L)
        .withString(META_KEY_LAST_MODIFIED, Concept.fromInstantToRfc(Instant.now())));
    return path;
  }

  @SneakyThrows
  @Override
  public long size(Path path) {
    Item item = getFile(path);
    return item.getBoolean(META_KEY_IS_FOLDER) ? 0L : item.getLong(META_KEY_CONTENT_LENGTH);
  }

  @Override
  public void delete(Path path) {
    table.deleteItem(new DeleteItemSpec()
        .withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit()));
    for (Item item : listItems(path.getV3().getBit())) { // delete file chunks
      table.deleteItem(new DeleteItemSpec()
          .withPrimaryKey(META_KEY_PK, path.getV3().getBit(), META_KEY_SK, item.getBinary(META_KEY_SK)));
    }
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return new DynamoInputStream(path);
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    return new DynamoOutputStream(path);
  }

  public class DynamoOutputStream extends OutputStream {
    private final byte[] pk;
    private final Path path;
    private long count;
    private long chunkCount;
    private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    public DynamoOutputStream(Path path) {
      this.pk = path.getV3().getBit();
      this.path = path;
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
        table.putItem(new Item()
            .withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, concept.vector(chunkCount).getBit())
            .withBinary(META_KEY_BINARY, Arrays.copyOf(bytes, DEFAULT_CHUNKSIZE_BYTES)));
        chunkCount++;
        bytes = Arrays.copyOfRange(bytes, DEFAULT_CHUNKSIZE_BYTES, bytes.length);
        byteStream = new ByteArrayOutputStream(bytes.length);
        byteStream.write(bytes);
      }
    }

    @Override
    public void close() throws IOException {
      super.close();
      table.putItem(new Item()
          .withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, concept.vector(chunkCount).getBit())
          .withBinary(META_KEY_BINARY, byteStream.toByteArray()));
      table.putItem(new Item()
          .withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit())
          .withBoolean(META_KEY_IS_FOLDER, false)
          .withString(META_KEY_FILE_NAME, path.getFileName())
          .withLong(META_KEY_CONTENT_LENGTH, count)
          .withString(META_KEY_LAST_MODIFIED, Concept.fromInstantToRfc(Instant.now())));
    }
  }

  public class DynamoInputStream extends InputStream {
    private final byte[] pk;
    private int count;
    private long chunkCount;
    private byte[] bytes;

    public DynamoInputStream(Path path) {
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
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytes == null || bytes.length <= count) {
        Item item = table.getItem(new GetItemSpec()
            .withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, concept.vector(chunkCount).getBit()));
        if (item == null) {
          return -1;
        }
        chunkCount++;
        count = 0;
        bytes = item.getBinary(META_KEY_BINARY);
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
