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

package energy.usef.cro.service.business;

import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.cro.config.ConfigCro;
import energy.usef.cro.config.ConfigCroParam;
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.model.MeterDataCompany;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.CongestionPointRepository;
import energy.usef.cro.repository.ConnectionRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service class implements the business logic related to the CRO part of the common reference update. Simultaneous inserts are
 * captured by optimistic lock exceptions. When an OptimisticLockException occurs, the system throws an exception and the message
 * from the queue will be processed again (up to 5 times).
 */
@Singleton
public class CommonReferenceUpdateBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceUpdateBusinessService.class);

    @Inject
    private DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    private CongestionPointRepository congestionPointRepository;

    @Inject
    private ConnectionRepository connectionRepository;

    @Inject
    private AggregatorRepository aggregatorRepository;

    @Inject
    private BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Inject
    private MeterDataCompanyRepository meterDataCompanyRepository;

    @Inject
    private ConfigCro configCro;

    /**
     * Executes CommonReferenceUpdate request from the DSO. A CommonReferenceUpdate from the DSO only contains the entity- type
     * CongestionPoint.
     *
     * @param message CommonReferenceUpdate message
     * @param errors error list, to be filled during the update
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateCongestionPoints(CommonReferenceUpdate message, List<String> errors) {
        LOGGER.debug("CommonReferenceUpdate - update congestion points");
        // Validation
        if (!validateByModeDso(message, errors)) {
            LOGGER.debug("Errors validating Distribution System Operator " + errors);
            return;
        }

        if (!validateDistributionSystemOperatorDomain(message, errors)) {
            LOGGER.debug("Errors validating Distribution System Operator Domain " + errors);
            return;
        }

        Set<String> connectionXmlElementSet = createEntityAddressSetFromXml(message);
        if (!validateConnections(message, connectionXmlElementSet, errors)) {
            LOGGER.debug("Errors validating Connections " + errors);
            return;
        }

        final List<energy.usef.core.data.xml.bean.message.Connection> connectionXmlElements = message.getConnection();
        if (connectionXmlElements == null || connectionXmlElements.isEmpty()) {
            // Deleting the congestion point
            deleteCongestionPoint(message.getEntityAddress());
            return;
        }

        // Updating congestion point
        CongestionPoint congestionPoint = updateCongestionPoint(message);

        // Updating connections
        updateConnections(congestionPoint, message, connectionXmlElementSet);
    }

    /**
     * Executes CommonReferenceUpdate request from the AGR. A CommonReferenceUpdate from the AGR only contains the entity- type
     * Aggregator.
     * 
     * @param message CommonReferenceUpdate message
     * @param errors error list, to be filled during the update
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateAggregatorConnections(CommonReferenceUpdate message, List<String> errors) {
        LOGGER.debug("CommonReferenceUpdate - update connections");
        // Validation
        Aggregator aggregator = validateByModeAggregator(message, errors);
        if (aggregator == null) {
            LOGGER.debug("Errors validating Aggregator " + errors);
            return;
        }

        if (!validateDistributionSystemOperatorDomain(message, errors)) {
            LOGGER.debug("Errors validating Distribution System Operator " + errors);
            return;
        }

        updateConnectionsForAggregator(message, aggregator);
    }

    /**
     * Executes CommonReferenceUpdate request from the BRP.
     * 
     * @param message CommonReferenceUpdate message
     * @param errors error list, to be filled during the update
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateBalanceResponsiblePartyConnections(CommonReferenceUpdate message, List<String> errors) {
        LOGGER.debug("CommonReferenceUpdate - update connections");

        // Validation
        BalanceResponsibleParty balanceResponsibleParty = validateByModeBalanceResponsibleParty(message, errors);
        if (balanceResponsibleParty == null) {
            LOGGER.debug("Errors validating BalanceResponsibleParty " + errors);
            return;
        }

        // Update
        updateConnectionsForBalanceResponsibleParty(message, balanceResponsibleParty);
    }

    /**
     * Gets DistributionSystemOperator entity by its domain.
     *
     * @param domain distribution system operator domain
     *
     * @return DistributionSystemOperator entity
     */
    public DistributionSystemOperator findDistributionSystemOperatorByDomain(String domain) {
        return distributionSystemOperatorRepository.findByDomain(domain);
    }

    /**
     * Gets DistributionSystemOperator entity by its domain. Or Creates it if it doesn't exist
     *
     * @param domain distribution system operator domain
     *
     * @return DistributionSystemOperator entity
     */
    public DistributionSystemOperator findOrCreateDistributionSystemOperatorByDomain(String domain) {
        return distributionSystemOperatorRepository.findOrCreateByDomain(domain);
    }

    /**
     * Find the BRP with this domain.
     * 
     * @param domain
     * @return BalanceResponsibleParty
     */
    public BalanceResponsibleParty findBRP(String domain) {
        return balanceResponsiblePartyRepository.findByDomain(domain);
    }

    /**
     * Find the MCM with this domain.
     * 
     * @param domain
     * @return
     */
    public MeterDataCompany findMDC(String domain) {
        return meterDataCompanyRepository.findByDomain(domain);
    }

    /**
     * Gets Aggregator entity by its domain.
     *
     * @param domain aggregator domain
     *
     * @return Aggregator entity
     */
    public Aggregator getAggregatorByDomain(String domain) {
        return aggregatorRepository.findByDomain(domain);
    }

    /**
     * Find the CongestionPoints and the linked Aggregators for a certain DSO, the specific entityAddress is optional.
     *
     * @param dsoDomain - The domain of the sending party.
     * @param entityAddress - Optional congestionPoint.entityAddress
     * @return
     */
    public Map<CongestionPoint, Map<Aggregator, Long>> findCongestionPointsWithAggregatorsByDSO(
            String dsoDomain, String entityAddress) {
        return congestionPointRepository.findAggregatorCountForCongestionPointsByDSO(dsoDomain, entityAddress);
    }

    /**
     * Gets Connections and CongestionPoints related to an AGR, either all or for a specific entityAdress.
     *
     * @param agrDomain - The domain of the requesting party
     * @param entityAddress - (optional) The congestionPoint entity address
     *
     * @return Map - The Map with connections and corresponding congestionPoints.
     */
    public Map<CongestionPoint, Set<Connection>> findConnectionsForCongestionPointsByAGR(String agrDomain,
            String entityAddress) {
        return congestionPointRepository.findConnectionsForCongestionPointsByAGR(agrDomain, entityAddress);
    }

    /**
     * Gets a CongestionPoint by an entity address.
     *
     * @param entityAddress entity address
     * @return CongestionPoint
     */
    public CongestionPoint getCongestionPointByEntityAddress(String entityAddress) {
        return congestionPointRepository.getCongestionPointByEntityAddress(entityAddress);
    }

    private void deleteCongestionPoint(String entityAddress) {
        LOGGER.debug("Delete congestion point with entity addres {}.", entityAddress);
        final CongestionPoint congestionPoint = congestionPointRepository.getCongestionPointByEntityAddress(entityAddress);
        if (congestionPoint == null) {
            LOGGER.warn("Tried to delete the congestion point with the entity address {}, but found no congestion point in the DB");
            return;
        }

        congestionPoint.getConnections().forEach(this::deleteConnection);
        congestionPointRepository.delete(congestionPoint);

        LOGGER.info("Deleted congestion point with the entity adress: {}", entityAddress);
    }

    private CongestionPoint updateCongestionPoint(CommonReferenceUpdate message) {
        CongestionPoint congestionPoint = congestionPointRepository.getCongestionPointByEntityAddress(message.getEntityAddress());

        if (congestionPoint == null) {
            // Creating congestion point
            congestionPoint = createCongestionPoint(message);
        }

        return congestionPoint;
    }

    private boolean validateByModeDso(CommonReferenceUpdate message, List<String> errors) {
        if (CommonReferenceMode.CLOSED.value().equals(configCro.getProperty(ConfigCroParam.COMMON_REFERENCE_MODE)) &&
                distributionSystemOperatorRepository.findByDomain(message.getMessageMetadata().getSenderDomain()) == null) {
            // Closed mode, DSO not registered, reject
            errors.add("You are not allowed to register DSO");
            return false;
        }
        // Open mode
        return true;
    }

    private Aggregator validateByModeAggregator(CommonReferenceUpdate message, List<String> errors) {
        String aggregatorDomain = message.getMessageMetadata().getSenderDomain();
        Aggregator aggregator = aggregatorRepository.findByDomain(aggregatorDomain);
        if (CommonReferenceMode.CLOSED.value().equals(configCro.getProperty(ConfigCroParam.COMMON_REFERENCE_MODE))) {
            // Closed mode
            if (aggregator == null) {
                // AGR not registered, reject
                errors.add("You are not allowed to register the aggregator " + message.getMessageMetadata().getSenderDomain());
                return null;
            }
        } else {
            // Open mode, aggregator will be created when it does not exist.
            if (aggregator == null) {
                LOGGER.debug("Aggregator {} does not exist and will be created.", aggregatorDomain);
                aggregator = new Aggregator(aggregatorDomain);
                aggregatorRepository.persist(aggregator);
            }
        }
        return aggregator;
    }

    private BalanceResponsibleParty validateByModeBalanceResponsibleParty(CommonReferenceUpdate message, List<String> errors) {
        String balanceResponsiblePartyDomain = message.getMessageMetadata().getSenderDomain();
        BalanceResponsibleParty balanceResponsibleParty = balanceResponsiblePartyRepository
                .findByDomain(balanceResponsiblePartyDomain);
        if (CommonReferenceMode.CLOSED.value().equals(configCro.getProperty(ConfigCroParam.COMMON_REFERENCE_MODE))) {
            // Closed mode
            if (balanceResponsibleParty == null) {
                // BRP not registered, reject
                errors.add("You are not allowed to register the BRP " + message.getMessageMetadata().getSenderDomain());
                return null;
            }
        } else {
            // Open mode, BRP will be created when it does not exist.
            if (balanceResponsibleParty == null) {
                LOGGER.debug("Balance Responsible Party {} does not exist and will be created.", balanceResponsiblePartyDomain);
                balanceResponsibleParty = new BalanceResponsibleParty(balanceResponsiblePartyDomain);
                balanceResponsiblePartyRepository.persist(balanceResponsibleParty);
            }
        }
        return balanceResponsibleParty;
    }

    private boolean validateDistributionSystemOperatorDomain(CommonReferenceUpdate message, List<String> errors) {
        CongestionPoint congestionPoint = congestionPointRepository.getCongestionPointByEntityAddress(message.getEntityAddress());

        if (congestionPoint != null) {
            String senderDomain = message.getMessageMetadata().getSenderDomain();
            // DSO domain check
            if (!senderDomain.equals(congestionPoint.getDistributionSystemOperator().getDomain())) {
                // Wrong DSO, reject
                errors.add("This congestion point has wrong DSO domain: '" + senderDomain + "'");
            }
        }

        return errors.isEmpty();
    }

    private boolean validateConnections(CommonReferenceUpdate message, Set<String> connectionXmlElementSet, List<String> errors) {
        final String congestionPointEntityAddress = message.getEntityAddress();
        final String senderDomain = message.getMessageMetadata().getSenderDomain();

        for (String connectionEntityAddress : connectionXmlElementSet) {
            Connection connection = connectionRepository.findConnectionByEntityAddress(connectionEntityAddress);
            if (connection != null && connection.getCongestionPoint() != null &&
                    !congestionPointEntityAddress.equals(connection.getCongestionPoint().getEntityAddress()) &&
                    !senderDomain.equals(connection.getCongestionPoint().getDistributionSystemOperator().getDomain())) {
                // The congestion point of this connection is related to another DSO, reject
                LOGGER.warn("The connection {} is related to another DSO.", connectionEntityAddress);
                errors.add("The connection: '" + connectionEntityAddress + "' is related to another DSO");
            }
        }

        return errors.isEmpty();
    }

    private CongestionPoint createCongestionPoint(CommonReferenceUpdate message) {
        String senderDomain = message.getMessageMetadata().getSenderDomain();

        DistributionSystemOperator distributionSystemOperator = distributionSystemOperatorRepository
                .findOrCreateByDomain(senderDomain);

        // Creating congestion point
        LOGGER.debug("Create congestion point for senderdomain {}.", senderDomain);
        CongestionPoint congestionPoint = new CongestionPoint();
        congestionPoint.setDistributionSystemOperator(distributionSystemOperator);
        congestionPoint.setEntityAddress(message.getEntityAddress());
        congestionPointRepository.persist(congestionPoint);

        LOGGER.info("Created congestion point with entity address: {}",
                message.getEntityAddress());

        return congestionPoint;
    }

    private void updateConnections(CongestionPoint congestionPoint,
            CommonReferenceUpdate message, Set<String> connectionXmlElementSet) {
        LOGGER.debug("Update connection for congestionPoint {}", congestionPoint);
        Map<String, Connection> connectionToDeleteMap = getCongestionPointConnectionMap(congestionPoint);

        for (String entityAddress : connectionXmlElementSet) {
            if (connectionToDeleteMap.get(entityAddress) != null) {
                // Connection exists in the DB and related to the given congestion point
                connectionToDeleteMap.remove(entityAddress);
            } else {
                // Looking for an unrelated connection or a connection related to another congestion point in the DB
                Connection connection = connectionRepository.findConnectionByEntityAddress(entityAddress);
                if (connection != null) {
                    // Set the connection in the DB
                    LOGGER.debug("Setting CongestionPoint {} for connection {}", congestionPoint, entityAddress);
                    connection.setCongestionPoint(congestionPoint);
                } else {
                    // Creating a new connection
                    LOGGER.debug("Creating new Connection with entity address {} and CongestionPoint {}", entityAddress,
                            congestionPoint);
                    Connection newConnection = new Connection(entityAddress);
                    newConnection.setCongestionPoint(congestionPoint);
                    connectionRepository.persist(newConnection);
                }
            }
        }

        // Deleting redundant connections
        connectionToDeleteMap.values().forEach(this::deleteConnection);

        LOGGER.info("Updated connections for congestion point with entity address: {}", message.getEntityAddress());
    }

    private void updateConnectionsForAggregator(CommonReferenceUpdate message, Aggregator aggregator) {
        LOGGER.debug("Updating connections for {}", aggregator);
        if (message.getConnection().isEmpty()) {
            LOGGER.warn("No connections to update for {}", aggregator);
        }
        List<Connection> connections = connectionRepository.findAll();
        for (final energy.usef.core.data.xml.bean.message.Connection xmlConnection : message.getConnection()) {
            Optional<Connection> optionalConnection = connections
                    .stream()
                    .filter(a -> a.getEntityAddress().equalsIgnoreCase(xmlConnection.getEntityAddress()))
                    .findFirst();

            if (!optionalConnection.isPresent()) {
                if (xmlConnection.isIsCustomer()) {
                    LOGGER.debug("Creating connection with entity address {} for {}", xmlConnection.getEntityAddress(), aggregator);
                    // A new connection will be created.
                    Connection connection = new Connection();
                    connection.setEntityAddress(xmlConnection.getEntityAddress());
                    connection.setAggregator(aggregator);
                    connectionRepository.persist(connection);
                // Assure that the newly created connection will be found in case of duplicates
                connections.add(connection);
                }
            } else {
                Connection connection = optionalConnection.get();
                updateConnectionWithAggregator(aggregator, xmlConnection, connection);

            }
            LOGGER.debug("Updated connections for {} and connection: {}", aggregator, xmlConnection.getEntityAddress());
        }
    }

    private void updateConnectionWithAggregator(Aggregator aggregator, energy.usef.core.data.xml.bean.message.Connection xmlConnection,
            Connection connection) {
        // The existing connection will be updated.
        // If there's no registered aggregator or the connection's isCustomer flag is true,
        // the aggregator is registered.
        // If the connection's isCustomer flag is false and the connection is updated for the registered aggregator,
        // the aggregator is unregistered.
        // Else, nothing happens
        if (connection.getAggregator() == null || xmlConnection.isIsCustomer()) {
            LOGGER.debug("Updating connection with entity address {} for {}", xmlConnection.getEntityAddress(), aggregator);
            connection.setAggregator(aggregator);
        } else if (!xmlConnection.isIsCustomer() && connection.getAggregator().equals(aggregator)) {
            LOGGER.debug("Resetting Aggregator for Connection with entity address {}", xmlConnection.getEntityAddress());
            connection.setAggregator(null);
            if (connection.getBalanceResponsibleParty() == null) {
                connectionRepository.delete(connection);
            }
        } else {
            LOGGER.debug("Connection with entity address {} will not be overridden because isCustomer is set to false.",
                    connection.getEntityAddress());
        }
    }

    private void updateConnectionsForBalanceResponsibleParty(CommonReferenceUpdate message,
            BalanceResponsibleParty balanceResponsibleParty) {
        LOGGER.debug("Updating connections for {}", balanceResponsibleParty);

        if (message.getConnection().isEmpty()) {
            LOGGER.warn("No connections to update for {}", balanceResponsibleParty);
        }

        List<Connection> connections = connectionRepository.findAll();

        for (final energy.usef.core.data.xml.bean.message.Connection xmlConnection : message.getConnection()) {

            Optional<Connection> optionalConnection = connections
                    .stream()
                    .filter(a -> a.getEntityAddress().equalsIgnoreCase(xmlConnection.getEntityAddress()))
                    .findFirst();

            if (!optionalConnection.isPresent()) {
                // Connection does not exist and will be created
                LOGGER.debug("Creating connection with entity address {} for {}", xmlConnection.getEntityAddress(),
                        balanceResponsibleParty);
                // A new connection will be created.
                Connection connection = new Connection();
                connection.setEntityAddress(xmlConnection.getEntityAddress());
                connection.setBalanceResponsibleParty(balanceResponsibleParty);
                connectionRepository.persist(connection);

                // Assure that the newly created connection will be found in case of duplicates
                connections.add(connection);
            } else {
                LOGGER.debug("Updating connection with entity address {} for {}", xmlConnection.getEntityAddress(),
                        balanceResponsibleParty);
                // Connection exists.
                Connection connection = optionalConnection.get();
                connection.setBalanceResponsibleParty(balanceResponsibleParty);
            }
            LOGGER.debug("Updated connections for {} and connection: {}", balanceResponsibleParty, xmlConnection.getEntityAddress());
        }
    }

    private void deleteConnection(Connection connection) {
        if (connection.getAggregator() != null) {
            // Connection has an aggregator, it can not be deleted
            LOGGER.debug("Resetting CongestionPoint for connection " + connection);
            connection.setCongestionPoint(null);
        } else {
            // No aggregator, connection can be deleted
            LOGGER.debug("Deleting Connection " + connection);
            connectionRepository.delete(connection);
        }
    }

    private Set<String> createEntityAddressSetFromXml(CommonReferenceUpdate message) {
        Set<String> result = new HashSet<>();
        List<energy.usef.core.data.xml.bean.message.Connection> connectionXmlElementList = message.getConnection();
        if (connectionXmlElementList != null && !connectionXmlElementList.isEmpty()) {
            result.addAll(connectionXmlElementList.stream()
                    .map(energy.usef.core.data.xml.bean.message.Connection::getEntityAddress)
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private Map<String, Connection> getCongestionPointConnectionMap(CongestionPoint congestionPoint) {
        Map<String, Connection> result = new HashMap<>();
        for (Connection connection : congestionPoint.getConnections()) {
            result.put(connection.getEntityAddress(), connection);
        }
        return result;
    }

}
