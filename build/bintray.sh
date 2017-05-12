#!/usr/bin/env bash

set -e

echo 'Creating bintray credentials'
if [[ "$CIRCLE_BRANCH" == "master" ]]; then
  mkdir $HOME/.bintray
  cat > $HOME/.bintray/.credentials <<EOF
realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_USER
password = $BINTRAY_PASS
EOF
fi
