#!/bin/bash

args=("$@")

TAG=s7i/doer
VERSION=$(cat ./version)
VCS_REF=$(git describe --tags --always --dirty)

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
    echo Tag: $TAG:$VERSION
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

slim_build () {
    ./gradlew test distTar --console=plain

    if [[ ! -e "./build/distributions/doer-${VERSION}.tar" ]]; then
      echo "missing doer.tar"
      exit 1
    fi

    cp ./build/distributions/doer-${VERSION}.tar ./doer.tar
    runBuild "Dockerfile-slim"
    rm ./doer.tar
}

runBuild () {
    local dockerFile

    if [[ -n "$1" ]]; then
        dockerFile="-f $1"
        echo "Using Dockerfile: $1"
    else
        echo "Using default dockerfile"
    fi
    local tag=$TAG:$(versionTag)
    echo "Docker Tag: $tag"

    docker build \
      --progress=plain \
      -t $tag \
      --build-arg VERSION=$VERSION \
      --build-arg BUILD_DATE="$(date +"%Y-%m-%dT%H:%M:%S%z")" \
      --build-arg VCS_REF=$VCS_REF \
      $dockerFile .
}

# call the main function
main "${args[@]}"
