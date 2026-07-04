#!/bin/sh
set -eu

AGENT_OPTS=""
if [ -n "${CLASSFINAL_PASSWORD:-}" ] && [ "${CLASSFINAL_PASSWORD}" != "#" ]; then
  AGENT_OPTS="-pwd=${CLASSFINAL_PASSWORD}"
fi

exec java ${JAVA_OPTS} -javaagent:/app/app.jar="${AGENT_OPTS}" -jar /app/app.jar
