#!/bin/bash

#
# Copyright  2022 LSD Information Technology (Pty) Ltd
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

# Usage
usage() {
  cat <<EOF
Usage: $0 options

OPTIONS:
   -h      Show this message
   -t      The docker image tag to run; default "latest"
EOF
}

# Defaults
TAG="latest"

while getopts "ht:" OPTION; do
  case $OPTION in
  h)
    usage
    exit 1
    ;;
  t)
    TAG=${OPTARG}
    ;;
  ?)
    usage
    exit
    ;;
  esac
done

docker run --name=ahoy --rm -p 8080:8080 --env PROFILES=dev,keycloak lsdopen/ahoy:$TAG
