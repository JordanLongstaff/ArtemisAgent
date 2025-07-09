#!/bin/sh

API_LEVEL=$1
ORIENTATION=$2

set -x
set +e
if [ ! -z $ORIENTATION ]; then
  echo "Rotating device..."
  xdotool keydown Ctrl key F12 keyup Ctrl
fi
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