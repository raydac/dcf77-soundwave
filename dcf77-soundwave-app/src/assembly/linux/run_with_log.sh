#!/bin/bash

DCF77SOUNDWAVE_HOME="$(dirname ${BASH_SOURCE[0]})"
LOG_FILE=$DCF77SOUNDWAVE_HOME/console.log
JAVA_HOME=$DCF77SOUNDWAVE_HOME/jre

JAVA_FLAGS="-server -Xverify:none -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED"

JAVA_RUN=$JAVA_HOME/bin/java

if [ -f $DCF77SOUNDWAVE_HOME/.pid ];
then
    SAVED_PID=$(cat $DCF77SOUNDWAVE_HOME/.pid)
    if [ -f /proc/$SAVED_PID/exe ];
    then
        echo Application already started! if it is wrong, just delete the .pid file in the application folder root!
        exit 1
    fi
fi

echo \$JAVA_RUN=$JAVA_RUN &>$LOG_FILE

echo ------JAVA_VERSION------ &>>$LOG_FILE

$JAVA_RUN -version &>>$LOG_FILE

echo ------------------------ &>>$LOG_FILE

$JAVA_RUN $JAVA_FLAGS -jar "$DCF77SOUNDWAVE_HOME"/dcf77soundwave.jar $@ &>>$LOG_FILE&
THE_PID=$!
echo $THE_PID>$DCF77SOUNDWAVE_HOME/.pid
wait $THE_PID
rm $DCF77SOUNDWAVE_HOME/.pid
exit 0
