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

import ab.usfs.Path;
import ab.usfs.Storage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static ab.Application.tick;

@Slf4j
public class UsfsFtpFile implements FtpFile {

  private final Path path;
  private final Storage storage;

  public UsfsFtpFile(String path, Storage storage) {
    this.path = Path.getPath(path);
    this.storage = storage;
    tick();
  }

  @Override
  public String getAbsolutePath() {
    tick();
    log.debug("getAbsolutePath " + path);
    return path.toString();
  }

  @Override
  public String getName() {
    tick();
    log.debug("getName " + path);
    return path.getFileName();
  }

  @Override
  public boolean doesExist() {
    tick();
    log.debug("doesExist " + path);
    return storage.exists(path);
  }

  @Override
  public boolean isDirectory() {
    tick();
    log.debug("isDirectory " + path);
    return storage.isFolder(path);
  }

  @Override
  public int getLinkCount() {
    return isDirectory() ? 3 : 1;
  }

  @Override
  public boolean isFile() {
    tick();
    log.debug("isFile " + path);
    return storage.isFile(path);
  }

  @Override
  public boolean isHidden() {
    return false; // by design
  }

  @Override
  public boolean isReadable() {
    tick();
    log.debug("isReadable " + path);
    return doesExist(); // used for files but folders return true
  }

  @Override
  public boolean isWritable() {
    tick();
    log.debug("isWritable " + path);
    return true; // if does not exist it is writable, existing folders are writable too
  }

  @Override
  public boolean isRemovable() {
    return doesExist(); // file or folder, if it exists, it can be deleted (removed)
  }

  @SneakyThrows
  @Override
  public boolean mkdir() {
    tick();
    log.debug("mkdir " + path);
    return storage.createFolder(path) != null;
  }

  @SneakyThrows
  @Override
  public boolean delete() {
    tick();
    log.debug("delete " + path);
    storage.delete(path);
    return true;
  }

  @SneakyThrows
  @Override
  public List<? extends FtpFile> listFiles() {
    tick();
    log.debug("listFiles " + path);
    return storage.listFiles(path).stream().map(path -> new UsfsFtpFile(path.toString(), storage))
        .collect(Collectors.toList());
  }

  @SneakyThrows
  @Override
  public long getLastModified() {
    tick();
    log.debug("getLastModified " + path);
    return storage.getLastModifiedInstant(path).toEpochMilli();
  }

  @SneakyThrows
  @Override
  public boolean setLastModified(long ms) {
    tick();
    log.debug("setLastModified " + path);
    return storage.setLastModifiedInstant(path, Instant.ofEpochMilli(ms)) != null;
  }

  @SneakyThrows
  @Override
  public long getSize() {
    tick();
    log.debug("getSize " + path);
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
    log.error("move " + path);
    tick();
    return false;
  }

  @Override
  public Object getPhysicalFile() {
    // there is no physical file, what should we return? null or exception?
    log.error("getPhysicalFile " + path);
    tick();
    return null;
  }

  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    tick();
    log.debug("createOutputStream " + path);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newOutputStream(path);
  }

  @Override
  public InputStream createInputStream(long offset) throws IOException {
    tick();
    log.debug("createInputStream " + path);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newInputStream(path);
  }

}
