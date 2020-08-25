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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileSystem extends AbstractStorage {

  private final Concept concept;
  private final String mountFolder;

  public FileSystem(String mountFolder,  Concept concept) throws IOException {
    super(concept);
    this.concept = concept;
    this.mountFolder = mountFolder;
    ab.usfs.Path root = new ab.usfs.Path("/");
    if (!exists(root)) {
      createFolder(root); // root meta need to be manually created
    }
  }

  public Path path(byte[] pk) {
    return Paths.get(mountFolder, concept.radixStr(pk));
  }

  public Path path(byte[] pk, byte[] sk) {
    return Paths.get(mountFolder, concept.radixStr(pk), concept.radixStr(sk));
  }

  @Override
  public byte[] loadByte(byte[] pk, byte[] sk) throws IOException {
    return Files.readAllBytes(path(pk, sk));
  }

  @Override
  public void saveByte(byte[] pk, byte[] sk, byte[] b) throws IOException {
    Path path = path(pk, sk);
    Files.createDirectories(path.getParent());
    Files.write(path, b, StandardOpenOption.CREATE_NEW);
  }

  @Override
  public void deleteByte(byte[] pk, byte[] sk) throws IOException {
    Files.delete(path(pk, sk));
  }

  @Override
  public void delete(ab.usfs.Path path) throws IOException {
    super.delete(path);
    try {
      Files.delete(path(getFpk(path)));
    } catch (NoSuchFileException e) {
      // expected, do nothing
    }
  }

  @Override
  public List<byte[]> listByte(byte[] pk) throws IOException {
    List<byte[]> list = new ArrayList<>();
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(path(pk))) {
      for (Path path : paths) {
        list.add(Files.readAllBytes(path));
      }
    } catch (NoSuchFileException e) {
      // expected, do nothing
    }
    return list;
  }
}
