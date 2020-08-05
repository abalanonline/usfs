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
import lombok.SneakyThrows;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static ab.usfs.Concept.fromInstantToRfc;
import static ab.usfs.Concept.fromRfcToInstant;

public class USFSFtpFile implements FtpFile {

  public static final Path ROOT_FOLDER = Paths.get("/");

  public static final String MOUNTED_FOLDER_PREFIX = "target";

  public static final char FOLDER_SEPARATOR = ROOT_FOLDER.toString().charAt(0);

  public static final boolean UNIX_FOLDER_SEPARATOR = FOLDER_SEPARATOR == '/';

  private final Path absoluteNormalizedPath;

  private final String usfsPath;

  private static String toUnixPath(Path path) {
    final String s = path.toString();
    return UNIX_FOLDER_SEPARATOR ? s : s.replace(FOLDER_SEPARATOR, '/');
  }

  public USFSFtpFile(Path path) {
    // TODO: 2020-08-04 path validation
    if (path.isAbsolute()) throw new IllegalStateException();
    this.absoluteNormalizedPath = path.normalize();
    usfsPath = Concept.fromUnixPathToUsfsPath(toUnixPath(absoluteNormalizedPath));
  }

  @Override
  public String getAbsolutePath() {
    return toUnixPath(absoluteNormalizedPath);
  }

  @Override
  public String getName() {
    return absoluteNormalizedPath.getFileName().toString();
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return getInternalPath().toFile().isDirectory();
  }

  @Override
  public boolean isFile() {
    return getInternalPath().toFile().isFile();
    //if (getRelativePath().length() == 0) return false;
    //return ftpFile.isFile();
    //throw new IllegalAccessError();
  }

  @Override
  public boolean doesExist() {
    return getInternalPath().toFile().exists();
  }

  @Override
  public boolean isReadable() {
    return getInternalPath().toFile().canRead();
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public String getOwnerName() {
    throw new IllegalAccessError();
  }

  @Override
  public String getGroupName() {
    throw new IllegalAccessError();
  }

  @Override
  public int getLinkCount() {
    throw new IllegalAccessError();
  }

  @SneakyThrows
  @Override
  public long getLastModified() {
    return fromRfcToInstant(getHeadProperties().getProperty("Last-Modified")).toEpochMilli();
  }

  @Override
  public boolean setLastModified(long ms) {
    updateHead("Last-Modified", fromInstantToRfc(Instant.ofEpochMilli(ms)));
    return true;
  }

  @Override
  public long getSize() {
    return getInternalPath().toFile().length();
  }

  @Override
  public Object getPhysicalFile() {
    throw new IllegalAccessError();
  }

  @Override
  public boolean mkdir() {
    updateHead(
        "FileName", absoluteNormalizedPath.getFileName().toString(),
        "IsFolder", "true",
        "Last-Modified", fromInstantToRfc(Instant.now()));
    return getInternalPath().toFile().mkdir();
  }

  @SneakyThrows
  private void updateHead(String ... args) {
    if (args.length % 2 != 0) throw new IllegalStateException();
    Properties properties;
    try {
      properties = getHeadProperties();
    } catch (IOException e) {
      // it is expected that properties file does not exist
      properties = new Properties();
    }
    for (int i = 0; i < args.length; i += 2) {
      properties.setProperty(args[i], args[i+1]);
    }
    try (FileOutputStream stream = new FileOutputStream(getInternalPath(true).toFile())) {
      properties.store(stream, "file system of the year"); // watch and learn
    }
    //list.add(new USFSFtpFile(path.resolve(properties.getProperty("FileName")), null));
  }

  private Properties getHeadProperties() throws IOException {
    final Properties properties = new Properties();
    try (FileInputStream stream = new FileInputStream(getInternalPath(true).toFile())) {
      properties.load(stream);
    }
    return properties;
  }

  @Override
  public boolean delete() {
    final boolean h = getInternalPath(true).toFile().delete(); // head
    return getInternalPath().toFile().delete(); // body
  }

  @Override
  public boolean move(FtpFile ftpFile) {
    throw new IllegalAccessError();
  }

  // usfsFilePath = target/47736
  // usfsMetaPath = target/47737
  public Path getInternalPath(boolean isHead) {
    String path = MOUNTED_FOLDER_PREFIX + usfsPath;
    return Paths.get(isHead ? Concept.v02Meta(path) : path);
  }

  public Path getInternalPath() {
    return getInternalPath(false);
  }

  @SneakyThrows
  @Override
  public List<? extends FtpFile> listFiles() {
    Path internalFolder = getInternalPath();
    Map<Integer, Integer> internalFiles = new HashMap<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(internalFolder, "[0-6][0-9][0-9][0-9][0-9]")) {
      for (Path path : directoryStream) {
        int i = Integer.parseInt(path.getFileName().toString());
        if (i > 0xFFFF) continue;
        internalFiles.put(i & 0xFFFE, internalFiles.getOrDefault(i & 0xFFFE, 0) | ((i & 1) + 1));
      }
    }
    List<FtpFile> list = new ArrayList<>();
    for (int i : internalFiles.keySet()) {
      if (internalFiles.get(i) != 0b11) continue;
      final Path path1 = internalFolder.resolve(String.format("%05d", i | 0x0001));
      String s1 = path1.toString();
      final Properties properties = new Properties();
      try (FileInputStream stream = new FileInputStream(path1.toFile())) {
        properties.load(stream);
      }
      list.add(new USFSFtpFile(absoluteNormalizedPath.resolve(properties.getProperty("FileName"))));
    }
    return list;
  }

  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    updateHead(
        "FileName", absoluteNormalizedPath.getFileName().toString(),
        "IsFolder", "false",
        "Last-Modified", fromInstantToRfc(Instant.now()));
    final RandomAccessFile randomAccessFile = new RandomAccessFile(getInternalPath().toFile(), "rw");
    randomAccessFile.setLength(offset);
    randomAccessFile.seek(offset);
    return new FileOutputStream(randomAccessFile.getFD()) {
      @Override
      public void close() throws IOException {
        super.close();
        randomAccessFile.close();
      }
    };
  }

  @Override
  public InputStream createInputStream(long offset) throws IOException {
    final RandomAccessFile randomAccessFile = new RandomAccessFile(getInternalPath().toFile(), "r");
    randomAccessFile.seek(offset);
    return new FileInputStream(randomAccessFile.getFD()) {
      @Override
      public void close() throws IOException {
        super.close();
        randomAccessFile.close();
      }
    };
  }
}
