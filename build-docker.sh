#!/bin/bash

args=("$@")

NAME="s7i/doer"
VERSION=$(cat ./version)
VCS_REF=$(git describe --tags --always --dirty)

REMOTE_REPO=${REMOTE_REPO:-"dwarf.syg:5817/docker"}
DOCKER_LOGIN_URL=${DOCKER_LOGIN_URL:-"http://dwarf.syg:5817/repository/docker/"}
DOCKER_USERNAME=${DOCKER_USERNAME:-"mario"}

main() {
    info Docker build helper script

    case $1 in
        builder)

            with_builder
            ;;
        *)
        slim_build
    esac
}

info() {
    echo
    echo Name and version: $NAME:$VERSION
    echo Git-sha: $VCS_REF
    echo
}

versionTag() {
  local mains=("master", "main")
  local branch=$(git rev-parse --abbrev-ref HEAD)

  if [[ ! " ${mains[*]} " =~ " ${branch} " ]]; then
    echo "$VERSION-${branch//\//\_}"
  else
    echo $VERSION
  fi
}

with_builder () {
    runBuild
}

function slim_build () {
    DIST_TAR="./build/distributions/doer-${VERSION}.tar"
    if [[ ! -e ${DIST_TAR} ]]; then
      ./gradlew distTar --console=plain --no-daemon
      if [[ ! -e ${DIST_TAR} ]]; then
        echo "missing doer.tar: (${DIST_TAR})"
        exit 1
      fi
    fi

    ln $DIST_TAR ./doer.tar
    runBuild "Dockerfile-slim"
    rm ./doer.tar
}

function docker_tags() {
      echo "--tag $NAME:${IMAGE_BUILD_TAG:-$(versionTag)}"
}

runBuild () {
    local dockerFile

    if [[ -n "$1" ]]; then
        dockerFile="-f $1"
        echo "Using Dockerfile: $1"
    else
        echo "Using default dockerfile"
    fi

    docker build \
      --progress=plain \
      $(docker_tags) \
      --build-arg VERSION=$VERSION \
      --build-arg BUILD_DATE="$(date +"%Y-%m-%dT%H:%M:%S%z")" \
      --build-arg VCS_REF=$VCS_REF \
      $dockerFile .

    if [[ "x${DOCKER_PUBLISH_IMAGE}" == "xYES" ]]; then
      echo "Publishing to remote repository: ${REMOTE_REPO}"

      echo ${DOCKER_PASSWD} | docker login \
      ${DOCKER_LOGIN_URL} \
      --username ${DOCKER_USERNAME} \
      --password-stdin

      LOCAL_NAME="${NAME}:${IMAGE_BUILD_TAG}"
      REMOTE_NAME="${REMOTE_REPO}/${LOCAL_NAME}"
      docker tag ${LOCAL_NAME} ${REMOTE_NAME}
      docker push ${REMOTE_NAME}
      docker image rm ${REMOTE_NAME}
    fi
}

# call the main function
main "${args[@]}"
