#!/usr/bin/env bash

set -e

echo 'Discarding changes'
git checkout -- .

echo 'Publishing the project'
sbt +publish
