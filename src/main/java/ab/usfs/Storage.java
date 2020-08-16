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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.List;

public interface Storage {
  boolean exists(Path path);
  boolean isFolder(Path path);
  boolean isFile(Path path);
  List<Path> listFiles(Path path);
  Instant getLastModifiedInstant(Path path);
  Path setLastModifiedInstant(Path path, Instant instant);
  Path createFolder(Path path);
  long size(Path path); // unspecified for folder

  /**
   * Delete file or folder
   * @throws NoSuchFileException
   * @throws DirectoryNotEmptyException
   * @param path
   */
  void delete(Path path) throws NoSuchFileException, DirectoryNotEmptyException;
  InputStream newInputStream(Path path);
  OutputStream newOutputStream(Path path);
}
