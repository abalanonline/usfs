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
import ab.usfs.Path;
import ab.usfs.Storage;
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
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static ab.usfs.Concept.fromInstantToRfc;

@Slf4j
public class USFSFtpFile implements FtpFile {

  public static final java.nio.file.Path ROOT_FOLDER = Paths.get("/");

  public static final String MOUNTED_FOLDER_PREFIX = "target";

  public static final char FOLDER_SEPARATOR = ROOT_FOLDER.toString().charAt(0);

  public static final boolean UNIX_FOLDER_SEPARATOR = FOLDER_SEPARATOR == '/';

  private final java.nio.file.Path windowsPath;
  private final String realPath;
  private final String realFileName;
  private final String usfsPath;

  private final Path path;
  private final Storage storage;

  private static String toUnixPath(java.nio.file.Path path) {
    final String s = path.toString();
    return UNIX_FOLDER_SEPARATOR ? s : s.replace(FOLDER_SEPARATOR, '/');
  }

  public USFSFtpFile(java.nio.file.Path path, Storage storage) {
    this.path = Path.getPath(toUnixPath(path.normalize()));
    this.storage = storage;
    // TODO: 2020-08-04 path validation
    if (path.isAbsolute()) throw new IllegalStateException();
    windowsPath = path.normalize();
    java.nio.file.Path windowsFileName = windowsPath.getFileName();
    realPath = toUnixPath(windowsPath);
    realFileName = windowsFileName == null ? null : windowsFileName.toString();
    usfsPath = Concept.fromUnixPathToUsfsPath(realPath);
  }


  public File legacyBody() {
    return new File(MOUNTED_FOLDER_PREFIX + usfsPath);
    //return Paths.get(MOUNTED_FOLDER_PREFIX + usfsPath);
    // TODO: 2020-08-04 use NIO
  }

  public File legacyHead() {
    return new File(Concept.v02Meta(MOUNTED_FOLDER_PREFIX + usfsPath));
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
    try (FileOutputStream stream = new FileOutputStream(legacyHead())) {
      properties.store(stream, "file system of the year"); // watch and learn
    }
    //list.add(new USFSFtpFile(path.resolve(properties.getProperty("FileName")), null));
  }

  private Properties getHeadProperties() throws IOException {
    final Properties properties = new Properties();
    try (FileInputStream stream = new FileInputStream(legacyHead())) {
      properties.load(stream);
    }
    return properties;
  }

  // v0.2 starts here

  @Override
  public String getAbsolutePath() {
    log.debug("getAbsolutePath " + realPath);
    return path.toString();
  }

  @Override
  public String getName() {
    log.debug("getName " + realPath);
    return path.getFileName();
  }

  @Override
  public boolean doesExist() {
    log.debug("doesExist " + realPath);
    return storage.exists(path);
  }

  @Override
  public boolean isDirectory() {
    log.debug("isDirectory " + realPath);
    return storage.isFolder(path);
  }

  @Override
  public int getLinkCount() {
    return isDirectory() ? 3 : 1;
  }

  @Override
  public boolean isFile() {
    log.debug("isFile " + realPath);
    return storage.isFile(path);
  }

  @Override
  public boolean isHidden() {
    return false; // by design
  }

  @Override
  public boolean isReadable() {
    log.info("isReadable " + realPath);
    return isFile(); // FIXME: 2020-08-07 review
  }

  @Override
  public boolean isWritable() {
    log.info("isWritable " + realPath);
    return true; // FIXME: 2020-08-07 review // if does not exist it is writable
  }

  @Override
  public boolean isRemovable() {
    return doesExist(); // file or folder, if it exists, it can be deleted (removed)
  }

  @Override
  public boolean mkdir() {
    log.info("mkdir " + realPath);
    return storage.createFolder(path) != null;
  }

  @Override
  public boolean delete() {
    log.debug("delete " + realPath);
    storage.delete(path);
    return true;
  }

  @Override
  public List<? extends FtpFile> listFiles() {
    log.debug("listFiles " + realPath);
    return storage.listFiles(path).stream().map(path -> new USFSFtpFile(Paths.get(path.toString()), storage))
        .collect(Collectors.toList()); // TODO: 2020-08-07 path to string to path - wrong
  }

  @Override
  public long getLastModified() {
    log.debug("getLastModified " + realPath);
    return storage.getLastModifiedInstant(path).toEpochMilli();
  }

  @Override
  public boolean setLastModified(long ms) {
    log.debug("setLastModified " + realPath);
    return storage.setLastModifiedInstant(path, Instant.ofEpochMilli(ms)) != null;
  }

  @Override
  public long getSize() {
    log.debug("getSize " + realPath);
    return storage.size(path);
  }

  @Override
  public String getOwnerName() {
    return "usfs";
  }

  @Override
  public String getGroupName() {
    return "usfs";
  }

  @Override
  public boolean move(FtpFile ftpFile) {
    // this thing is not going to be supported
    log.error("move " + realPath);
    return false;
  }

  @Override
  public Object getPhysicalFile() {
    // there is no physical file, what should we return? null or exception?
    log.error("getPhysicalFile " + realPath);
    return null;
  }

  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    log.info("createOutputStream " + realPath);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newOutputStream(path);
  }

  @Override
  public InputStream createInputStream(long offset) throws IOException {
    log.info("createInputStream " + realPath);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newInputStream(path);
  }

}
