Secure PV Access
================

By default, the PV Access server and client will use plain TCP sockets to communicate.
Secure PV Access uses Transport Layer Security (TLS) sockets.
TLS sockets, also known as secure sockets, are encrypted.
Clients will only communicate with trusted servers, and servers can
determine the identity of their clients in a trusted way.

Secure PV Access is under development for PVXS, the current C++ implementation
of PV Access. This java implementation aims to be compatible with recent versions of PVXS.
Secure PV Access is not supported in the original C++ (pvAccessCpp) and Java (pvAccessJava) implementations,
but PVXS and this java library can still communicate with the original implementations
using plain TCP sockets.

TLS relies on private and public encryption key pairs, where public keys are
exchanged in the form of certificates.
In a secure EPICS environment, the PV Access Certificate Management Service (pvacms)
issues certificates for servers and clients and allows online checks of their
validity.


PV Access Certificate Management Service (pvacms)
=================================================

An EPICS administrator needs to deploy `pvacms` as a service and maintain
certificates for servers (IOCs) and clients (users running CS-Studio
as well as IOCs reading from other IOCs).
This is an example recipe for getting started.

1) Build EPICS base and PVXS as described on
   https://george-mcintyre.github.io/pvxs/spvaqstart.html

2) Start `pvacms -v`. It will create several files, including

 * `~/.config/pva/1.3/admin.p12`: Certificate for the `admin` user
 
3) For an IOC, request a hybrid server and client certificate.
   Note its "Certificate identifier":

   ```
   $ authnstd --name ioc --cert-usage hybrid
   Keychain file created   : /home/user/.config/pva/1.3/server.p12
   Certificate identifier  : e53ed409:15273288300286014953
   ```

   As `admin`, accept that certificate:

   ```
   $ EPICS_PVA_TLS_KEYCHAIN=~/.config/pva/1.3/admin.p12  \
     pvxcert --approve e53ed409:15273288300286014953
   Approve ==> CERT:STATUS:e53ed409:15273288300286014953 ==> Completed Successfully
   ```

 * `~/.config/pva/1.3/server.p12`: Our server certificate (hybrid, for IOC) 

4) Request a client certificate, note its identifier:

   ```
   $ authnstd 
   Keychain file created   : /home/user/.config/pva/1.3/client.p12
   Certificate identifier  : e53ed409:11521018863975115478
   ```

   Accept that certificate: 

   ```
   $ EPICS_PVA_TLS_KEYCHAIN=~/.config/pva/1.3/admin.p12  \
     pvxcert --approve  e53ed409:11521018863975115478
   Approve ==> CERT:STATUS:e53ed409:11521018863975115478 ==> Completed Successfully
   ```

 * `~/.config/pva/1.3/client.p12`: Our client (user) certificate


You now have a server and a client certificate.
Example for checking the status:

```
$ pvxcert -f ~/.config/pva/1.3/client.p12
...
Subject        : CN=fred, C=US, O=host.site.org
...
Cert Expires   : Wed Apr 16 18:08:58 2025 UTC
...
Certificate ID : e53ed409:11521018863975115478
Status         : VALID
...
```

To list certificate details:

```
keytool -list -v -keystore ~/.config/pva/1.3/client.p12 -storepass ""
```

For a test setup, all the above can be executed by a single user on one host.
In a production setup, however, each human user should only have access to their own `client.p12` file.
Pseudo-users running IOCs would have a `server.p12` file.
Only an admin user on a designated host would have access to the remaining `pvacms` files,
including the `admin.p12` file that permits accepting and revoking certificates.


Secure IOC
==========

Example for running a secure IOC:

```
$ EPICS_PVAS_TLS_KEYCHAIN=~/.config/pva/1.3/server.p12  \
  softIocPVX -m user=fred -d pvxs/test/testioc.db
```

The command `pvxsr 10` will list all connected clients
with their connection credentials.


Secure Java PVA Client
======================

Example for running Java PVA client command line tool:

```
$ export EPICS_PVA_TLS_KEYCHAIN=~/.config/pva/1.3/client.p12
$ pvaclient monitor -v 5 fred:aiExample
```

Example for running CS-Studio:

```
$ export EPICS_PVA_TLS_KEYCHAIN=~/.config/pva/1.3/client.p12
$ phoebus.sh
```


For more, refer to the PVXS documentation.


--------------------------------------------------------


Manually creating certificates
==============================

In this section we describe an earlier approach to creating certificates.
It is left for reference, the suggested approach is now based on `pvacms`.

We start with a minimal setup for initial tests.

Step 1: Create a server KEYSTORE that contains a public and private key.
-------

The server passes the public key to clients. Clients then use that to encrypt messages
to the server which only the server can decode with its private key.

```
keytool -genkey -alias mykey -dname "CN=server" -keystore KEYSTORE -storepass changeit -keyalg RSA
```

To check, note "Entry type: PrivateKeyEntry" because the certificate holds both a public and private key:

```
keytool -list -v -keystore KEYSTORE -storepass changeit
```


Step 2: Create a client TRUSTSTORE to register the public server key
-------

Clients check a list of public keys to identify trusted servers.
Clients can technically use the keystore we just created, but
they should really only have access to the server's public key,
not the server's private key.
In addition, you may want to add public keys from more than one server into
the client truststore.

First export the server's public key.

```
keytool -export -alias mykey -keystore KEYSTORE -storepass changeit -rfc -file mykey.cer
```

Import the certificate into a new client truststore.

```
keytool -import -alias mykey -file mykey.cer -keystore TRUSTSTORE -storepass changeit -noprompt
```

To check, note "Entry type: trustedCertEntry" because the truststore contains only public keys:

```
keytool -list -v -keystore TRUSTSTORE -storepass changeit
```

While the key and trust files were created with the Java keytool,
they are compatible with generic open SSL tools:

```
openssl pkcs12 -info -in KEYSTORE -nodes
```

The essential commands are also in `make_tls_simple.sh`


Step 3: Configure and run the demo server
-------

Set environment variable `EPICS_PVAS_TLS_KEYCHAIN` to inform the server about its keystore.
The format of this setting is `/path/to/file;password`.
If you used `make_tls_simple.sh`, that would be `demo/KEYSTORE;changeit`.

Then run a demo server:

```
export EPICS_PVAS_TLS_KEYCHAIN="/path/to/KEYSTORE;changeit"
java -cp target/classes org.epics.pva.server.ServerDemo
```


Step 4: Configure and run the demo client
-------

Set environment variable `EPICS_PVA_TLS_KEYCHAIN` to inform the client about its truststore.
If you used `make_tls_simple.sh`, that would be `demo/TRUSTSTORE;changeit`.
Then run a demo client (`-v 5` to see protocol detail):

```
export EPICS_PVA_TLS_KEYCHAIN="/path/to/TRUSTSTORE;changeit"
java -cp target/classes org.epics.pva.client.PVAClientMain get -v 5 demo
# Or:  ./pvaclient get -v 5 demo
```

On both the server and the client note how they mention "TLS" in their log messages.

Logging
-------

The PVA server and client code logs in the `org.epics.pva` package.
For lower level encryption information, add `-Djavax.net.debug=all`.

For example, when receiving subscription updates for a string PV with status and timestamp,
the log messages indicate that the encrypted data size of 74 bytes is almost twice the size
of the decrypted payload of 41 bytes: 

```
javax.net.ssl|DEBUG|91|TCP receiver /127.0.0.1|2023-05-05 15:57:37.299 EDT|SSLSocketInputRecord.java:214|READ: TLSv1.2 application_data, length = 74
javax.net.ssl|DEBUG|91|TCP receiver /127.0.0.1|2023-05-05 15:57:37.299 EDT|SSLSocketInputRecord.java:493|Raw read (
  0000: 65 27 79 1D 33 5D 7C AB   A6 06 86 31 0D 9C 10 70  e'y.3].....1...p
  0010: 3C 85 58 D3 FD AA 81 57   00 0A 04 DF 37 3D EE 31  <.X....W....7=.1
  0020: C4 2A CE E5 24 A3 E8 F3   F2 B6 7E 64 BE 32 9E 71  .*..$......d.2.q
  0030: F8 29 81 C4 3F 61 D0 E4   D1 D5 A7 BC 5A 21 D0 B8  .)..?a......Z!..
  0040: F5 5F DD 5E DE 59 79 F8   45 86                    ._.^.Yy.E.
)
javax.net.ssl|DEBUG|91|TCP receiver /127.0.0.1|2023-05-05 15:57:37.300 EDT|SSLCipher.java:1935|Plaintext after DECRYPTION (
  0000: CA 02 40 0D 21 00 00 00   01 00 00 00 00 02 02 03  ..@.!...........
  0010: 0B 56 61 6C 75 65 20 69   73 20 38 33 B1 5F 55 64  .Value is 83._Ud
  0020: 00 00 00 00 10 0E AD 11   00                       .........
```

Firewalls
---------

Tests from other hosts may require firewall openings.
For RHEL9, use this get both immediate openings and have them
persist a reboot:

```
# Default UDP search port
sudo firewall-cmd --zone=public --add-port=5076/udp
sudo firewall-cmd --zone=public --add-port=5076/udp --permanent

# Default plain TCP port
sudo firewall-cmd --zone=public --add-port=5075/tcp
sudo firewall-cmd --zone=public --add-port=5075/tcp --permanent

# Default secure (TLS) TCP port
sudo firewall-cmd --zone=public --add-port=5076/tcp
sudo firewall-cmd --zone=public --add-port=5076/tcp --permanent

# Show
sudo firewall-cmd --info-zone=public
```

Use a Certification Authority
-----------------------------

Instead of creating a separate key pair for each server and telling each client to trust all those public server keys,
we can use a Certification Authority (CA).
That way, clients trust the CA, and you can create individual key pairs for servers without need
to distribute their public keys to each client.

For a stand-alone demo, we create our own CA, and make its public certificate available as `myca.cer`:

```
keytool -genkeypair -alias myca -keystore ca.p12 -storepass changeit -dname "CN=myca" -keyalg RSA -ext BasicConstraints=ca:true
keytool -list -v                -keystore ca.p12 -storepass changeit
keytool -exportcert -alias myca -keystore ca.p12 -storepass changeit -rfc -file myca.cer
keytool -printcert -file myca.cer
```

Create a truststore that holds the public certificate of our CA.
A client that simply wants to trust any CA-signed certificate
could use this, it's equivalent to the `TRUSTSTORE` from the original example.

```
keytool -importcert -alias myca  -keystore trust_ca.p12 -storepass changeit -file myca.cer  -noprompt
keytool -list -v                 -keystore trust_ca.p12 -storepass changeit
```

Now create a server keypair for use by the IOC:

```
keytool -genkeypair -alias myioc -keystore ioc.p12 -storepass changeit -dname "CN=myioc" -keyalg RSA
keytool -list -v                 -keystore ioc.p12 -storepass changeit
```

It starts out as a "self-signed certificate" with `myioc` as both "owner" and "issuer".
Create a certificate signing request. The CSR could be sent to a commercial CA, but we sign it with our own CA.

```
keytool -certreq -alias myioc -keystore ioc.p12 -storepass changeit -file myioc.csr
keytool -gencert -alias myca  -keystore ca.p12  -storepass changeit -ext SubjectAlternativeName=DNS:myioc -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -infile myioc.csr -outfile myioc.cer
keytool -printcert -file myioc.cer
```

Import the signed certificate into the ioc keystore. Since `ioc.cer` is signed by `myca`, which
is not a generally known CA, we will get an error "Failed to establish chain"
unless we first import `myca.cer` to trust out local CA.

```
keytool -importcert -alias myca  -keystore ioc.p12 -storepass changeit -file myca.cer  -noprompt
keytool -importcert -alias myioc -keystore ioc.p12 -storepass changeit -file myioc.cer
keytool -list -v                 -keystore ioc.p12 -storepass changeit
```

We can now run the server with `EPICS_PVAS_TLS_KEYCHAIN=/path/to/ioc.p12;changeit` and clients with
`EPICS_PVA_TLS_KEYCHAIN=/path/to/trust_ca.p12;changeit`.

See `make_tls_ca.sh` for a copy of the essential commands.
You can create additional server files `ioc1.p12`, `ioc2.p12` and have each IOC use its own key pair.
Clients will trust them without any changes to the `trust_ca.p12`
as long as the IOC certificates are signed by your CA.


Add client certificate
----------------------

Using certificates as described so far results in encrypted communication.
Clients trust servers either because a server's public key is directly known to the client,
or a server's certificate is signed by a CA known to the client.
Servers, on the other hand, do not know for certain who the client is.
Authentication is handled by the client sending a name, typically based on the user name provided by the operating system.
This is called "ca authentication" because it mimics the Channel Access behavior.

By adding a client certificate that is signed by the CA, the client identifies
via that signed certificate. This is called "x509 authentication".

Create a client certificate that uses the name "Fred F." for x509 authentication:

```
keytool -genkeypair -alias myclient -keystore client.p12 -storepass changeit -dname "CN=Fred F." -keyalg RSA
keytool -list -v                 -keystore client.p12 -storepass changeit
```

Sign the client certificate with the CA and update the client file with that signed certificate:

```
keytool -certreq -alias myclient -keystore client.p12 -storepass changeit -file myclient.csr
keytool -gencert -alias myca     -keystore ca.p12     -storepass changeit -ext SubjectAlternativeName=DNS:client -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -infile myclient.csr -outfile myclient.cer
keytool -printcert -file myclient.cer

keytool -importcert -alias myca  -keystore client.p12 -storepass changeit -file myca.cer  -noprompt
keytool -importcert -alias myclient -keystore client.p12 -storepass changeit -file myclient.cer
keytool -list -v                 -keystore client.p12 -storepass changeit
```

In the last step, note that the file `client.p12` uses an "alias" and "DNSName" of "myclient",
and an "owner" of `CN=Fred F.`. The latter "Common Name" listed as the "owner"
is used as the client name with x509 authentication.

We can now run the server with `EPICS_PVAS_TLS_KEYCHAIN=/path/to/ioc.p12;changeit` and a client with
`EPICS_PVA_TLS_KEYCHAIN=/path/to/client.p12;changeit`.
The server will identify the client as "Fred F.".

By default, the server supports clients with certificate and x509 authentication,
but client certificates are not required.
By setting `EPICS_PVAS_TLS_OPTIONS="client_cert=require"`, the server will
abort the initial TLS handshake for clients that do not have a certificate.


In total, we now have the following:

 * `KEYSTORE`, `TRUSTSTORE`:
   Server keystore and client truststore from the first, minimalistic example.
   All IOCs could use the KEYSTORE and all clients the TRUSTSTORE, but
   example becomes cumbersome when we want to add certificates that are specific
   to IOCs, or if clients should use x509 authentication.

 * `ca.p12`:
   Keystore with public and private key of our Certification Authority (CA).
   This file needs to be guarded because it allows creating new IOC
   and client keystores.
   
 * `myca.cer`:
   Public certificate of the CA.
   Imported into any `*.p12` that needs to trust the CA.
   
 * `trust_ca.p12`:
   Truststore with public certificate of the CA.
   Equivalent to `myca.cer`, and some tools might directly use `myca.cer`,
   but `trust_ca.p12` presents it in the commonly used  PKCS12 `*.p12` file format. 
   Clients can set their `EPICS_PVA_TLS_KEYCHAIN` to this file to
   communicate with IOCs, resulting in encryption and "ca" authentication.
   
 * `ioc.p12`:
   Keystore with public and private key of an IOC.
   The public key certificate is signed by the CA so that clients
   will trust it.
   To be used with `EPICS_PVAS_TLS_KEYCHAIN` of IOCs.
   
 * `myioc.cer`, `myioc.csr`: Public IOC certificate and certificate signing request.
   Intermediate files used to sign the IOC certificate.
   May be deleted.

 * `client.p12`:
   Keystore with public and private key of a client,
   providing a client name for x509 authentication.
   Clients can set their `EPICS_PVA_TLS_KEYCHAIN` to this file to
   communicate with IOCs using x509 authentication.
   If the server runs with `EPICS_PVAS_TLS_OPTIONS="client_cert=require"`,
   the clients MUST use a client certificate like `client.p12`.
   Clients with just `trust_ca.p12` will fail during the initial TLS handshake.

 * `myclient.cer`, `myclient.csr`:
   Intermediate files used to sign the client certificate.
   May be deleted.


The example can be extended by creating one certificate per IOC and one per client,
and by adding one or more intermediate CA levels instead of directly
signing IOC and client certificates with the "root" CA.

