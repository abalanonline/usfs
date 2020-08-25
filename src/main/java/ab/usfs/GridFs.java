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

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.BsonBinary;
import org.bson.BsonValue;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class GridFs implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_FILE_ID = "FileId";
  public static final String META_KEY_PATH_ID = "PathId";
  public static final InputStream EMPTY = new ByteArrayInputStream(new byte[0]);

  private final GridFSBucket bucket;
  private final Concept concept;

  public GridFs(MongoDatabase mongoDatabase, Concept concept) {
    this.concept = concept;
    this.bucket = GridFSBuckets.create(mongoDatabase);
    Path root = new Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public byte[] getPk(Path path) {
    return concept.digest(path.getP1());
  }

  public String getPkStr(Path path) {
    return concept.digestStr(path.getP1());
  }

  public byte[] getSk(Path path) {
    return concept.digest(path.getP2());
  }

  public String getFpkStr(Path path) {
    return concept.digestStr(path.getP3());
  }

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

  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    for (GridFSFile file : bucket.find(new Document("filename", getFpkStr(path)))) {
      String propertyFileName = file.getMetadata().getString(META_KEY_FILE_NAME);
      if (propertyFileName.isEmpty()) {
        continue; // skip empty names in list, they are technical entries
      }
      list.add(new Path(path.getP3() + '/' + propertyFileName));
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
    byte[] b12;
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      stream.write(getPk(path));
      stream.write(getSk(path));
      b12 = stream.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e); // not expected for ByteArrayOutputStream
    }

    return new BsonBinary(b12);
  }

  GridFSUploadOptions metadata(Path path, boolean folder) {
    return new GridFSUploadOptions().metadata(
        new Document(META_KEY_IS_FOLDER, folder)
            .append(META_KEY_PATH_ID, getPk(path))
            .append(META_KEY_FILE_ID, getSk(path))
            .append(META_KEY_FILE_NAME, path.getFileName())
    );
  }

  @Override
  public Path createFolder(Path path) {
    bucket.uploadFromStream(gridFileId(path), getPkStr(path), EMPTY, metadata(path, true));
    return path;
  }

  @Override
  public long size(Path path) {
    return getFile(path).getLength();
  }

  @Override
  public void delete(Path path) {
    bucket.delete(gridFileId(path));
  }

  @Override
  public InputStream newInputStream(Path path) {
    return bucket.openDownloadStream(gridFileId(path));
  }

  @Override
  public OutputStream newOutputStream(Path path) {
    return bucket.openUploadStream(gridFileId(path), getPkStr(path), metadata(path, false));
  }

}
