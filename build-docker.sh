TAG=s7i/doer
VERSION=0.1.1
VCS_REF=$(git rev-parse HEAD)

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

slim_build () {
    if [[ ! -e "./build/libs/doer-all.jar" ]]; then
        ./gradlew build --console=plain
    fi
    runBuild "Dockerfile-slim"
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

info

case $1 in
    slim)
        slim_build
        ;;         
    *)
    with_builder        
esac

