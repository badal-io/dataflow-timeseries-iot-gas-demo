#!/bin/bash

#Part 1: install FogLAMP
sudo apt-get update
sudo apt-get upgrade
sudo apt-get update

wget -q -O - http://archives.dianomic.com/KEY.gpg | sudo apt-key add -
sudo add-apt-repository "deb http://archives.dianomic.com/foglamp/latest/ubuntu1804/x86_64/ /"

sudo apt update

sudo apt -y install foglamp foglamp-gui foglamp-north-gcp foglamp-south-opcua

/usr/local/foglamp/bin/foglamp start

#Part 2: install Java
sudo apt install openjdk-8-jdk
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

#Part 3: install Prosys OPC UA server simulator
wget https://www.prosysopc.com/opcua/apps/JavaServer/dist/5.0.8-330/prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
sudo chmod u=x prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
sudo ./prosys-opc-ua-simulation-server-linux-5.0.8-330.sh

#Part 4: install Chrome remote desktop
sudo apt update
sudo apt install --assume-yes wget

wget https://dl.google.com/linux/direct/chrome-remote-desktop_current_amd64.deb
sudo dpkg --install chrome-remote-desktop_current_amd64.deb
sudo apt install --assume-yes --fix-broken

sudo DEBIAN_FRONTEND=noninteractive \
    apt install --assume-yes xfce4 desktop-base

sudo bash -c 'echo "exec /etc/X11/Xsession /usr/bin/xfce4-session" > /etc/chrome-remote-desktop-session'

sudo apt install --assume-yes xscreensaver
sudo systemctl disable lightdm.service

wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb

sudo dpkg --install google-chrome-stable_current_amd64.deb
sudo apt install --assume-yes --fix-broken