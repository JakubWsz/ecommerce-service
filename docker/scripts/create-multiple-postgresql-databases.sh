#!/bin/bash

set -e
set -u

function create_user_and_database() {
    local database=$1
    echo "  Tworzenie użytkownika i bazy danych '$database'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE USER $database;
        CREATE DATABASE $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $database;
EOSQL
    local event_store_db="${database}_event_store"
    echo "  Tworzenie bazy event store '$event_store_db'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $event_store_db;
        GRANT ALL PRIVILEGES ON DATABASE $event_store_db TO $database;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Żądanie utworzenia wielu baz danych: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_user_and_database $db
    done
    echo "Wszystkie bazy danych zostały utworzone"
fi
