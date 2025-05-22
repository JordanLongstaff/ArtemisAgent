#!/bin/sh

API_LEVEL=$1

set -x
set +e
echo "Starting instrumented tests..."
./gradlew connectedCheck & echo $! > test_pid.txt
echo "Starting the screen recording..."
adb exec-out "while true; do screenrecord --bugreport --output-format=h264 -; done" | ffmpeg -i - testRecording-$API_LEVEL.mp4 &
echo "Waiting for instrumented tests to finish..."
cat test_pid.txt | xargs wait
TEST_STATUS=$?
echo "Stopping the screen recording..."
kill $!
echo "Test run completed with status $TEST_STATUS"
# Wait for the screen recording process to exit
sleep 1
exit $TEST_STATUS