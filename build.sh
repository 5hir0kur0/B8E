#!/bin/sh

[ -d tmp ] || mkdir tmp
find src -type f -iname '*.java' -not -iname '*test*' -print0 | xargs -0 javac -d tmp
cd src
find . -type f -iname '*.asm' -not -iname '*test*' -print0 | xargs -0 cp -t ../tmp --parents
find . -type f -iname '*.mcu' -print0 | xargs -0 cp -t ../tmp --parents
cd ..
echo 'Main-Class: controller.Main' > manifest.txt
jar cfm b8e.jar manifest.txt -C tmp .

rm -r manifest.txt tmp
