#!/bin/sh
set -e
java -jar -Dgrails.env=$GRAILS_ENV /app/myhab.jar $@