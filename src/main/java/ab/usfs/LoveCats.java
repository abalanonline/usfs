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
import java.math.BigInteger;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * WARNING! Random numbers can be offensive. Please do not use LoveCats.
 * <p><ul>
 * <li>Crockford's Base32 excludes the letter U to reduce the likelihood of accidental obscenity
 * <li>Video games passwords omit vowels to prevent the game from accidentally giving a profane password
 * <li>Word-safe alphabet uses digits and letters chosen to avoid accidentally forming words
 * </ul><p>
 * You have been warned.
 * <hr>
 * LoveCats is a balanced folder tree with the names of random species.
 * @see <a href="https://en.wikipedia.org/wiki/Base32#Crockford's_Base32">Crockford's Base32</a>
 * @see <a href="https://en.wikipedia.org/wiki/Base32#Video_games">Nintendo video games passwords</a>
 * @see <a href="https://en.wikipedia.org/wiki/Base32#Word-safe_alphabet">Word-safe alphabet</a>
 */
public class LoveCats extends FileSystem {

  // 360KB floppy disks with FAT12 had 112 root folder entries so first number must be 6 or less
  // second - maybe geometric mean of first 6(5) and 55 = 18(16)
  public static final int[] BIT = {5, 11};
  // TODO: 2020-08-30 detect duplicates
  public static final String[][] ADJ = {{
      "love", // is all you need
      "small", "fat", "big", "young", "old",
  }, {
      "new", "long", "short", "fast", "slow",
      "hot", "warm", "cool", "cold", // amount of time and matter
      "black", "red", "brown", "blue", "green", "gray", "white", "pink", "olive", "cyan",
      "gold", "lime", "aqua", "navy", "coral", "teal", "amber", "ivory", "wheat", "peach",  // rainbow
      "play", "nice", "happy", "sad", "good", "bad", // and the Ugly
      // vocabulary
  }, {}};
  public static final String[][] NOUN = {{
      "cat", "dog", "pig", "cow", // this is our domesticated friends
      "boy", "man", "girl", "woman", "god",
  }, {
      "baby", "child", "bro", "sis", "mom", "dad", // this is us
      "earth", "water", "air", "wind", "fire", "wood", "metal", // rock paper scissors
      "body", "eye", "knee", "foot", "leg", "nail", "mouth", "hand", "toe", "arm",
      "nose", "hair", "thumb", "ear", "head", "lip", "ankle", "wrist", "elbow", "neck",
      "back", "butt", "heel", "waist", "chest", "jaw", "cheek", "face", "beard",
      "brain", "bone", "heart", "lung", "vein", "liver", "anus", "skin",
      "hoof", "horn", "tail", // body without some essential systems
      "ridge", "roof", "pot", "dish", "wall", "door", "stair", "brick", "fence", "bed",
      "bath", "box", "alarm", "step", "attic", "phone", "cup", "sofa", "table", "fan",
      "lamp", "clock", "vase", "case", "oven", "fork", "knife", "spoon", "plate", "glass",
      "bowl", "board", "grill", "bin", "soap", // house
      "van", "taxi", "car", "bus", "bike", "lorry", "crane", "truck", "ship", "train",
      "jet", "plane", "boat", "sled", "skate", "ferry", "road", // transport
      "goat", "sheep",
      "tiger", "owl", "panda", "koala", "shark", "crab", "camel", "zebra", "eagle", "wolf",
      "cow", "bunny", "duck", "deer", "bee", "fish", "dove", "horse", "lion", "otter",
      "mouse", "mole", "fox", "bear", "snake", "bison", "deer", "bull", "fawn", "hyena",
      "moose", "viper", "whale", "yak", "frog", "hen", "bitch", "drake", "doe", "ewe", // more animals
  }, { // Merriam-Webster 3,000 Core Vocabulary Words
      // "ache", "actor", "adult", "age", "agent", "aid", "air", "aisle", "alarm", "alert",
      // "anger", "angle", "apple", "", "", "", "", "", "", "",
      // "", "", "", "", "", "", "", "", "", "",
      // I feel that LoveCats engine will be one of my next projects
  }, {}};
  public static final String[][] DICT;

  private static List<String> newCats(int a, int n) {
    List<String> list = new ArrayList<>();
    for (String adj : ADJ[a]) {
      for (String noun : NOUN[n]) {
        if (adj.length() + noun.length() > 8) {
          continue;
        }
        list.add(adj + noun);
      }
    }
    return list;
  }

  static {
    int levels = BIT.length;
    DICT = new String[levels][];
    List<String> dictionary = new ArrayList<>();
    Random random = new Random(0L);
    for (int level = 0; level < levels; level++) {
      for (int i = 0; i < level; i++) {
        dictionary.addAll(newCats(i, level)); // loveboy
        dictionary.addAll(newCats(level, i)); // smallcat
      }
      dictionary.addAll(newCats(level, level));
      int items = 1 << BIT[level];
      System.out.println("dict " + level + " - " + items + " / " + dictionary.size() + " ");
      DICT[level] = new String[items];
      for (int item = 0; item < items; item++) {
        int i = random.nextInt(dictionary.size());
        DICT[level][item] = dictionary.get(i);
        dictionary.remove(i);
      }
    }
  }

  public LoveCats(String mountFolder, Concept concept) throws IOException {
    super(mountFolder, concept.withBitSize(120, 5)); // divisible by 5 and 8, shorter than md5
  }

  private String url(byte[] key, boolean balanced) {
    if (!balanced) {
      byte mostSigBytes = 0;
      for (int i = 0; i < key.length - Short.BYTES; i++) {
        mostSigBytes |= key[i];
      }
      if (mostSigBytes == 0) {
        return Concept.USFS.radixStr(Arrays.copyOfRange(key, key.length - Short.BYTES, key.length));
        // use 177777
      }
    }

    String s = concept.radixStr(key);
    s = s.substring(0, 8) + '.' + s.substring(8, 11); // 55 bit
    if (balanced) {
      int bit = IntStream.of(BIT).sum();
      int k = new BigInteger(key).shiftRight(120 - 55 - bit).intValue();
      //k &= (1 << (bit + 5)) - 1;
      for (int level = BIT.length - 1; level >= 0; level--) {
        int b = BIT[level];
        s = DICT[level][k & ((1 << b) - 1)] + '/' + s;
        k >>= b;
      }
      //s = "usfs/" + s;
    }
    return s;
  }

  @Override
  public Path path(byte[] pk) {
    return Paths.get(mountFolder, url(pk, true));
  }

  @Override
  public Path path(byte[] pk, byte[] sk) {
    return Paths.get(mountFolder, url(pk, true), url(sk, false));
  }

  @Override
  public void delete(ab.usfs.Path path) throws IOException {
    super.delete(path);
    try {
      Path folderPath = path(getFpk(path)).getParent();
      Files.delete(folderPath);
      folderPath = folderPath.getParent();
      Files.delete(folderPath);
    } catch (DirectoryNotEmptyException e) {
      // expected, do nothing
    }
  }

}
