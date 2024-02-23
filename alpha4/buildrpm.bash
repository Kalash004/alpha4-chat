#!/bin/bash
set -e -o pipefail
set -x

PROJECT_NAME=alpha4

TARGET_DIR="./target"
MISC_DIR="./misc"

JAR_FILE=$(ls ${TARGET_DIR} | grep ${PROJECT_NAME} | grep "with-dependencies")

RPM_DIR="$(pwd)/rpmbuild"

function cleanup() {
    if [ -d ${RPM_DIR} ]; then
        rm -rf ${RPM_DIR}
    fi
}

function package() {
	cleanup

    echo "Resolving RPM version"

    PROJECT_VERSION=$(echo $(ls ${TARGET_DIR} | grep ${PROJECT_NAME})| cut -d'-' -f 2)

    echo "Resolved RPM version: $PROJECT_VERSION"

    # create rpm build environment
    echo "Creating RPM dirs"
    mkdir -p ${RPM_DIR}/SPECS
    mkdir -p ${RPM_DIR}/BUILD
    mkdir -p ${RPM_DIR}/RPMS
    mkdir -p ${RPM_DIR}/SRPMS
    mkdir -p ${RPM_DIR}/SOURCES
    mkdir -p ${RPM_DIR}/TMP

	# create distribution directory with all our jars and files
	DISTRO_DIR="${RPM_DIR}/TMP/${PROJECT_NAME}-${PROJECT_VERSION}"

	mkdir "${DISTRO_DIR}"
	# move our jar to the distribution directory
	cp ${TARGET_DIR}/${JAR_FILE} "${DISTRO_DIR}"
	cp ${MISC_DIR}/*.sh "${DISTRO_DIR}"
	cp ${MISC_DIR}/*.properties "${DISTRO_DIR}"
	cp ${MISC_DIR}/*.service "${DISTRO_DIR}"

    echo "Building Tarball"
	TARFILE="${PROJECT_NAME}-${PROJECT_VERSION}.tar.gz"
    tar -czf ${RPM_DIR}/TMP/${TARFILE} --directory="${RPM_DIR}/TMP/" "${PROJECT_NAME}-${PROJECT_VERSION}"

    cp ${RPM_DIR}/TMP/${TARFILE} ${RPM_DIR}/SOURCES
    cp ${MISC_DIR}/${PROJECT_NAME}.spec ${RPM_DIR}/SPECS

    echo "Building RPM"
    # define target os as linux because it is a requirement x86_64
    rpmbuild --define "_target_os linux" --define "_topdir ${RPM_DIR}" --define "_version $PROJECT_VERSION" --define "_tmppath ${RPM_DIR}/TMP" -bb "${RPM_DIR}/SPECS/${PROJECT_NAME}.spec"

    echo "RPM build complete, check current directory for RPMs"

	# find RPM and copy to the current directory
    find "${RPM_DIR}" -name "${PROJECT_NAME}*${PROJECT_VERSION}*.rpm" -exec cp "{}" . \;

	# cleanup RPM build files
    echo "Cleanup"
    #cleanup
}

package
