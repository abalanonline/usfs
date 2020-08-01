# USFS - unsigned short file system

Let's trip back to the 80s and save our files in [8.3 filename convention](https://en.wikipedia.org/wiki/8.3_filename).

Calculate SHA-256 of file name, take 15 bits of it and create a pair of unsigned shorts, even with content and odd with metadata (original file name, modification time)

16 bits total that give file names 00000 - 65535 and excitingly high chance of collision.
