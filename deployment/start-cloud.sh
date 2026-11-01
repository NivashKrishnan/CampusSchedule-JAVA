#!/bin/sh
set -eu
if [ "${TIMETABLE_STORAGE:-}" = "files" ]; then
  node /opt/blob-bridge/server.mjs &
  bridge_pid=$!
  catalina.sh run &
  tomcat_pid=$!
  trap 'kill "$bridge_pid" "$tomcat_pid" 2>/dev/null || true' EXIT INT TERM
  while kill -0 "$bridge_pid" 2>/dev/null && kill -0 "$tomcat_pid" 2>/dev/null; do sleep 2; done
  exit 1
fi
exec catalina.sh run
