# Minimal example, see TLS.md steps 1-4

rm -rf demo
mkdir demo
cd demo


# Creates a key pair for the server
keytool -genkey -alias mykey -dname "CN=server" -keystore KEYSTORE -storepass changeit -keyalg RSA
# Export the public key
keytool -export -alias mykey -keystore KEYSTORE -storepass changeit -rfc -file mykey.cer

# Create a trust store for the client to make it aware of the server's public key
keytool -import -alias mykey -file mykey.cer -keystore TRUSTSTORE -storepass changeit -noprompt

echo "*************************************************************"
echo "***************** KEYSTORE **********************************"
echo "*************************************************************"
keytool -list -v -keystore KEYSTORE -storepass changeit

echo "*************************************************************"
echo "***************** TRUSTSTORE ********************************" 
echo "*************************************************************"
keytool -list -v -keystore TRUSTSTORE -storepass changeit
