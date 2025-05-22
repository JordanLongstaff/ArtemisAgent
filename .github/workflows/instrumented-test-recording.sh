#!/bin/sh

API_LEVEL=$1

set -x
set +e
echo "Starting instrumented tests..."
./gradlew connectedCheck &
TEST_PID=$!
echo "Starting the screen recording..."
adb exec-out "while true; do screenrecord --bugreport --output-format=h264 -; done" | ffmpeg -i - testRecording-$API_LEVEL.mp4 &
echo "Waiting for instrumented tests to finish..."
wait $TEST_PID
TEST_STATUS=$?
echo "Stopping the screen recording..."
ps aux | grep ffmpeg | awk '{print $2}' | xargs kill -9
echo "Test run completed with status $TEST_STATUS"
# Wait for the screen recording process to exit
sleep 1
exit $TEST_STATUS