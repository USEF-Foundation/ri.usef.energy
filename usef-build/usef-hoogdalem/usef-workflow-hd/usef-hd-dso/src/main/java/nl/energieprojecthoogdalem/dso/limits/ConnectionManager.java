/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.dso.limits;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import info.usef.core.config.AbstractConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * manages ms sqlserver connections for a given property file
*/
public class ConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private boolean isConnected = false;
    private Connection connection;

    /**
     * attempts to connect to a com.microsoft.sqlserver.jdbc.SQLServerDataSource datasource,
     *
     * the following propertie(s) are required to be set:
     * "host" dns name / ip adress pointing to the server (defaults to 127.0.0.1)
     * "port" port number of the server valid range 1-65535 (defaults to 1433)
     * "username" username used in authentication (defaults to "root")
     *
     * "password" password used in authentication (defaults to "root")
     *
     * the following propertie(s) are optional to be set:
     * "encrypt" use an encrypted connection valid values: true or false (defaults to false)
     * "certificate_host" the hostname of the server certificate (defaults to *.database.windows.net)
     * "trust_server_certificate" whenever to trust the server certificate or validate it valid values: true or false (defaults to false)
     *
     * "timeout" the amount of time to wait before a connection attempt times out in seconds (defaults to "15")
     * @return boolean if true a connection can be established, if false then a connection can't be made
     * @see com.microsoft.sqlserver.jdbc.SQLServerDataSource
     * */
    public boolean connect()
    {
        if(!isConnected)
        {
            Properties prop = loadDatabaseConfig();
            SQLServerDataSource dataSource = new SQLServerDataSource();
            dataSource.setServerName(prop.getProperty("host","127.0.0.1"));
            dataSource.setPortNumber(Integer.parseInt(prop.getProperty("port", "1433")));
            dataSource.setUser(prop.getProperty ("username", "root"));
            dataSource.setPassword(prop.getProperty("password", "root"));
            dataSource.setDatabaseName(prop.getProperty("database", "master"));
            dataSource.setLoginTimeout(Integer.parseInt(prop.getProperty("timeout","3") ));

            //SSL properties, no need to add server certificate to java store
            dataSource.setHostNameInCertificate(prop.getProperty("certificate_host", "*.database.windows.net"));
            dataSource.setTrustServerCertificate(Boolean.parseBoolean(prop.getProperty("trust_server_certificate", "false")));
            dataSource.setEncrypt(Boolean.parseBoolean(prop.getProperty("encrypt", "false")));

            try
            {
                connection = dataSource.getConnection();
                isConnected = true;
            }
            catch (SQLServerException exception)
            {
                LOGGER.warn("Unable to connect with database Reason: {}", exception.getMessage());
                isConnected = false;
            }
        }

        return isConnected;
    }

    /**
     * when a connection is established, a java.sql.Connection will be returned
     * otherwise this method returns null
     *
     * @return Connection
     * @see Connection
     * */
    public Connection getConnection()
    {
        return connection;
    }

    /**
     * this will close the connection if ConnectionManager.connect() has been successfully called
     * */
    public void disconnect()
    {
        if(isConnected)
        {
            try
            {   connection.close(); }
            catch (SQLException exception)
            {   LOGGER.warn("Unable to close connection reason: ", exception);  }

            isConnected = false;
        }
    }


    /**
     * loads the database.properties file from the configuration folder of the usef party
     * */
    private static Properties loadDatabaseConfig()
    {
        Properties prop = new Properties();
        try
        {
            prop.load(new FileReader(AbstractConfig.getConfigurationFolder() + "database.properties"));
        }
        catch(IOException exception)
        {
            LOGGER.warn("Unable to read database properties reason: {}", exception.getMessage());
        }
        return prop;
    }

}
