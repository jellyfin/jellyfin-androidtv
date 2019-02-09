#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Handle the release type
case ${RELEASE} in
    'production')
        RELEASE_SUFFIX=""
        RFLAG="assembleRelease"
    ;;
    'debug')
        RELEASE_SUFFIX=""
        RFLAG="assembleDebug"
    ;;
esac

# Export environment variables
export ANDROID_HOME=${ANDROID_DIR}

# Move to source directory
pushd ${SOURCE_DIR}

# Build APK
bash gradlew ${RFLAG}

# Move the artifacts out
mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/app/build/outputs/apk/*/*/app-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_#1_${RELEASE_SUFFIX}#3.apk"
