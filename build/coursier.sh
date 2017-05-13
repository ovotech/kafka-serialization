#!/usr/bin/env bash

set -e

echo 'Installing coursier'
curl -L -o ${HOME}/bin/coursier https://git.io/vgvpD && chmod +x ${HOME}/bin/coursier

echo 'Adding coursier plugin to sbt'
mkdir -p $HOME/.sbt/0.13/plugins
echo 'addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC2")' > $HOME/.sbt/0.13/plugins/coursier.sbt