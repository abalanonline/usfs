# ÜSFS LoveCats - über short file system

WARNING! Random numbers can be offensive. Please do not use LoveCats.

* [Crockford's Base32](https://en.wikipedia.org/wiki/Base32#Crockford's_Base32) excludes the letter U to reduce the likelihood of accidental obscenity
* [Video games passwords](https://en.wikipedia.org/wiki/Base32#Video_games) omit vowels to prevent the game from accidentally giving a profane password
* [Word-safe alphabet](https://en.wikipedia.org/wiki/Base32#Word-safe_alphabet) uses digits and letters chosen to avoid accidentally forming words

You have been warned.

## Usage example:
```console
sudo apt install dosfstools
dd if=/dev/zero of=/tmp/usfs.dos bs=1M count=1200
sudo mkfs.msdos /tmp/usfs.dos
mkdir /tmp/usfs
sudo mount -t msdos -o loop,umask=000,check=s,tz=UTC /tmp/usfs.dos /tmp/usfs
sudo docker run -d --rm --name usfs -v /tmp/usfs:/mnt -p 21:21 -p 1024:1024 abalanonline/usfs:lovecats
```
