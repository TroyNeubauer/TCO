#!/bin/sh
./gradlew jar
cp build/libs/TCO*.jar server/mods
cd server
java -jar forge-1.12.2-14.23.5.2855.jar --nogui
cd ..
