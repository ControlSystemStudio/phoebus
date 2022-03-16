
Installing TimescaleDB
======================

For complete instructions, see https://docs.timescale.com/latest/main
and https://www.postgresql.org/docs/12/index.html

Following is an example for RedHat 7.9, Postgres 14, TimescaleDB 2


Install Postgres and TimescaleDB
--------------------------------

Remove previous installation

    sudo yum remove postgresql12
    sudo yum remove postgresql12-libs
    sudo yum remove timescaledb-tools
    
Register RPM repo

```
sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-$(rpm -E %{rhel})-x86_64/pgdg-redhat-repo-latest.noarch.rpm

sudo tee /etc/yum.repos.d/timescale_timescaledb.repo <<EOL
[timescale_timescaledb]
name=timescale_timescaledb
baseurl=https://packagecloud.io/timescale/timescaledb/el/$(rpm -E %{rhel})/\$basearch
repo_gpgcheck=1
gpgcheck=0
enabled=1
gpgkey=https://packagecloud.io/timescale/timescaledb/gpgkey
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
metadata_expire=300
EOL

sudo yum update -y
```

Install

    sudo yum install -y timescaledb-2-postgresql-14
    
As a result, `/usr/pgsql-14` has binaries, `/var/lib/pgsql/14` has config and data.
Initialize the empty data dirrectory:

    sudo su postgres
    ls -l /var/lib/pgsql/14/data
    cd /usr/pgsql-14/bin
    ./pg_ctl -D /var/lib/pgsql/14/data initdb

Now there is a file /var/lib/pgsql/14/data/postgresql.conf, so enable timescaledb

    sudo su postgres
    # Answer 'y' to all the prompts:
    timescaledb-tune --pg-config=/usr/pgsql-14/bin/pg_config


Start Postgres for the first time
---------------------------------

    sudo su postgres
    # /usr/pgsql-14/bin/pg_ctl -D /var/lib/pgsql/14/data -l logfile start
    export PGDATA=/var/lib/pgsql/14/data
    cd /var/lib/pgsql/14
    /usr/pgsql-14/bin/pg_ctl -l logfile start

    cat logfile

    /usr/pgsql-14/bin/pg_ctl  status

Try to connect, then exit via \q

    psql

Change admin password:

    psql -U postgres
    ALTER USER postgres WITH PASSWORD 'NEW_PASSWORD';
    \q

Allow Remote Access
-------------------

.. in the database:

    sudo su postgres

    vi /var/lib/pgsql/14/data/postgresql.conf
    # Find 'listen_addresses', set to
    listen_addresses = '*'


    vi /var/lib/pgsql/14/data/pg_hba.conf 

    # Add these lines to end
    # 'md5' means request password. 'trust' would allow connection without pw
    host    all             all              0.0.0.0/0              md5
    host    all             all              ::/0                   md5
    
    # Alternatively, only allow access for specific users
    host    all             report,tsarch    0.0.0.0/0              md5
    host    all             report,tsarch    ::/0                   md5


.. in the firewall:

    sudo firewall-cmd --permanent --direct --add-rule ipv4 filter IN_public_allow 0 -m tcp -p tcp --dport 5432 -j ACCEPT
    sudo firewall-cmd --direct --get-rules ipv4 filter IN_public_allow

Start/Stop/Status/Restart Database
----------------------------------

    sudo su postgres
    export PGDATA=/var/lib/pgsql/14/data
    /usr/pgsql-14/bin/pg_ctl  restart
    /usr/pgsql-14/bin/pg_ctl  status
    /usr/pgsql-14/bin/pg_ctl  stop

Automated Startup
-----------------

Check `/usr/lib/systemd/system/postgresql-14.service`

    sudo systemctl status postgresql-14
    sudo systemctl enable postgresql-14
    sudo systemctl start postgresql-14


See https://www.postgresql.org/docs/14/server-start.html for more
