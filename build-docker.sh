#!/bin/bash

TAG=s7i/doer
VERSION=0.1.1
VCS_REF=$(git rev-parse HEAD)

main() {
    info

    case $1 in
        jre11)
            build_jdk11
            ;;
        *)
        with_builder
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
    echo "$VERSION-$branch"
  else
    echo $VERSION
  fi
}

with_builder () {
    runBuild
}

build_jdk11 () {
    ./gradlew distTar --console=plain
    runBuild "Dockerfile.jre11"
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

    docker build -t $tag --build-arg VERSION=$VERSION --build-arg VCS_REF=$VCS_REF $dockerFile .
}

# call the main function
main $@
