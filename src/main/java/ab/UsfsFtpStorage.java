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
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import static ab.Application.tick;

@Slf4j
public class UsfsFtpStorage implements FileSystemFactory, FileSystemView {

  private String currentFolder;
  private final Storage storage;

  public UsfsFtpStorage(Storage storage) {
    currentFolder = "/";
    this.storage = storage;
  }

  @Override
  public FileSystemView createFileSystemView(User user) {
    return new UsfsFtpStorage(this.storage);
  }

  @Override
  public FtpFile getHomeDirectory() {
    return new UsfsFtpFile("/", storage);
  }

  @Override
  public FtpFile getWorkingDirectory() {
    return new UsfsFtpFile(currentFolder, storage);
  }

  private String resolve(String s) {
    tick();
    // who can resolve better than native file system?
    return java.nio.file.Paths.get(currentFolder).resolve(s).normalize().toString().replace('\\', '/');
  }

  @Override
  public boolean changeWorkingDirectory(String s) {
    tick();
    String path = resolve(s);
    boolean canChange = storage.isFolder(Path.getPath(path));
    if (canChange) currentFolder = path;
    return canChange;
  }

  @Override
  public FtpFile getFile(String s) {
    tick();
    return new UsfsFtpFile(resolve(s), storage);
  }

  @Override
  public boolean isRandomAccessible() {
    return false; // not implemented
  }

  @Override
  public void dispose() {
    // do nothing
  }

}
