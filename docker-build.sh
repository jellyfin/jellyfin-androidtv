#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Handle the release type
case ${RELEASE} in
    'production')
        RFLAG="assembleRelease"
    ;;
    'debug')
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
mmv "${SOURCE_DIR}/app/build/outputs/apk/*/jellyfin-androidtv_*.apk" "${ARTIFACT_DIR}/apk/"
