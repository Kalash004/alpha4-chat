#!/bin/bash

APP_NAME=alpha4

DIST_DIR="./dist"
MISC_DIR="./misc"

rm -irf "${DIST_DIR}"

mkdir "${DIST_DIR}"

cp ./target/${APP_NAME}*-jar-with-dependencies.jar "${DIST_DIR}"
cp ${MISC_DIR}/*.sh ${DIST_DIR}
cp ${MISC_DIR}/*.service ${DIST_DIR}
cp ${MISC_DIR}/*.properties ${DIST_DIR}
cp ${MISC_DIR}/*.xml ${DIST_DIR}