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

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ab.usfs.Concept.fromInstantToRfc;
import static ab.usfs.Concept.fromRfcToInstant;

public class FileSystem implements Storage {

  public static final String META_KEY_FILE_NAME = "FileName";
  public static final String META_KEY_IS_FOLDER = "IsFolder";
  public static final String META_KEY_LAST_MODIFIED = "Last-Modified";

  private final String mountPoint;
  private final Concept concept;

  @SneakyThrows
  public FileSystem(String s, Concept concept) {
    this.concept = concept;
    mountPoint = s;
    Path root = new Path("/", concept);
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public static FileSystem mount(String s) {
    return new FileSystem(s, Concept.USFS);
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
    return getProperties(mountPoint + path.getHead());
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
    try (FileOutputStream stream = new FileOutputStream(mountPoint + path.getHead())) {
      properties.store(stream, null);
    }
  }

  @Override
  public boolean exists(Path path) {
    return Files.exists(Paths.get(mountPoint + path.getHead()));
  }

  @Override
  public boolean isFolder(Path path) {
    return exists(path) && !Files.exists(Paths.get(mountPoint + path.getBody()));
    //return exists(path) && Boolean.parseBoolean(getProperty(path, META_KEY_IS_FOLDER));
  }

  @Override
  public boolean isFile(Path path) {
    return exists(path) && Files.exists(Paths.get(mountPoint + path.getBody()));
    //return exists(path) && !Boolean.parseBoolean(getProperty(path, META_KEY_IS_FOLDER));
  }

  @SneakyThrows
  @Override
  public List<Path> listFiles(Path path) {
    List<Path> list = new ArrayList<>();
    String nioFolder = mountPoint + path.getFoot();
    String usfsFolder = path.toString().equals("/") ? path.toString() : (path.toString() + '/');
    try (DirectoryStream<java.nio.file.Path> directoryStream =
             Files.newDirectoryStream(Paths.get(nioFolder), concept.getFileMask() + ".0")) {
      for (java.nio.file.Path nioPath : directoryStream) {
        String propertyFileName = getProperties(nioFolder + '/' + nioPath.getFileName().toString()).getProperty(META_KEY_FILE_NAME);
        if (propertyFileName.isEmpty()) {
          continue; // skip empty names in list, they are technical entries
        }
        list.add(new Path(usfsFolder + propertyFileName, this.concept));
      }
    }
    return list;
  }

  @Override
  public Instant getLastModifiedInstant(Path path) {
    return fromRfcToInstant(getProperty(path, META_KEY_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    setProperty(path, META_KEY_LAST_MODIFIED, fromInstantToRfc(instant));
    return path;
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    java.nio.file.Path nioFolder = Paths.get(mountPoint + path.getFoot());
    if (!Files.isDirectory(nioFolder)) { // tree balancing
      Files.createDirectory(nioFolder);
    }
    Files.createFile(Paths.get(mountPoint + path.getHead()));
    setProperty(path,
        META_KEY_IS_FOLDER, Boolean.TRUE.toString(),
        META_KEY_FILE_NAME, path.getFileName(),
        META_KEY_LAST_MODIFIED, fromInstantToRfc(Instant.now()));
    return path;
  }

  @SneakyThrows
  @Override
  public long size(Path path) {
    if (!Files.exists(Paths.get(mountPoint + path.getHead()))) {
      throw new NoSuchFileException(path.toString());
    }
    try {
      return Files.size(Paths.get(mountPoint + path.getBody()));
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
      Files.delete(Paths.get(mountPoint + path.getBody()));
    } else {
      try {
        Files.delete(Paths.get(mountPoint + path.getFoot()));
      } catch (DirectoryNotEmptyException e) {
        // tree balancing
      }
    }
    Files.delete(Paths.get(mountPoint + path.getHead()));
  }

  @SneakyThrows
  @Override
  public InputStream newInputStream(Path path) {
    return Files.newInputStream(Paths.get(mountPoint + path.getBody()));
  }

  @SneakyThrows
  @Override
  public OutputStream newOutputStream(Path path) {
    Files.createFile(Paths.get(mountPoint + path.getBody()));
    Files.createFile(Paths.get(mountPoint + path.getHead()));
    setProperty(path,
        META_KEY_IS_FOLDER, Boolean.FALSE.toString(),
        META_KEY_FILE_NAME, path.getFileName(),
        META_KEY_LAST_MODIFIED, fromInstantToRfc(Instant.now()));
    return Files.newOutputStream(Paths.get(mountPoint + path.getBody()));
  }

}
