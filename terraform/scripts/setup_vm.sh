#!/bin/bash

#Part 1: install Chrome remote desktop
sudo apt-get -y update
sudo DEBIAN_FRONTEND=noninteractive apt-get -y upgrade

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

#Part 2: install FogLAMP
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

sudo apt -y install foglamp foglamp-gui foglamp-north-gcp foglamp-south-opcua foglamp-filter-metadata

wget http://archives.dianomic.com/foglamp/nightly/ubuntu1804/x86_64/foglamp-filter-rename-1.9.1-x86_64.deb
sudo dpkg -i foglamp-filter-rename-1.9.1-x86_64.deb

/usr/local/foglamp/bin/foglamp start

wget https://pki.goog/roots.pem
cp roots.pem /usr/local/foglamp/data/etc/certs/pem/
cp ~/foglamp_keys/rsa_private.pem /usr/local/foglamp/data/etc/certs/pem/ #TODO copy not working properly
cp ~/foglamp_keys/rsa_public.pem /usr/local/foglamp/data/etc/certs/pem/

#Part 3: install Java
sudo apt -y install openjdk-8-jdk
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

#Part 4: install Prosys OPC UA server simulator
wget https://www.prosysopc.com/opcua/apps/JavaServer/dist/5.0.8-330/prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
chmod u=x prosys-opc-ua-simulation-server-linux-5.0.8-330.sh
printf '\n\n\n\n\n\n\n\n\n1\n\n\n\n' | sudo ./prosys-opc-ua-simulation-server-linux-5.0.8-330.sh

cp /opt/prosys-opc-ua-simulation-server/'Prosys OPC UA Simulation Server.desktop' ~/Desktop