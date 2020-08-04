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

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class USFSFtpFileTest {

  @Test
  public void stringToId() {
    Function<Long, String> msToRfc = l ->
        DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC)); // RFC 7231,7232
    Function<String, Long> rfcToMs = s ->
        Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s)).toEpochMilli(); // RFC 7231,7232
    Assert.assertEquals("Tue, 15 Nov 1994 12:45:26 GMT", msToRfc.apply(784903526000L)); // rfc7232
    //Assert.assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", msToRfc.apply(784111777000L)); // rfc7231
    Assert.assertEquals(784111777000L, (long) rfcToMs.apply("Sun, 06 Nov 1994 08:49:37 GMT"));
    //Instant.parse("Sun, 06 Nov 1994 08:49:37 GMT").toEpochMilli();
  }
}
