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

import ab.usfs.Concept;
import ab.usfs.DynamoDb;
import ab.usfs.FileSystem;
import ab.usfs.GridFs;
import ab.usfs.Memory;
import ab.usfs.MongoDb;
import ab.usfs.Storage;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

@Slf4j
@SpringBootApplication
public class Application {

  @Bean
  public Concept encryptedConcept() {
    return Concept.USFS;
  }

  @ConditionalOnProperty("dynamo")
  @Bean
  public Storage dynamoDb(@Autowired Concept concept, @Value("${dynamo}") String url) {
    log.info("Storage: DynamoDB");
    // Table name: usfs
    // Primary partition key: pk (Binary)
    // Primary sort key: sk (Binary)
    return new DynamoDb(new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient()).getTable("usfs"), concept);
  }

  @ConditionalOnProperty("mongo")
  @Bean
  public Storage mongoDb(@Autowired Concept concept, @Value("${mongo}") String url) {
    final String mongoUrl = url.startsWith("mongodb://") ? url : "mongodb://localhost:27017/usfs";
    log.info("Storage: MongoDB, url: " + mongoUrl);
    ConnectionString connectionString = new ConnectionString(mongoUrl);
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase mongoDatabase = mongoClient.getDatabase(connectionString.getDatabase());
//    return GridFs.mount(mongoDatabase);
    return new MongoDb(mongoDatabase, concept);
  }

  @ConditionalOnProperty("folder")
  @Bean
  public Storage fileFolder(@Autowired Concept concept, @Value("${folder}") String folder) {
    log.info("Storage: file system, folder: " + folder);
    return new FileSystem(folder, concept);
  }

  @ConditionalOnMissingBean
  @Bean
  public Storage memoryStorage(@Autowired Concept concept) {
    log.warn("Storage: not configured, using memory");
    return new Memory(new HashMap<>(), concept);
  }

  @Bean
  public FtpServer ftpServer(@Autowired Storage usfsMedium) throws FtpException {
    FtpServerFactory factory = new FtpServerFactory();
    factory.setUserManager(NullUser.MANAGER);
    factory.setFileSystem(new UsfsFtpStorage(usfsMedium));
    FtpServer ftpServer = factory.createServer();
    ftpServer.start();
    return ftpServer;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  private static final Logger profilerLogger = org.slf4j.LoggerFactory.getLogger("ab.Profiler");
  private static Instant tick = Instant.now();
  private static Instant tock = Instant.now();
  public static void tick() { // 15 lines profiler
    if (!profilerLogger.isDebugEnabled()) {
      return;
    }
    Instant now = Instant.now();
    if (Duration.between(tick, now).getSeconds() > 5) {
      profilerLogger.debug("busy " + tock + " - " + tick + " - " + Duration.between(tock, tick).getSeconds() + " s");
      profilerLogger.debug("idle " + tick + " - " + now);
      tock = now;
    }
    tick = now;
  }

}
