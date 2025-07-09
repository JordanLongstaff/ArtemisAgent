#!/bin/sh

API_LEVEL=$1
ORIENTATION=$2

set -x
set +e
echo "Setting device orientation..."
adb shell content insert --uri content://settings/system --bind name:s:accelerometer_rotation --bind value:i:0
adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:$ORIENTATION
echo "Starting instrumented tests..."
./gradlew connectedCheck &
sleep 10
TEST_PID=$!
echo "Starting the screen recording..."
adb exec-out "while true; do screenrecord --bugreport --output-format=h264 -; done" | ffmpeg -i - testRecording-$API_LEVEL-$ORIENTATION.mp4 &
sleep 1
echo "Waiting for instrumented tests to finish..."
wait $TEST_PID
TEST_STATUS=$?
# Wait for the screen recording process to exit
sleep 1
exit $TEST_STATUS