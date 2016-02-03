/*
 * Copyright 2015 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.schema;

import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Persistence;

import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.jpa.AvailableSettings;

/**
 *
 */
public class SchemaObjectCreator {

    public static void execute(String persistenceUnitName, String dialect, String destination) {

        try {
            // Create initial context
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
            InitialContext ic = new InitialContext();

            System.setProperty("jboss.server.log.dir", "target/log");

            ic.createSubcontext("java:");
            ic.createSubcontext("java:jboss");
            ic.createSubcontext("java:jboss/datasources");

            // Construct DataSource
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:TestDB;DB_CLOSE_DELAY=-1");
            ds.setUser("");
            ds.setPassword("");

            ic.bind("java:jboss/datasources/USEF_DS", ds);
        } catch (NamingException ex) {
            System.err.println("Caught exception" + ex);
        }

        final Properties persistenceProperties = new Properties();
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_SCRIPTS_ACTION, "drop-and-create");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_SCRIPTS_CREATE_TARGET, destination + "create-script.sql");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_SCRIPTS_DROP_TARGET, destination + "drop-script.sql");

        persistenceProperties.setProperty(org.hibernate.cfg.AvailableSettings.DIALECT, dialect);
        persistenceProperties.setProperty(org.hibernate.cfg.AvailableSettings.SHOW_SQL, "true");

        Persistence.generateSchema(persistenceUnitName, persistenceProperties);
    }

    public static void main(String[] args) throws IOException {
        execute(args[0], args[1], args[2]);
        System.exit(0);
    }
}
