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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Rfc7231 {

  public static String string(Instant instant) {
    String rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    if (rfc1123.charAt(6) == ' ') {
      // there are magic numbers in this block so back validation required
      try {
        String rfc7231 = rfc1123.substring(0, 5) + '0' + rfc1123.substring(5);
        if (instant(rfc7231).equals(instant)) {
          rfc1123 = rfc7231; // use new generated string
        }
      } catch (DateTimeException e) {
        // do nothing, error in string generation
      }
    }
    return rfc1123;
  }

  public static Instant instant(String s) {
    return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
  }

}
