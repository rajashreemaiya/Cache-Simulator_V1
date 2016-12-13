#!/bin/sh

CLASSPATH=/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/bin
PROGRAM_NAME=*.java
MAINCLASS_NAME=SimulatorDriver
cd ~/Documents/workspace/Cache-Simulator-1/src
# for i in `ls ~/Desktop/Sim.jar`
#   do
#   THE_CLASSPATH=${THE_CLASSPATH}:${i}
# done

javac -classpath $CLASSPATH $PROGRAM_NAME

for j in `ls ~/Documents/workspace/Cache-Simulator-1/Inputs/*.properties`
  do
    echo "Running simulation for ${j}....."
		java -classpath $CLASSPATH $MAINCLASS_NAME ${j}
done

if [ $? -eq 0 ]
then
  echo "compile worked!"
fi
