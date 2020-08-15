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
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.SneakyThrows;
import org.bson.BsonBinary;
import org.bson.BsonValue;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynamoDb implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";
  public static final String META_KEY_FILE_ID = "FileId";
  public static final String META_KEY_PATH_ID = "PathId";
  public static final String META_KEY_PK = "pk";
  public static final String META_KEY_SK = "sk";
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

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String usfsFolder = path.toString().equals("/") ? path.toString() : (path.toString() + '/');

    QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#yr = :yyyy")
        .withNameMap(new NameMap().with("#yr", "pk"))
        .withValueMap(new ValueMap().withBinary(":yyyy", path.getV3().getBit()));
    ItemCollection<QueryOutcome> items = table.query(querySpec);
    IteratorSupport<Item, QueryOutcome> iterator = items.iterator();
    while (iterator.hasNext()) {
      Item item = iterator.next();
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
    return Instant.EPOCH;
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    return path;
  }

  BsonValue gridFileId(Path path) {
    return new BsonBinary(path.getB12());
  }

  Item metadata(Path path, boolean folder) {
    return new Item().withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit())
        .withBoolean(META_KEY_IS_FOLDER, folder)
        .withString(META_KEY_FILE_NAME, path.getFileName())
        .withBinary("b", new byte[0]);
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    table.putItem(metadata(path, true));
    return path;
  }

  @SneakyThrows
  @Override
  public long size(Path path) {
    return getFile(path).getBinary("b").length;
  }

  @Override
  public void delete(Path path) {
    DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
        .withPrimaryKey(META_KEY_PK, path.getV1().getBit(), META_KEY_SK, path.getV2().getBit());
    table.deleteItem(deleteItemSpec);
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return null;//bucket.openDownloadStream(gridFileId(path));
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    return null;//bucket.openUploadStream(gridFileId(path), path.getV1().getStr(), metadata(path, false));
  }

}
