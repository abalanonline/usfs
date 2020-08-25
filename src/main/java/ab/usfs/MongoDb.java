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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MongoDb extends AbstractStorage {
  // storage abstraction is dangerously powerful
  // this mongo implementation took only 3 hours

  public static final String META_KEY_ID = "_id";
  public static final String META_KEY_PK = "_pk";
  public static final String META_KEY_BINARY = "b";

  private final MongoCollection<Document> collection;

  public MongoDb(MongoDatabase mongoDatabase, Concept concept) throws IOException {
    super(concept);
    DEFAULT_CHUNKSIZE_BYTES = 16383 * 1024;
    collection = mongoDatabase.getCollection("usfs");
    Path root = new Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
      collection.createIndex(new Document(META_KEY_PK, 1));
    }
  }

  @Override
  public byte[] load(byte[] pk, byte[] sk) throws IOException {
    Document item = collection.find(new Document(META_KEY_ID, concat(pk, sk))).first();
    if (item == null) {
      throw new FileNotFoundException();
    }
    return item.get(META_KEY_BINARY, Binary.class).getData();
  }

  @Override
  public void save(byte[] pk, byte[] sk, byte[] b) throws IOException {
    collection.insertOne(new Document(META_KEY_ID, concat(pk, sk))
        .append(META_KEY_PK, pk)
        .append(META_KEY_BINARY, b));
  }

  @Override
  public void delete(byte[] pk, byte[] sk) throws IOException {
    if (collection.deleteOne(new Document(META_KEY_ID, concat(pk, sk))).getDeletedCount() == 0) {
      throw new FileNotFoundException();
    }
  }

  @Override
  public List<byte[]> list(byte[] pk) throws IOException {
    List<byte[]> list = new ArrayList<>();
    for (Document document : collection.find(new Document(META_KEY_PK, pk))) {
      list.add(document.get(META_KEY_BINARY, Binary.class).getData());
    }
    return list;
  }
}
