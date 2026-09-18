/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.security.ldap.server.EmbeddedLdapServerContainer;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * This is a modification of {@link org.springframework.security.ldap.server.UnboundIdContainer} for the purpose
 * of disabling ldif file validation against default schema.
 */
public class SaveAndRestoreUnboundIdContainer implements EmbeddedLdapServerContainer, InitializingBean, DisposableBean, Lifecycle, ApplicationContextAware {

    private InMemoryDirectoryServer directoryServer;
    private final String defaultPartitionSuffix;
    private int port = 53389;
    private boolean isEphemeral;
    private ConfigurableApplicationContext context;
    private boolean running;
    private final String ldif;

    public SaveAndRestoreUnboundIdContainer(String defaultPartitionSuffix, String ldif) {
        this.defaultPartitionSuffix = defaultPartitionSuffix;
        this.ldif = ldif;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
        this.isEphemeral = port == 0;
    }

    public void destroy() {
        this.stop();
    }

    public void afterPropertiesSet() {
        this.start();
    }

    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.context = (ConfigurableApplicationContext)applicationContext;
    }

    public void start() {
        if (!this.isRunning()) {
            try {
                InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(new String[]{this.defaultPartitionSuffix});
                config.addAdditionalBindCredentials("uid=admin,ou=system", "secret");
                config.setListenerConfigs(new InMemoryListenerConfig[]{InMemoryListenerConfig.createLDAPConfig("LDAP", this.port)});
                config.setEnforceSingleStructuralObjectClass(false);
                config.setEnforceAttributeSyntaxCompliance(true);
                // Setting null schema disables ldif validation
                config.setSchema(null);

                DN dn = new DN(this.defaultPartitionSuffix);
                Entry entry = new Entry(dn);
                entry.addAttribute("objectClass", new String[]{"top", "domain", "extensibleObject"});
                entry.addAttribute("dc", dn.getRDN().getAttributeValues()[0]);
                InMemoryDirectoryServer directoryServer = new InMemoryDirectoryServer(config);
                directoryServer.add(entry);
                this.importLdif(directoryServer);
                directoryServer.startListening();
                this.port = directoryServer.getListenPort();
                this.directoryServer = directoryServer;
                this.running = true;
            } catch (LDAPException ex) {
                throw new RuntimeException("Server startup failed", ex);
            }
        }
    }

    private void importLdif(InMemoryDirectoryServer directoryServer) {
        if (StringUtils.hasText(this.ldif)) {
            try {
                Resource[] resources = this.context.getResources(this.ldif);
                if (resources.length > 0) {
                    if (!resources[0].exists()) {
                        throw new IllegalArgumentException("Unable to find LDIF resource " + this.ldif);
                    }

                    try (InputStream inputStream = resources[0].getInputStream()) {
                        directoryServer.importFromLDIF(false, new LDIFReader(inputStream));
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to load LDIF " + this.ldif, ex);
            }
        }

    }

    public void stop() {
        if (!this.isEphemeral || this.context == null || this.context.isClosed()) {
            this.directoryServer.shutDown(true);
            this.running = false;
        }
    }

    public boolean isRunning() {
        return this.running;
    }
}
