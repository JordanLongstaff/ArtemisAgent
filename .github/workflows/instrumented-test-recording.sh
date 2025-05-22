#!/bin/sh

API_LEVEL=$1

set -x
echo "Starting the screen recording..."
(adb exec-out "while true; do screenrecord --bugreport --output-format=h264 -; done" | ffmpeg -b:v 20M -i - testRecording-$API_LEVEL.mp4) &
echo $! > ffmpeg_pid.txt
set +e
./gradlew connectedCheck
TEST_STATUS=$?
echo "Test run completed with status $TEST_STATUS"
kill -2 $(cat ffmpeg_pid.txt)
# Wait for the screen recording process to exit
sleep 1
exit $TEST_STATUS