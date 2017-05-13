#!/usr/bin/env bash

set -e

SCALA_BINARY_VERSION=2.12
SCALAFMT_VERSION=0.7.0-RC1

echo 'Installing scalafmt'
coursier bootstrap --standalone com.geirsson:scalafmt-cli_${SCALA_BINARY_VERSION}:${SCALAFMT_VERSION} -o /usr/local/bin/scalafmt -f --main org.scalafmt.cli.Cli
