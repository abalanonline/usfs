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

import ab.usfs.FileSystem;
import ab.usfs.GridFs;
import ab.usfs.Storage;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import lombok.SneakyThrows;
import org.apache.ftpserver.FtpServerFactory;
import org.springframework.stereotype.Service;

@Service
public class FtpServer {

  @SneakyThrows
  public FtpServer() {
    final String mongoUrl = "mongodb://localhost:27017/usfs";
    ConnectionString connectionString = new ConnectionString(mongoUrl);
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase mongoDatabase = mongoClient.getDatabase(connectionString.getDatabase());
    GridFSBucket gridFs = GridFSBuckets.create(mongoDatabase);

    FtpServerFactory factory = new FtpServerFactory();
    factory.setUserManager(NullUser.MANAGER);
    //Storage usfsMedium = FileSystem.mount("target");
    Storage usfsMedium = GridFs.mount(gridFs);
    factory.setFileSystem(new UsfsFtpStorage(usfsMedium));
    factory.createServer().start();
  }

}
