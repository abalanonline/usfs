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

import ab.usfs.FileSystem;
import ab.usfs.Storage;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.nio.file.Path;
import java.nio.file.Paths;

public class USFSFileSystemFactory implements FileSystemFactory, FileSystemView {

  private Path currentFolder;
  private final Storage storage;

  public USFSFileSystemFactory() {
    currentFolder = Paths.get("/");
    storage = FileSystem.mount("target");
  }

  @Override
  public FileSystemView createFileSystemView(User user) throws FtpException {
    return new USFSFileSystemFactory();
  }

  @Override
  public FtpFile getHomeDirectory() throws FtpException {
    throw new IllegalAccessError();
  }

  @Override
  public FtpFile getWorkingDirectory() throws FtpException {
    return new USFSFtpFile(currentFolder, storage);
  }

  @Override
  public boolean changeWorkingDirectory(String s) throws FtpException {
    Path path = currentFolder.resolve(s);
    boolean canChange = new USFSFtpFile(path, storage).isDirectory();
    if (canChange) currentFolder = path;
    return canChange;
  }

  @Override
  public FtpFile getFile(String s) throws FtpException {
    return new USFSFtpFile(currentFolder.resolve(s), storage);
  }

  @Override
  public boolean isRandomAccessible() throws FtpException {
    throw new IllegalAccessError();
  }

  @Override
  public void dispose() {
    // do nothing
  }

}
