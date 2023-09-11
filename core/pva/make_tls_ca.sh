# More elaborate example, see TLS.md "Use a Certification Authority"
rm -rf demo
mkdir demo
cd demo

# Create our own CA, and make its public certificate available as `myca.cer`:
keytool -genkeypair -alias myca -keystore ca.p12 -storepass changeit -dname "CN=myca" -keyalg RSA -ext BasicConstraints=ca:true
keytool -list -v                -keystore ca.p12 -storepass changeit
keytool -exportcert -alias myca -keystore ca.p12 -storepass changeit -rfc -file myca.cer
keytool -printcert -file myca.cer

# For clients, create a truststore that holds the public certificate of our CA
keytool -importcert -alias myca  -keystore trust_ca.p12 -storepass changeit -file myca.cer  -noprompt
keytool -list -v                 -keystore trust_ca.p12 -storepass changeit

# Now create a server keypair for use by the IOC:
keytool -genkeypair -alias myioc -keystore ioc.p12 -storepass changeit -dname "CN=myioc" -keyalg RSA
keytool -list -v                 -keystore ioc.p12 -storepass changeit

# Create signing request, sign with our CA, import signed cert into ioc.p12
keytool -certreq -alias myioc -keystore ioc.p12 -storepass changeit -file myioc.csr
keytool -gencert -alias myca  -keystore ca.p12  -storepass changeit -ext SubjectAlternativeName=DNS:myioc -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -infile myioc.csr -outfile myioc.cer
keytool -printcert -file myioc.cer
keytool -importcert -alias myca  -keystore ioc.p12 -storepass changeit -file myca.cer  -noprompt
keytool -importcert -alias myioc -keystore ioc.p12 -storepass changeit -file myioc.cer
keytool -list -v                 -keystore ioc.p12 -storepass changeit

# Create client keypair so client can authenticate as "Fred F."
keytool -genkeypair -alias myclient -keystore client.p12 -storepass changeit -dname "CN=Fred F." -keyalg RSA
keytool -list -v                 -keystore client.p12 -storepass changeit

# Sign client certificate with our CA
keytool -certreq -alias myclient -keystore client.p12 -storepass changeit -file myclient.csr
keytool -gencert -alias myca     -keystore ca.p12     -storepass changeit -ext SubjectAlternativeName=DNS:client -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -infile myclient.csr -outfile myclient.cer
keytool -printcert -file myclient.cer
keytool -importcert -alias myca  -keystore client.p12 -storepass changeit -file myca.cer  -noprompt
keytool -importcert -alias myclient -keystore client.p12 -storepass changeit -file myclient.cer
keytool -list -v                 -keystore client.p12 -storepass changeit

echo "*************************************************************"
echo "***************** trust_ca **********************************"
echo "*************************************************************"
keytool -list -v -keystore trust_ca.p12 -storepass changeit

echo "*************************************************************"
echo "***************** IOC ***************************************" 
echo "*************************************************************"
keytool -list -v -keystore ioc.p12 -storepass changeit

echo "*************************************************************"
echo "***************** Client ************************************" 
echo "*************************************************************"
keytool -list -v -keystore client.p12 -storepass changeit

