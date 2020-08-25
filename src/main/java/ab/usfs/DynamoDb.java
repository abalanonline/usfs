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
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DynamoDb extends AbstractStorage {

  public static final String META_KEY_PK = "pk";
  public static final String META_KEY_SK = "sk";
  public static final String META_KEY_BINARY = "b";

  private final Table table;

  public DynamoDb(Table table, Concept concept) throws IOException {
    super(concept);
    DEFAULT_CHUNKSIZE_BYTES = 399 * 1024;
    this.table = table;
    Path root = new Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  @Override
  public byte[] loadByte(byte[] pk, byte[] sk) throws IOException {
    GetItemSpec spec = new GetItemSpec().withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, sk);
    Item item = table.getItem(spec);
    if (item == null) {
      throw new FileNotFoundException();
    }
    return item.getBinary(META_KEY_BINARY);
  }

  @Override
  public void saveByte(byte[] pk, byte[] sk, byte[] b) throws IOException {
    table.putItem(new Item()
        .withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, sk)
        .withBinary(META_KEY_BINARY, b));
  }

  @Override
  public void deleteByte(byte[] pk, byte[] sk) throws IOException {
    try {
      table.deleteItem(new DeleteItemSpec()
          .withPrimaryKey(META_KEY_PK, pk, META_KEY_SK, sk)
          .withConditionExpression(META_KEY_SK + " = :chunk")
          .withValueMap(new ValueMap().withBinary(":chunk", sk)));
    } catch (ConditionalCheckFailedException e) {
      throw new FileNotFoundException();
    }
  }

  @Override
  public List<byte[]> listByte(byte[] pk) throws IOException {
    QuerySpec querySpec = new QuerySpec().withKeyConditionExpression(META_KEY_PK + " = :pk")
        .withValueMap(new ValueMap().withBinary(":pk", pk));
    ItemCollection<QueryOutcome> items = table.query(querySpec);
    IteratorSupport<Item, QueryOutcome> iterator = items.iterator();
    List<byte[]> list = new ArrayList<>();
    while (iterator.hasNext()) {
      list.add(iterator.next().getBinary(META_KEY_BINARY));
    }
    return list;
  }
}
