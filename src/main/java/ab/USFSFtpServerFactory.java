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

import lombok.SneakyThrows;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.springframework.stereotype.Service;

@Service
public class USFSFtpServerFactory {

  @SneakyThrows
  public USFSFtpServerFactory() {
    FtpServerFactory ftpServerFactory = new FtpServerFactory();
    NullUser.MANAGER.setHomeDirectory("target");
    ftpServerFactory.setUserManager(NullUser.MANAGER);
    ftpServerFactory.setFileSystem(new USFSFileSystemFactory());
    FtpServer ftpServer = ftpServerFactory.createServer();
    ftpServer.start();
  }

}
