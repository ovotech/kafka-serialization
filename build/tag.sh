#!/usr/bin/env bash

set -e

echo 'Fetching tag from remote...'
git tag -l | xargs git tag -d
git fetch --tags

echo 'Extracting version from sbt project...'
sbt release-write-next-version
version=`cat target/next_release_version`

echo 'Tagging the current commit'
git tag -a v${version} -m "Release version "${version}

echo 'Pushing tag v'${version}' to origin'
git push origin v${version}