#! /bin/bash

# Wait until AWX is running
until curl -m 5 -s --location --request GET "$AWX_SCHEME://$AWX_HOST:$AWX_PORT"; do
  echo "AWX is unavailable - sleeping"
  sleep 1
done

# Wait until Vault is running
export VAULT_TOKEN="UnusedDummyToken"
VAULT_ROLEID_FILE=/opt/vault/awx-jwt-authenticator/role_id
VAULT_SECRETID_FILE=/opt/vault/awx-jwt-authenticator/secret_id
until [ -f "$VAULT_ROLEID_FILE" ]; do
  echo "Vault is not initialized - sleeping"
  sleep 3
done

export VAULT_APPROLE_ROLEID=$(cat $VAULT_ROLEID_FILE)
export VAULT_APPROLE_SECRETID=$(cat $VAULT_SECRETID_FILE)

# Wait until Keycloak configuration is present
KEYCLOAK_CONFIG_DIRECTORY="/app/keycloak/"
while [ -z "$(ls -A $KEYCLOAK_CONFIG_DIRECTORY)" ]; do
  echo "Keycloak config file(s) in $KEYCLOAK_CONFIG_DIRECTORY missing -> sleeping"
  sleep 3
done
export KEYCLOAK_AUTHSERVERURL=$(jq -r .\"auth-server-url\" /app/keycloak/fabos-keycloak.json)
export KEYCLOAK_REALM=$(jq -r .realm /app/keycloak/fabos-keycloak.json)
export KEYCLOAK_RESOURCE=$(jq -r .resource /app/keycloak/fabos-keycloak.json)
export KEYCLOAK_CREDENTIALS_SECRET=$(jq -r .credentials.secret /app/keycloak/fabos-keycloak.json)

# Wait until Keycloak is running
until curl -m 5 -s --location --request GET "${KEYCLOAK_AUTHSERVERURL}realms/${KEYCLOAK_REALM}/.well-known/openid-configuration"; do
  echo "Keycloak is unavailable -> sleeping"
  sleep 1
done

# Wait for AWX configuration
AWX_USERNAME_FILE="/app/awx/awx_username"
until [ -f "$AWX_USERNAME_FILE" ]; do
  echo "Config file for AWX username '$AWX_USERNAME_FILE' missing -> sleeping"
  sleep 3
done
export AWX_USERNAME=$(cat "$AWX_USERNAME_FILE")
echo "AWX_USERNAME: $AWX_USERNAME"

AWX_PASSWORD_FILE="/app/awx/awx_password"
until [ -f "$AWX_PASSWORD_FILE" ]; do
  echo "Config file for AWX password '$AWX_PASSWORD_FILE' missing -> sleeping"
  sleep 3
done
export AWX_PASSWORD=$(cat "$AWX_PASSWORD_FILE")
echo "AWX_PASSWORD: $AWX_PASSWORD"

# Start App
java -Djavax.net.ssl.trustStore=/app/truststore/truststore.p12 -jar /app/app.jar
