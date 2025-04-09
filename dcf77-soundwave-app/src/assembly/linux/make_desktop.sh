#!/bin/bash

# Script just generates free desktop descriptor to start application

DCF77SOUNDWAVE_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$DCF77SOUNDWAVE_HOME/dcf77soundwave.desktop

echo [Desktop Entry] > $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Name=DCF77Soundwave >> $TARGET
echo Comment=DCF77Soundwave generator >> $TARGET
echo GenericName=DCF77Soundwave >> $TARGET
echo Exec=$DCF77SOUNDWAVE_HOME/run.sh >> $TARGET
echo Terminal=false >> $TARGET
echo Type=Application >> $TARGET
echo Icon=$DCF77SOUNDWAVE_HOME/icon.svg >> $TARGET
echo "Categories=Application;" >> $TARGET
echo "Keywords=dcf77;sound;generator;" >> $TARGET
echo StartupWMClass=DCF77Soundwave >> $TARGET
echo StartupNotify=true >> $TARGET

echo Desktop script has been generated: $TARGET

if [ -d ~/.gnome/apps ]; then
    echo copy to ~/.gnome/apps
    cp -f $TARGET ~/.gnome/apps
fi

if [ -d ~/.local/share/applications ]; then
    echo copy to ~/.local/share/applications
    cp -f $TARGET ~/.local/share/applications
fi

if [ -d ~/Desktop ]; then
    echo copy to ~/Desktop
    cp -f $TARGET ~/Desktop
fi

