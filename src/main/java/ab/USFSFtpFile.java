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
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.File;
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

@Slf4j
public class USFSFtpFile implements FtpFile {

  public static final Path ROOT_FOLDER = Paths.get("/");

  public static final String MOUNTED_FOLDER_PREFIX = "target";

  public static final char FOLDER_SEPARATOR = ROOT_FOLDER.toString().charAt(0);

  public static final boolean UNIX_FOLDER_SEPARATOR = FOLDER_SEPARATOR == '/';

  private final Path windowsPath;
  private final String realPath;
  private final String realFileName;
  private final String usfsPath;

  private static String toUnixPath(Path path) {
    final String s = path.toString();
    return UNIX_FOLDER_SEPARATOR ? s : s.replace(FOLDER_SEPARATOR, '/');
  }

  public USFSFtpFile(Path path) {
    // TODO: 2020-08-04 path validation
    if (path.isAbsolute()) throw new IllegalStateException();
    windowsPath = path.normalize();
    Path windowsFileName = windowsPath.getFileName();
    realPath = toUnixPath(windowsPath);
    realFileName = windowsFileName == null ? null : windowsFileName.toString();
    usfsPath = Concept.fromUnixPathToUsfsPath(realPath);
  }


  public File body() {
    return new File(MOUNTED_FOLDER_PREFIX + usfsPath);
    //return Paths.get(MOUNTED_FOLDER_PREFIX + usfsPath);
    // TODO: 2020-08-04 use NIO
  }

  public File head() {
    return new File(Concept.v02Meta(MOUNTED_FOLDER_PREFIX + usfsPath));
  }

  @Override
  public String getAbsolutePath() {
    log.info("getAbsolutePath " + realPath);
    return realPath;
  }

  @Override
  public String getName() {
    log.info("getName " + realPath);
    return realFileName;
  }

  @Override
  public boolean isHidden() {
    log.info("isHidden " + realPath);
    return false;
  }

  @Override
  public boolean isDirectory() {
    log.info("isDirectory " + realPath);
    return body().isDirectory();
  }

  @Override
  public boolean isFile() {
    log.info("isFile " + realPath);
    return body().isFile();
  }

  @Override
  public boolean doesExist() {
    log.info("doesExist " + realPath);
    return body().exists();
  }

  @Override
  public boolean isReadable() {
    log.info("isReadable " + realPath);
    return body().canRead();
  }

  @Override
  public boolean isWritable() {
    log.info("isWritable " + realPath);
    return true;
  }

  @Override
  public boolean isRemovable() {
    log.info("isRemovable " + realPath);
    return true;
  }

  @Override
  public String getOwnerName() {
    log.info("getOwnerName " + realPath);
    throw new IllegalAccessError();
  }

  @Override
  public String getGroupName() {
    log.info("getGroupName " + realPath);
    throw new IllegalAccessError();
  }

  @Override
  public int getLinkCount() {
    log.info("getLinkCount " + realPath);
    throw new IllegalAccessError();
  }

  @SneakyThrows
  @Override
  public long getLastModified() {
    log.info("getLastModified " + realPath);
    return fromRfcToInstant(getHeadProperties().getProperty("Last-Modified")).toEpochMilli();
  }

  @Override
  public boolean setLastModified(long ms) {
    log.info("setLastModified " + realPath);
    updateHead("Last-Modified", fromInstantToRfc(Instant.ofEpochMilli(ms)));
    return true;
  }

  @Override
  public long getSize() {
    log.info("getSize " + realPath);
    return body().length();
  }

  @Override
  public Object getPhysicalFile() {
    log.info("getPhysicalFile " + realPath);
    throw new IllegalAccessError();
  }

  @Override
  public boolean mkdir() {
    log.info("mkdir " + realPath);
    updateHead(
        "FileName", realFileName,
        "IsFolder", "true",
        "Last-Modified", fromInstantToRfc(Instant.now()));
    return body().mkdir();
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
    try (FileOutputStream stream = new FileOutputStream(head())) {
      properties.store(stream, "file system of the year"); // watch and learn
    }
    //list.add(new USFSFtpFile(path.resolve(properties.getProperty("FileName")), null));
  }

  private Properties getHeadProperties() throws IOException {
    final Properties properties = new Properties();
    try (FileInputStream stream = new FileInputStream(head())) {
      properties.load(stream);
    }
    return properties;
  }

  @Override
  public boolean delete() {
    log.info("delete " + realPath);
    final boolean h = head().delete(); // head
    return body().delete(); // body
  }

  @Override
  public boolean move(FtpFile ftpFile) {
    log.info("move " + realPath);
    throw new IllegalAccessError();
  }

  @SneakyThrows
  @Override
  public List<? extends FtpFile> listFiles() {
    log.info("listFiles " + realPath);
    Path internalFolder = body().toPath();
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
      list.add(new USFSFtpFile(windowsPath.resolve(properties.getProperty("FileName"))));
    }
    return list;
  }

  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    log.info("createOutputStream " + realPath);
    updateHead(
        "FileName", realFileName,
        "IsFolder", "false",
        "Last-Modified", fromInstantToRfc(Instant.now()));
    final RandomAccessFile randomAccessFile = new RandomAccessFile(body(), "rw");
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
    log.info("createInputStream " + realPath);
    final RandomAccessFile randomAccessFile = new RandomAccessFile(body(), "r");
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
