#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Handle the release type
case ${RELEASE} in
    'production')
        RELEASE_SUFFIX=""
        NODE_ENV="production"
        RFLAG="assembleRelease"
    ;;
    'unminified')
        RELEASE_SUFFIX="unminified_"
        NODE_ENV="development"
        RFLAG="assembleRelease"
    ;;
    'debug')
        RELEASE_SUFFIX=""
        NODE_ENV="development"
        RFLAG="assembleDebug"
    ;;
esac

# Export environment variables
export ANDROID_HOME=${ANDROID_DIR}
export NODE_ENV

# Move to source directory
pushd ${SOURCE_DIR}

# Build APK
bash gradlew ${RFLAG}

# Move the artifacts out
mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/app/build/outputs/apk/app-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_${RELEASE_SUFFIX}#1.apk"
