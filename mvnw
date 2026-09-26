#!/bin/sh
#
# Maven Wrapper Startup Script
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Resolve the location of this script
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done
SAVED="$(pwd)"
cd "$(dirname "$PRG")" >/dev/null
BASEDIR="$(pwd -P)"
cd "$SAVED" >/dev/null

MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-$BASEDIR}
export MAVEN_PROJECTBASEDIR

# Java command to use
if [ -z "$JAVA_HOME" ] ; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

# Maven wrapper JAR
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

# Download wrapper if missing
if [ ! -f "$WRAPPER_JAR" ] ; then
  echo "Downloading maven-wrapper.jar..."
  if command -v curl >/dev/null 2>&1 ; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget >/dev/null 2>&1 ; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: Neither curl nor wget found. Please install one or download maven-wrapper.jar manually."
    exit 1
  fi
fi

exec "$JAVA_CMD" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"