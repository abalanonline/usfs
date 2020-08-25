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

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class Rfc7231Test {

  @Test
  public void instantStrings() {
    Instant rfc7231 = Instant.parse("1994-11-06T08:49:37Z"); // rfc7231 - Sun, 06 Nov 1994 08:49:37 GMT
    assertEquals(rfc7231, Instant.ofEpochMilli(784111777000L));
    assertEquals(784111777000L, rfc7231.toEpochMilli());
    assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", Rfc7231.string(rfc7231)); // rfc7231
    assertEquals(rfc7231, Rfc7231.instant("Sun, 6 Nov 1994 08:49:37 GMT"));
    assertEquals(rfc7231, Rfc7231.instant("Sun, 06 Nov 1994 08:49:37 GMT"));

    Instant rfc7232 = Instant.parse("1994-11-15T12:45:26Z"); // rfc7232 - Tue, 15 Nov 1994 12:45:26 GMT
    assertEquals(rfc7232, Instant.ofEpochMilli(784903526000L));
    assertEquals(784903526000L, rfc7232.toEpochMilli());
    assertEquals("Tue, 15 Nov 1994 12:45:26 GMT", Rfc7231.string(rfc7232)); // rfc7232
    assertEquals(rfc7232, Rfc7231.instant("Tue, 15 Nov 1994 12:45:26 GMT"));
  }

}
