#!/bin/sh

[ -d tmp ] || mkdir tmp
find src -type f -iname '*.java' -not -iname '*test*' -print0 | xargs -0 javac -d tmp
echo 'Main-Class: controller.Main' > tmp/manifest.txt
jar cfm b8e.jar tmp/manifest.txt -C tmp .
rm -r tmp
