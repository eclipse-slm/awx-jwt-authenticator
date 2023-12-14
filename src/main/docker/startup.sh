#! /bin/bash

# Start App
java -Djavax.net.ssl.trustStore=/app/truststore/truststore.p12 -jar /app/app.jar
