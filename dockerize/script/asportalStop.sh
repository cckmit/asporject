#!/bin/bash
export dockerHome=/usr/local/dockerize
containerId=`docker ps | grep 'as-portal' | awk '{print $1}'`
docker stop ${containerId}