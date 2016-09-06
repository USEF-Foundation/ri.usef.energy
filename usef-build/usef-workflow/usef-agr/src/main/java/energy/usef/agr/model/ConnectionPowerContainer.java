/*
 * Copyright 2015-2016 USEF Foundation
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

package energy.usef.agr.model;

import energy.usef.core.model.Connection;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.joda.time.LocalDate;

/**
 * Extension of {@link PowerContainer} to link it with a {@link Connection}
 */
@Entity
@DiscriminatorValue("CONNECTION")
@Table(name = "POWER_CONTAINER_CONNECTION")
public class ConnectionPowerContainer extends PowerContainer {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONNECTION_ENTITY_ADDRESS")
    private Connection connection;

    /**
     * Default constructor.
     */
    public ConnectionPowerContainer() {
        super();
    }

    /**
     * Initial Constructor.
     *
     * @param connection {@link Connection} the udi this power information is related to.
     * @param period
     * @param timeIndex
     */
    public ConnectionPowerContainer(Connection connection, LocalDate period, Integer timeIndex) {
        super(period, timeIndex);
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
