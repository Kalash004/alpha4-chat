#!/usr/bin/bash

APP_DIR=/usr/share/alpha4

JAR_FILE=$(ls ${APP_DIR} | grep jar)

CUSTOM_PROPERTIES="${APP_DIR}/custom.properties"

JAVA_OPTIONS=" "
APP_OPTIONS=" ${CUSTOM_PROPERTIES}"

nohup java -jar ${APP_DIR}/${JAR_FILE} ${CUSTOM_PROPERTIES} 2>&1 &
