# USFS - unsigned short file system

Let's trip back to the 80s and save our modern files in [8.3 filename convention](https://en.wikipedia.org/wiki/8.3_filename).

Calculate SHA-256 of file name, take 16 bits of it and use this unsigned short as a new name. And one more file for metadata (original file name, modification time)

Octal encoding give file names 000000 - 177777 and excitingly high chance of collision.

[USFS release notes and roadmap](https://github.com/abalanonline/usfs/releases)

## Tests:
1. small - 799 files, 46 folders, 2.14 MiB - spring-core-4.3.25.RELEASE.jar file content
1. fat - 30 files, 1 folder, 1 GiB - hi-res pictures, jdk and idea archives

Write (seconds) / Read (after write, presumably cached, seconds) / Delete (seconds)

* file system - s:44/17/19, f:7/4/0
* mongo gridfs - s:204/33/36, f:44/26/1
