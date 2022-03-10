-- sql script to create the user and database for this project
-- we have chosen PostgreSQL for our project, 
-- so the script can run under the root user (probably "postgres") in psql

CREATE USER dev WITH PASSWORD 'password';

CREATE DATABASE it5100a_proj WITH OWNER=dev;