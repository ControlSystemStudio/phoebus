/**
 * Copyright (C) 2024 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.service.saveandrestore.application;

import org.phoebus.pv.PVPool;
import org.phoebus.service.saveandrestore.migration.MigrateRdbToElastic;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Save-and-restore service main class.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore"})
@EnableScheduling
@EnableAutoConfiguration
public class Application {

    private static ConfigurableApplicationContext context;

    private static void help() {
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                                    - This text");
        System.out.println("-migrate http://legacy.service.url       - Migrate data from legacy service at http://legacy.service.url");
        System.out.println();
    }

    /**
     * Main class
     * @param args Program arguments. Specifying -help will print options.
     */
    public static void main(String[] args) {

        // Hack: PVPool reads properties from dependency jar. Override of default
        // protocol done here by direct access to PVPool.default_type.
        String defaultProtocol = System.getProperty("defaultProtocol");
        if(defaultProtocol != null && !defaultProtocol.isEmpty()){
            PVPool.default_type = defaultProtocol;
        }
        System.out.println("Using default EPICS protocol: " + PVPool.default_type);
        // load the default properties
        final Properties properties = PropertiesHelper.getProperties();

        final List<String> arguments = new ArrayList<>(List.of(args));
        final Iterator<String> iterator = arguments.iterator();
        boolean runMigration = false;
        try {
            while (iterator.hasNext()) {
                final String command = iterator.next();
                if (command.equals("-h") || command.equals("-help")) {
                    help();
                    System.exit(0);
                    return;
                } else if (command.equals("-migrate")) {
                    if (!iterator.hasNext())
                        throw new Exception("Missing -migrate legacy.service.url");
                    runMigration = true;
                    System.setProperty("migrationContext", "true");
                    iterator.remove();
                    properties.put("legacy.service.url", iterator.next());
                    iterator.remove();
                } else if (command.equals("-dryRun")) {
                    iterator.remove();
                    properties.put("dryRun", true);
                }

            }
        } catch (Exception ex) {
            System.out.println("\n>>>> Print StackTrace ....");
            ex.printStackTrace();
            System.out.println("\n>>>> Please check available arguments of save & restore as follows:");
            help();
            System.exit(-1);
            return;
        }


        context = SpringApplication.run(Application.class, args);

        if (runMigration) {
            try {
                String dryRun = properties.getProperty("dryRun", "false");
                MigrateRdbToElastic migrateRdbToElastic =
                        new MigrateRdbToElastic(context.getBean(ElasticsearchDAO.class),
                                properties.getProperty("legacy.service.url"),
                                Boolean.parseBoolean(dryRun));
                migrateRdbToElastic.runMigration();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }
    }

    private static void close() {
        System.out.println("\n Shutdown");
        if (context != null) {
            context.close();
        }
        System.exit(0);
    }
}
