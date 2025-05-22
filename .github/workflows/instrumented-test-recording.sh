#!/bin/sh

API_LEVEL=$1

set -x
echo "Starting the screen recording..."
./scrcpy -r testRecording-$API_LEVEL.mp4 &
echo $! > scrcpy_pid.txt
set +e
./gradlew connectedCheck
TEST_STATUS=$?
echo "Test run completed with status $TEST_STATUS"
kill -2 $(cat scrcpy_pid.txt)
# Wait for the screen recording process to exit
sleep 1
exit $TEST_STATUS