#!/bin/bash

APP_DIR=/usr/share/alpha4

JAR_FILE=$(ls ${APP_DIR} | grep jar)

CUSTOM_PROPERTIES=

JAVA_OPTIONS=" -Dlogback.configurationFile=${APP_DIR}/logback.xml"
APP_OPTIONS=" ${APP_DIR}/custom.properties"

nohup java ${JAVA_OPTIONS} -jar ${APP_DIR}/${JAR_FILE} ${APP_OPTIONS} 2>&1 &