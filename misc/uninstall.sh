#!/bin/bash

APP_NAME=alpha4
USER=jouda
GROUP=jouds

APP_DIR="/usr/share/${APP_NAME}"
SVC_DIR=/etc/systemd/system

echo "Stopping service before removal"
{
	systemctl stop "${APP_NAME}"
} || {
	echo "Failed to stop previous service version"
}

rm "${SVC_DIR}/${APP_NAME}.service"
rm -rf "${APP_DIR}"


echo "Refreshing systemd services"
systemctl daemon-reload
systemctl reset-failed