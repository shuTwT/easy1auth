#!/bin/sh
set -eu

marker=/var/run/easy1auth/maintenance
mkdir -p "$(dirname "$marker")"
if [ "${MAINTENANCE_MODE:-on}" = "on" ]; then
  touch "$marker"
else
  rm -f "$marker"
fi

exec nginx -g 'daemon off;'
