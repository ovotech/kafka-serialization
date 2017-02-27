#!/usr/bin/env bash

function start_services {

    echo "Docker services... Starting."

    docker-compose -f docker/docker-compose.yaml up -d 2> /dev/null

    echo "Docker services... Started."

}

function stop_services {

    echo "Docker services... Stopping."

    docker-compose -f docker/docker-compose.yaml down 2> /dev/null

    echo "Docker services... Stopped."

}

function is_installed {

    command -v $1 > /dev/null 2>&1 || { echo >&2 "$1 is not installed... Aborting."; exit 1;}

}

echo "Integration tests... Started."

is_installed sbt

is_installed docker-compose

is_installed curl

is_installed jq

stop_services

start_services

echo "Docker services... Waiting."

sleep 10 # Has kafka health check REST API?

echo "Docker services... Ready"

sbt it:test

sbt_exit_code=$?

stop_services

if [ "$sbt_exit_code" == "0" ]
then
    echo "Integration tests... Succeeded."
    exit 0;
else
    echo "Integration tests... Failed."
    exit 1;
fi