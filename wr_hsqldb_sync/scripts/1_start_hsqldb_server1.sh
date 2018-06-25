#!/bin/bash

DIR=`dirname "$(readlink -f "$0")"`
java -cp  $DIR/../utils/hsqldb-2.4.0/hsqldb/lib/hsqldb.jar org.hsqldb.server.Server --database.0 file:/tmp/teste1 --dbname.0 teste1
