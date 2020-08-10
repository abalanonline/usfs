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

package ab;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.BsonBinary;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@SpringBootApplication
public class Application {

  @Bean
  public GridFSBucket gridFs() {
    final String mongoUrl = "mongodb://localhost:27017/usfs";
    ConnectionString connectionString = new ConnectionString(mongoUrl);
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase mongoDatabase = mongoClient.getDatabase(connectionString.getDatabase());
    GridFSBucket gridFs = GridFSBuckets.create(mongoDatabase);

    //for (GridFSFile file : gridFs.find(new Document("metadata", new Document("PathId", new byte[]{0,0})))) {
    for (GridFSFile file : gridFs.find(new Document("filename", "a"))) {
      String s = file.toString();
      s = s + ".";
    }

//    byte[] key1 = "filename.ext".getBytes();
//    BsonValue key2 = new BsonBinary(key1);
//
//    gridFs.uploadFromStream("filename.ext", new ByteArrayInputStream("quick brown fox".getBytes()));
//    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//    gridFs.downloadToStream(key2, byteArrayOutputStream);
//
//    Document filter = new Document("filename", "find.txt");
//    Iterable<GridFSFile> list = gridFs.find(filter);
//    for (GridFSFile file : list) {
//      String s = file.toString();
//      s = s + ".";
//    }
    // connect to mongo instance
    //final String uriString = "mongodb://$[username]:$[password]@$[hostlist]/$[database]?authSource=$[authSource]";
    // switch to test database, inventory collection
    //MongoCollection<Document> collection = mongoDB.getCollection("inventory");
    //Document canvas = new Document("item", "canvas")
    //    .append("qty", 100)
    //    .append("tags", singletonList("cotton"));
    //Document size = new Document("h", 28)
    //    .append("w", 35.5)
    //    .append("uom", "cm");
    //canvas.put("size", size);
    //collection.insertOne(canvas);
    return gridFs;
    //mongoClient.close();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
