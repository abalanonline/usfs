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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;

@SpringBootApplication
public class Application {

  @Bean
  public String gridFs() throws IOException {
    if (true) return "gridFs";

    AmazonDynamoDB client =
        AmazonDynamoDBClientBuilder.standard()
//        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
        .build();

    DynamoDB dynamoDB = new DynamoDB(Regions.US_EAST_1);

    Table table = dynamoDB.getTable("usfs");

    byte[] bytes = new byte[32];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) i;
    }
    Item item = new Item().withPrimaryKey("pk", BigInteger.valueOf(1).toByteArray(), "sk", bytes).withBinary("b", bytes);
    String s = item.toJSON();
    table.putItem(item);

    //TableCollection<ListTablesResult> tables = dynamoDB.listTables();

    JsonParser parser = new JsonFactory().createParser(new File("moviedata.json"));

    JsonNode rootNode = new ObjectMapper().readTree(parser);
    Iterator<JsonNode> iter = rootNode.iterator();

    ObjectNode currentNode;

    while (iter.hasNext()) {
      currentNode = (ObjectNode) rootNode;//iter.next();

      int year = currentNode.path("year").asInt();
      String title = currentNode.path("title").asText();

      try {
        byte[] a = {1,0,2};
        table.putItem(new Item().withPrimaryKey("pk", a, "sk", a).withJSON("info",
            currentNode.path("info").toString()));
        System.out.println("PutItem succeeded: " + year + " " + title);

      }
      catch (Exception e) {
        System.err.println("Unable to add movie: " + year + " " + title);
        System.err.println(e.getMessage());
        break;
      }
      break;
    }
    parser.close();
    return "gridFs";
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
