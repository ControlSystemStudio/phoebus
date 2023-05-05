Secure Socket Support
=====================

The server and client provide a simple preview of TLS-enabled "secure socket" communication.

By default, the server and client will use plain TCP sockets to communicate.
By configuring a keystore for the server and a truststore for the client,
the communication can be switched to secure (TLS) sockets.
The sockets are encrypted, and clients will only communicate with trusted servers.

Step 1: Create a server KEYSTORE that contains a public and private key.
-------

The server passes the public key to clients. Clients then use that to encrypt messages
to the server which only the server can decode with its private key.

```
keytool -genkey -alias mykey -keystore KEYSTORE -keyalg RSA
```

Use password `changeit`.


To check, note "Entry type: PrivateKeyEntry" because the certificate holds both a public and private key:

```
keytool -list -v -keystore KEYSTORE
```

This example so far only uses self-signed certificates.
An operational setup might prefer to sign them by a publicly trusted certificate authority.


Step 2: Create a client TRUSTSTORE to register the public server key
-------

Clients check this list of public keys to identify trusted servers.
Clients can technically use the keystore we just created, but
they should really only have access to the server's public key.
In addition, you may want to add public keys from more than one server into
the client truststore.

First export the server's public key.

```
keytool -export -alias mykey -keystore KEYSTORE -rfc -file mykey.cer
```

Import the certificate into a new client truststore.

```
keytool -import -alias mykey -file mykey.cer -keystore TRUSTSTORE
```

To check, note "Entry type: trustedCertEntry" because the truststore contains only public keys:

```
keytool -list -v -keystore TRUSTSTORE
```

While the key and trust files were created with the Java keytool,
they are compatible with generic open SSL tools:

```
openssl pkcs12 -info -in KEYSTORE -nodes
```


Step 3: Configure and run the demo server
-------

Set environment variables to inform the server about its keystore:

```
export EPICS_PVA_SERVER_KEYSTORE=/path/to/KEYSTORE
export EPICS_PVA_STOREPASS=changeit
```

Then run a demo server

```
java -cp target/classes org/epics/pva/server/ServerDemo
```


Step 4: Configure and run the demo client
-------

Set environment variables to inform the server about its keystore:

```
export EPICS_PVA_SERVER_KEYSTORE=/path/to/KEYSTORE
export EPICS_PVA_STOREPASS=changeit
```

Then run a demo client

```
java -cp target/classes org/epics/pva/client/PVAClientMain monitor demo
```

Add `-Djavax.net.debug=all` to see encryption information.

