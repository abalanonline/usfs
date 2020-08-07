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

public class Path {



  private boolean isValidPath(String s) {
    char[] invalidChars = {'\\', '\0'};
    if (s.equals("/")) {
      return true; // root folder is boundary case
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
      // I was not sure about three dot file name. To test it I created "..." folder on my w10 desktop.
      // I could. But when tried to delete it, I lost all files on desktop.
      // Imagine that - to lose files in the year 2020! I am really tripping to 80s.
      // The times when you could easily lose everything just by changing floppy.
      if (split[i].isEmpty() || (split[i].charAt(split[i].length() - 1) == '.')) {
        return false;
      }
    }
    return true;
  }

  public Path(String s, Concept concept) {
    if (!isValidPath(s)) { // one validation in constructor is enough
      throw new IllegalArgumentException(s);
    }
  }

  public static Path getPath(String s) {
    return new Path(s, Concept.USFS);
  }

}