# USFS - unsigned short file system

Let's trip back to the 80s and save our modern files in [8.3 filename convention](https://en.wikipedia.org/wiki/8.3_filename).

Calculate SHA-256 of file name, take 16 bits of it and use this unsigned short as a new name. And one more file for metadata (original file name, modification time)

Octal encoding give file names 000000 - 177777 and excitingly high chance of collision.

[USFS release notes and roadmap](https://github.com/abalanonline/usfs/releases)
