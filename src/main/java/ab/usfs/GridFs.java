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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static ab.usfs.Concept.fromInstantToRfc;

public class GridFs implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";
  public static final String META_KEY_FILE_ID = "FileId";
  public static final String META_KEY_PATH_ID = "PathId";
  public static final InputStream EMPTY = new ByteArrayInputStream(new byte[0]);

  private final GridFSBucket bucket;
  private final Concept concept;

  @SneakyThrows
  public GridFs(GridFSBucket bucket, Concept concept) {
    this.concept = concept;
    this.bucket = bucket;
    Path root = new Path("/", concept);
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public static GridFs mount(GridFSBucket bucket) {
    return new GridFs(bucket, Concept.USFS);
  }

  public String getProperty(Path path, String key) {
    GridFSFile file = getFile(path);
    Document metadata = (file == null) ? null : file.getMetadata();
    return metadata == null ? null : metadata.getString(key);
  }

  public void setProperty(Path path, String... keyvalue) {
  }

  @SneakyThrows
  @Override
  public boolean exists(Path path) {
    return getFile(path) != null;
  }

  @Override
  public boolean isFolder(Path path) {
    return getFile(path).getMetadata().getBoolean(META_KEY_IS_FOLDER);
  }

  @Override
  public boolean isFile(Path path) {
    return !getFile(path).getMetadata().getBoolean(META_KEY_IS_FOLDER);
  }

  public GridFSFile getFile(Path path) {
    for (GridFSFile file : bucket.find(new Document("_id", gridFileId(path)))) {
      return file;
    }
    return null;
  }

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String usfsFolder = path.toString().equals("/") ? path.toString() : (path.toString() + '/');
    for (GridFSFile file : bucket.find(new Document("filename", path.getV3().getStr()))) {
      String propertyFileName = file.getMetadata().getString(META_KEY_FILE_NAME);
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

  GridFSUploadOptions metadata(Path path, boolean folder) {
    return new GridFSUploadOptions().metadata(
        new Document(META_KEY_IS_FOLDER, folder)
            .append(META_KEY_PATH_ID, path.getV1().getBit())
            .append(META_KEY_FILE_ID, path.getV2().getBit())
            .append(META_KEY_FILE_NAME, path.getFileName())
    );
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    bucket.uploadFromStream(gridFileId(path), path.getV1().getStr(), EMPTY, metadata(path, true));
    return path;
  }

  @SneakyThrows
  @Override
  public long size(Path path) {
    return getFile(path).getLength();
  }

  @Override
  public void delete(Path path) {
    bucket.delete(gridFileId(path));
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return bucket.openDownloadStream(gridFileId(path));
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    return bucket.openUploadStream(gridFileId(path), path.getV1().getStr(), metadata(path, false));
  }

}
