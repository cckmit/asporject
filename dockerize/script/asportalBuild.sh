#!/bin/bash
export dockerHome=/usr/local/dockerize
version=`cat ${dockerHome}/ASPortal/version.txt`
cd  ${dockerHome}/ASPortal/
docker build -f ${dockerHome}/ASPortal/Dockerfile -t asportal_${version} .