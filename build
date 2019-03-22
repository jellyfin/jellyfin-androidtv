#!/usr/bin/env bash

# Build APKs using a reproducible Docker container

set -o errexit

dockerfile="Dockerfile"
image_name="jellyfin-androidtv-apkbuild"

# Initialize the submodules
git submodule update --init

usage() {
    echo -e "Usage:"
    echo -e " $0 [-r/--release <release>]"
    echo -e "The release defaults to a minified 'production' build; specify 'debug' for a debug release."
    exit 1
}

# Handle the release argument
if [[ ${1} == '--release' || ${1} == '-r' ]]; then
    if [[ -n ${2} ]]; then
        release="${2}"
        shift 2
    else
        usage
    fi
else
    release="production"
fi

set -o xtrace
package_temporary_dir="$( mktemp -d )"
current_user="$( whoami )"

# Trap cleanup for latter sections
cleanup() {
    # Remove tempdir
    rm -rf "${package_temporary_dir}"
}
trap cleanup EXIT INT

# Set up the build environment docker image
docker build . -t "${image_name}" -f ./${dockerfile}
# Build the APKs and copy out to ${package_temporary_dir}
docker run --rm -e "RELEASE=${release}" -v "${package_temporary_dir}:/dist" "${image_name}"
# Correct ownership on the APKs (as current user, then as root if that fails)
chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null \
  || sudo chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null
# Move the APKs to the parent directory
mkdir -p ../bin &>/dev/null
mv "${package_temporary_dir}"/apk/*.apk ../bin
