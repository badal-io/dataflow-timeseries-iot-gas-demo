#!/bin/bash

#Part 1: install FogLAMP
sudo apt-get -y update
sudo apt-get -y upgrade
sudo apt-get -y update

sudo apt install --assume-yes wget
sudo apt-get install --assume-yes debconf-utils

## set debconf values for Kerberos
cat << EOF | sudo debconf-set-selections
# Default Kerberos version 5 realm:
krb5-config     krb5-config/default_realm       string
# Kerberos servers for your realm:
krb5-config     krb5-config/kerberos_servers    string
# Administrative server for your Kerberos realm:
krb5-config     krb5-config/admin_server        string
# Add locations of default Kerberos servers to /etc/krb5.conf?
krb5-config     krb5-config/add_servers boolean true
EOF

wget -q -O - http://archives.dianomic.com/KEY.gpg | sudo apt-key add -
sudo add-apt-repository "deb http://archives.dianomic.com/foglamp/latest/ubuntu1804/x86_64/ /"

sudo apt update

sudo apt -y install foglamp foglamp-gui foglamp-north-gcp foglamp-south-opcua

/usr/local/foglamp/bin/foglamp start

#Part 2: install Java
sudo apt -y install openjdk-8-jdk
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

#Part 3: install Prosys OPC UA server simulator
wget https://www.prosysopc.com/opcua/apps/JavaServer/dist/5.0.8-330/prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
sudo chmod u=x prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
printf '\n\n\n\n\n\n\n\n\n1\n\n\n\n' | sudo ./prosys-opc-ua-simulation-server-linux-5.0.8-330.sh

cp /opt/prosys-opc-ua-simulation-server/'Prosys OPC UA Simulation Server.desktop' ~/Desktop

#Part 4: install Chrome remote desktop
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

DISPLAY= /opt/google/chrome-remote-desktop/start-host \
        --code=${CODE} \
        --redirect-url="https://remotedesktop.google.com/_/oauthredirect" \
        --name=$(hostname) \
        --pin=${PIN}