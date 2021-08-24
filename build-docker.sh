TAG=s7i/doer
VERSION=0.1.1
VCS_REF=$(git rev-parse HEAD)

docker build -t $TAG:$VERSION --build-arg VERSION=$VERSION --build-arg VCS_REF=$VCS_REF .