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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
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

  // TODO: 2020-08-07 Review the key names and which class they must belong
  public static final String HEAD_FILE_NAME = "FileName";
  public static final String HEAD_IS_FOLDER = "IsFolder";
  public static final String HEAD_LAST_MODIFIED = "Last-Modified";

  private final String mountPoint;
  private final Concept concept;

  @SneakyThrows
  public FileSystem(String s, Concept concept) {
    this.concept = concept;
    mountPoint = s;
    Path root = new Path("/", concept);
    if (!exists(root)) {
      String head = mountPoint + root.getHead();
      Files.createDirectories(Paths.get(head.substring(0, head.lastIndexOf('/'))));
      Properties properties = new Properties();
      properties.setProperty(HEAD_FILE_NAME, "");
      properties.setProperty(HEAD_IS_FOLDER, "true");
      properties.setProperty(HEAD_LAST_MODIFIED, fromInstantToRfc(Instant.now()));
      try (FileOutputStream stream = new FileOutputStream(head)) {
        properties.store(stream, null);
      }
    }
    java.nio.file.Path test = Paths.get("/");
    Files.exists(test);
    Files.isDirectory(test);
    Files.isRegularFile(test);
    Files.getLastModifiedTime(test);
    //Files.setLastModifiedTime(test, null);
    //Files.createDirectory(test);
    //Files.delete(test);
    Files.size(test);
  }

  public static FileSystem mount(String s) {
    return new FileSystem(s, Concept.USFS);
  }

  @SneakyThrows
  private Properties getHead(String nioPath) {
    final Properties properties = new Properties();
    try (FileInputStream stream = new FileInputStream(nioPath)) {
      properties.load(stream);
    }
    return properties;
  }

  public Properties getHead(Path path) {
    return getHead(mountPoint + path.getHead());
  }

  public String getProperty(Path path, String key) {
    return getHead(path).getProperty(key);
  }

  @SneakyThrows
  public void setProperty(Path path, String key, String value) {
    Properties properties = getHead(path);
    properties.setProperty(key, value);
    try (FileOutputStream stream = new FileOutputStream(mountPoint + path.getHead())) {
      properties.store(stream, "file system of the year"); // watch and learn
    }
  }

  @Override
  public boolean exists(Path path) {
    return Files.exists(Paths.get(mountPoint + path.getHead()));
  }

  @Override
  public boolean isFolder(Path path) {
    return exists(path) && Boolean.parseBoolean(getProperty(path, HEAD_IS_FOLDER));
  }

  @Override
  public boolean isFile(Path path) {
    return exists(path) && !Boolean.parseBoolean(getProperty(path, HEAD_IS_FOLDER));
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
        String propertyFileName = getHead(nioFolder + '/' + nioPath.getFileName().toString()).getProperty(HEAD_FILE_NAME);
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
    return fromRfcToInstant(getProperty(path, HEAD_LAST_MODIFIED));
  }

  @Override
  public Path setLastModifiedInstant(Path path, Instant instant) {
    setProperty(path, HEAD_LAST_MODIFIED, fromInstantToRfc(instant));
    return path;
  }

  @SneakyThrows
  @Override
  public Path createFolder(Path path) {
    Files.createDirectory(Paths.get(mountPoint + path.getFoot()));
    Files.createFile(Paths.get(mountPoint + path.getHead()));
    setProperty(path, HEAD_IS_FOLDER, "true");
    setProperty(path, HEAD_FILE_NAME, path.getFileName());
    setProperty(path, HEAD_LAST_MODIFIED, fromInstantToRfc(Instant.now()));
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
    if (isFolder(path)) {
      Files.delete(Paths.get(mountPoint + path.getFoot()));
    } else {
      Files.delete(Paths.get(mountPoint + path.getBody()));
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
    setProperty(path, HEAD_IS_FOLDER, Boolean.FALSE.toString());
    setProperty(path, HEAD_FILE_NAME, path.getFileName());
    setProperty(path, HEAD_LAST_MODIFIED, fromInstantToRfc(Instant.now()));
    return Files.newOutputStream(Paths.get(mountPoint + path.getBody()));
  }

}
