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

import org.junit.Test;

import static org.junit.Assert.*;

public class PathTest {
  @Test
  public void constructorValidation() {
    String[] validPaths = {"/", "/file", "/folder/file", "/dir/subdir/file.ext", "/0/1/2/3/4/5/6/7/8/9/a/b/c/d/e/f"};
    String[] invalidPaths = {null, "/a/b../c", "", "file", "/folder/", "/a/./b", "/a//b", "\\folder\\file", "/file\0"};
    String createdInvalidPath = null;
    int createdInvalidPaths = 0;
    for (String path : validPaths) {
      Path.getPath(path); // no exception
    }
    for (String path : invalidPaths) {
      try {
        Path.getPath(path);
        createdInvalidPaths++;
        createdInvalidPath = path;
      } catch (Exception e) {
        // expected, do nothing
      }
    }
    assertEquals("Created path: " + createdInvalidPath, 0, createdInvalidPaths);
  }
}
