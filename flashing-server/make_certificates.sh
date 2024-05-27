
export KEYTOOL=$JAVA_HOME/bin/keytool
rm *.pem *.jks *.p12
${KEYTOOL}  -storepass ${STORE_PASS} -genkeypair -keyalg EC -keystore root.jks -alias root -ext bc:c -dname "O=1D6 Foundation, L=New York City, S=New York, C=USA"
${KEYTOOL}  -storepass ${STORE_PASS} -genkeypair -keyalg EC -keystore ca.jks -alias ca -ext bc:c -dname "O=1D6 Foundation, L=New York City, S=New York, C=USA"
${KEYTOOL}  -storepass ${STORE_PASS}  -genkeypair -keyalg EC -keystore server.jks -alias server -dname "CN=localhost, OU=DiceCTF Finals, O=1D6 Foundation, L=New York City, S=New York, C=USA"

${KEYTOOL} -storepass ${STORE_PASS} -keystore root.jks -alias root -exportcert -rfc > root.pem
${KEYTOOL} -storepass ${STORE_PASS} -keystore ca.jks -certreq  -keyalg EC -alias ca | ${KEYTOOL} -storepass ${STORE_PASS} -keystore root.jks -gencert -keyalg EC  -alias root -ext BC=0 -rfc > ca.pem

cat root.pem ca.pem > cachain.pem
${KEYTOOL} -storepass ${STORE_PASS} -keystore ca.jks -importcert -alias ca -file cachain.pem

${KEYTOOL} -storepass ${STORE_PASS} -keystore server.jks -certreq -keyalg EC -alias server | ${KEYTOOL} -keyalg EC  -storepass ${STORE_PASS} -keystore ca.jks -gencert -alias ca -ext ku:c=dig,keyEncipherment -rfc > server.pem
cat root.pem ca.pem server.pem > serverchain.pem
${KEYTOOL} -storepass ${STORE_PASS} -keystore server.jks -importcert -alias server -file serverchain.pem

${KEYTOOL} -importkeystore \
    -srckeystore server.jks \
    -destkeystore server.p12 \
    -deststoretype PKCS12 \
    -deststorepass ${STORE_PASS} \
    -destkeypass ${STORE_PASS} -srcstorepass ${STORE_PASS}
openssl pkcs12 -in server.p12  -nodes -nocerts -out server_privkey.pem -passin pass:${STORE_PASS} -passout pass:${STORE_PASS}
openssl pkcs12 -export -in serverchain.pem -inkey server_privkey.pem -out server_legacy.p12 -legacy -passin pass:${STORE_PASS}  -passout pass:${STORE_PASS}
