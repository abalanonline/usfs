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
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class UsfsFtpFile implements FtpFile {

  private final Path path;
  private final Storage storage;

  public UsfsFtpFile(String path, Storage storage) {
    this.path = Path.getPath(path);
    this.storage = storage;
  }

  @Override
  public String getAbsolutePath() {
    log.debug("getAbsolutePath " + path);
    return path.toString();
  }

  @Override
  public String getName() {
    log.debug("getName " + path);
    return path.getFileName();
  }

  @Override
  public boolean doesExist() {
    log.debug("doesExist " + path);
    return storage.exists(path);
  }

  @Override
  public boolean isDirectory() {
    log.debug("isDirectory " + path);
    return storage.isFolder(path);
  }

  @Override
  public int getLinkCount() {
    return isDirectory() ? 3 : 1;
  }

  @Override
  public boolean isFile() {
    log.debug("isFile " + path);
    return storage.isFile(path);
  }

  @Override
  public boolean isHidden() {
    return false; // by design
  }

  @Override
  public boolean isReadable() {
    log.debug("isReadable " + path);
    return doesExist(); // used for files but folders return true
  }

  @Override
  public boolean isWritable() {
    log.info("isWritable " + path);
    return true; // if does not exist it is writable, existing folders are writable too
  }

  @Override
  public boolean isRemovable() {
    return doesExist(); // file or folder, if it exists, it can be deleted (removed)
  }

  @Override
  public boolean mkdir() {
    log.info("mkdir " + path);
    return storage.createFolder(path) != null;
  }

  @Override
  public boolean delete() {
    log.debug("delete " + path);
    storage.delete(path);
    return true;
  }

  @Override
  public List<? extends FtpFile> listFiles() {
    log.debug("listFiles " + path);
    return storage.listFiles(path).stream().map(path -> new UsfsFtpFile(path.toString(), storage))
        .collect(Collectors.toList());
  }

  @Override
  public long getLastModified() {
    log.debug("getLastModified " + path);
    return storage.getLastModifiedInstant(path).toEpochMilli();
  }

  @Override
  public boolean setLastModified(long ms) {
    log.debug("setLastModified " + path);
    return storage.setLastModifiedInstant(path, Instant.ofEpochMilli(ms)) != null;
  }

  @Override
  public long getSize() {
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
    return false;
  }

  @Override
  public Object getPhysicalFile() {
    // there is no physical file, what should we return? null or exception?
    log.error("getPhysicalFile " + path);
    return null;
  }

  @Override
  public OutputStream createOutputStream(long offset) {
    log.info("createOutputStream " + path);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newOutputStream(path);
  }

  @Override
  public InputStream createInputStream(long offset) {
    log.info("createInputStream " + path);
    if (offset > 0) throw new IllegalStateException("stream with offset is not supported");
    return storage.newInputStream(path);
  }

}
