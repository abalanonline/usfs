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

import lombok.Getter;

public class Path {
  // /usr/local/bin/docker
  // (==== p1 ====) (=p2=)
  // (======= p3 ========)

  private final String path;
  @Getter
  private final String p1;
  @Getter
  private final String p2;
  @Getter
  private final String p3;

  private boolean isValidPath(String s) {
    char[] invalidChars = {'\\', '\0', '*', '?'};
    if (s.equals("/")) {
      return true; // root folder is valid
    }
    for (char c : invalidChars) {
      if (s.indexOf(c) >= 0) {
        return false;
      }
    }
    String[] split = s.split("/");
    if ((split.length < 2) || !split[0].isEmpty() || (s.charAt(s.length() - 1) == '/')) {
      return false;
    }
    for (int i = 1; i < split.length; i++) {
      if (split[i].isEmpty() || (split[i].charAt(split[i].length() - 1) == '.')) {
        return false;
      }
    }
    return true;
  }

  public Path(String s) {
    this.path = s;
    if (!isValidPath(this.path)) { // one validation in constructor is enough
      throw new IllegalArgumentException(this.path);
    }
    int index = this.path.lastIndexOf('/');
    p1 = this.path.substring(0, index);
    p2 = this.path.substring(index + 1);
    p3 = this.path.equals("/") ? "" : this.path;
  }

  public static Path getPath(String s) {
    return new Path(s);
  }

  public String getFileName() {
    return this.p2;
  }

  @Override
  public String toString() {
    return this.path;
  }
}
