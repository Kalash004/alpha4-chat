#!/bin/bash

APP_NAME=alpha4
USER=jouda
GROUP=jouds

APP_DIR="/usr/share/${APP_NAME}"
SVC_DIR=/etc/systemd/system

mkdir -p "${APP_DIR}"
mkdir -p "SVC_DIR"

cp -f *.service "${SVC_DIR}"
cp -f *.jar "${APP_DIR}"
cp -f *.sh "${APP_DIR}"
cp -f *.properties "${APP_DIR}"

chown ${USER}:${GROUP} "${APP_DIR}"
chmod 644 "${APP_DIR}/*.jar"
chmod 644 "${APP_DIR}/*.properties"
chmod 755 "${APP_DIR}/*.sh"
chmod 644 "${SVC_DIR}/${APP_NAME}.service"

echo "Refreshing systemd services"
systemctl daemon-reload
echo "Starting service"
systemctl start "${APP_NAME}"
echo "Enabling boot time start"
systemctl enable "${APP_NAME}"
