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

import ab.Rfc7231;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FileSystemV03 implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";

  private final String mountPoint;
  private final Concept concept;

  @SneakyThrows
  public FileSystemV03(String s, Concept concept) {
    this.concept = concept;
    mountPoint = s;
    Path root = new Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public String getHead(Path path) {
    return '/' + concept.digestStr(path.getP1()) + '/' + concept.digestStr(path.getP2()) + ".0";
  }

  public String getBody(Path path) {
    return '/' + concept.digestStr(path.getP1()) + '/' + concept.digestStr(path.getP2());
  }

  public String getFoot(Path path) {
    return '/' + concept.digestStr(path.getP3());
  }

  @SneakyThrows
  private Properties getProperties(String nioPath) {
    Properties properties = new Properties();
    try (FileInputStream stream = new FileInputStream(nioPath)) {
      properties.load(stream);
    }
    return properties;
  }

  public Properties getProperties(Path path) {
    return getProperties(mountPoint + getHead(path));
  }

  public String getProperty(Path path, String key) {
    return getProperties(path).getProperty(key);
  }

  @SneakyThrows
  public void setProperty(Path path, String... keyvalue) {
    if (keyvalue.length % 2 != 0) throw new IllegalStateException();
    Properties properties = getProperties(path);
    for (int i = 0; i < keyvalue.length; i+=2) {
      properties.setProperty(keyvalue[i], keyvalue[i+1]);
    }
    try (FileOutputStream stream = new FileOutputStream(mountPoint + getHead(path))) {
      properties.store(stream, null);
    }
  }

  @Override
  public boolean exists(Path path) {
    return Files.exists(Paths.get(mountPoint + getHead(path)));
  }

  @Override
  public boolean isFolder(Path path) {
    return exists(path) && !Files.exists(Paths.get(mountPoint + getBody(path)));
    //return exists(path) && Boolean.parseBoolean(getProperty(path, META_KEY_IS_FOLDER));
  }

  @Override
  public boolean isFile(Path path) {
    return exists(path) && Files.exists(Paths.get(mountPoint + getBody(path)));
    //return exists(path) && !Boolean.parseBoolean(getProperty(path, META_KEY_IS_FOLDER));
  }

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String nioFolder = mountPoint + getFoot(path);
    try (DirectoryStream<java.nio.file.Path> directoryStream =
             Files.newDirectoryStream(Paths.get(nioFolder), "*.0")) {
      for (java.nio.file.Path nioPath : directoryStream) {
        String propertyFileName = getProperties(nioFolder + '/' + nioPath.getFileName().toString()).getProperty(META_KEY_FILE_NAME);
        if (propertyFileName.isEmpty()) {
          continue; // skip empty names in list, they are technical entries
        }
        list.add(new Path(path.getP3() + '/' + propertyFileName));
      }
    }
    return list;
  }

  @Override
  public Instant getLastModifiedInstant(Path path) {
    return Rfc7231.instant(getProperty(path, META_KEY_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    setProperty(path, META_KEY_LAST_MODIFIED, Rfc7231.string(instant));
    return path;
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    java.nio.file.Path nioFolder = Paths.get(mountPoint + getFoot(path));
    if (!Files.isDirectory(nioFolder)) { // tree balancing
      Files.createDirectory(nioFolder);
    }
    Files.createFile(Paths.get(mountPoint + getHead(path)));
    setProperty(path,
        META_KEY_IS_FOLDER, Boolean.TRUE.toString(),
        META_KEY_FILE_NAME, path.getFileName(),
        META_KEY_LAST_MODIFIED, Rfc7231.string(Instant.now()));
    return path;
  }

  @SneakyThrows
  @Override
  public long size(Path path) {
    if (!Files.exists(Paths.get(mountPoint + getHead(path)))) {
      throw new NoSuchFileException(path.toString());
    }
    try {
      return Files.size(Paths.get(mountPoint + getBody(path)));
    } catch (NoSuchFileException e) {
      return 0L; // folder
    }
  }

  @SneakyThrows
  @Override
  public void delete(Path path) {
    if (!exists(path)) {
      throw new NoSuchFileException(path.toString());
    }
    if (isFile(path)) {
      Files.delete(Paths.get(mountPoint + getBody(path)));
    } else {
      try {
        Files.delete(Paths.get(mountPoint + getFoot(path)));
      } catch (DirectoryNotEmptyException e) {
        // tree balancing
      }
    }
    Files.delete(Paths.get(mountPoint + getHead(path)));
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return Files.newInputStream(Paths.get(mountPoint + getBody(path)));
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    Files.createFile(Paths.get(mountPoint + getBody(path)));
    Files.createFile(Paths.get(mountPoint + getHead(path)));
    setProperty(path,
        META_KEY_IS_FOLDER, Boolean.FALSE.toString(),
        META_KEY_FILE_NAME, path.getFileName(),
        META_KEY_LAST_MODIFIED, Rfc7231.string(Instant.now()));
    return Files.newOutputStream(Paths.get(mountPoint + getBody(path)));
  }

}
