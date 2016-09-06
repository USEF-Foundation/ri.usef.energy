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

package energy.usef.mdc.service.business;

import static java.util.stream.Collectors.toList;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.mdc.model.Aggregator;
import energy.usef.mdc.model.AggregatorConnection;
import energy.usef.mdc.model.BalanceResponsibleParty;
import energy.usef.mdc.model.CommonReferenceOperator;
import energy.usef.mdc.model.CommonReferenceQueryState;
import energy.usef.mdc.model.Connection;
import energy.usef.mdc.model.DistributionSystemOperator;
import energy.usef.mdc.repository.AggregatorConnectionRepository;
import energy.usef.mdc.repository.AggregatorRepository;
import energy.usef.mdc.repository.BalanceResponsiblePartyRepository;
import energy.usef.mdc.repository.CommonReferenceOperatorRepository;
import energy.usef.mdc.repository.CommonReferenceQueryStateRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.MdcConnectionRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core business service for the MDC role.
 */
@Stateless
public class MdcCoreBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdcCoreBusinessService.class);

    @Inject
    private MdcConnectionRepository mdcConnectionRepository;

    @Inject
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    private AggregatorConnectionRepository aggregatorConnectionRepository;

    @Inject
    private AggregatorRepository aggregatorRepository;

    @Inject
    private DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    private BalanceResponsiblePartyRepository balanceResponsibleParty;

    @Inject
    private CommonReferenceQueryStateRepository commonReferenceQueryStateRepository;

    /**
     * Find the entity addresses of all the registered {@link Connection} entities.
     *
     * @return a {@link java.util.List} of {@link String}.
     */
    public List<String> findAllConnectionEntityAddresses() {
        return mdcConnectionRepository.findAllConnections().stream().map(Connection::getEntityAddress).collect(toList());
    }

    /**
     * Finds the domain names of all the registered Common Reference Operators.
     *
     * @return a {@link java.util.List} of {@link String}.
     */
    public List<String> findAllCommonReferenceOperatorDomains() {
        return commonReferenceOperatorRepository.findAll().stream().map(CommonReferenceOperator::getDomain).collect(toList());
    }

    /**
     * Store new or modify existing connections resulting from the common reference query of the specified common reference
     * operator.
     *
     * @param xmlConnections {@link java.util.List} of {@link energy.usef.core.data.xml.bean.message.Connection}.
     * @param croDomain {@link String} CRO domain name.
     */
    public void storeConnectionsForCommonReferenceOperator(List<energy.usef.core.data.xml.bean.message.Connection> xmlConnections,
            String croDomain) {
        LocalDate modificationDate = DateTimeUtil.getCurrentDate();
        List<AggregatorConnection> currentStates = aggregatorConnectionRepository
                .findActiveAggregatorConnectionsForCommonReferenceOperator(croDomain, modificationDate);

        // for each connection in the message for which no state is there, create one
        xmlConnections
                .stream()
                .collect(Collectors.toMap(xmlConnection -> xmlConnection.getEntityAddress(), Function.identity()))
                .entrySet()
                .stream()
                .filter(entry -> currentStates.stream()
                        .map(connection -> connection.getConnection().getEntityAddress())
                        .noneMatch(address -> entry.getKey().equals(address)))
                .forEach(
                        entry -> {
                            LOGGER.debug("New connection [{}] in the CommonReferenceQueryResponse with aggregator [{}].",
                                    entry.getKey(),
                                    entry.getValue().getAGRDomain());
                            createAggregatorOnConnectionState(entry.getKey(), entry.getValue().getAGRDomain(), croDomain,
                                    modificationDate);
                        });

        // for each connection in the current states which is not in the message close it
        currentStates.stream()
                .collect(Collectors.toMap(state -> state.getConnection().getEntityAddress(), Function.identity()))
                .entrySet()
                .stream()
                .filter(entry -> xmlConnections.stream()
                        .map(xmlConnection -> xmlConnection.getEntityAddress())
                        .noneMatch(xmlAddress -> xmlAddress.equals(entry.getKey())))
                .forEach(entry -> {
                    LOGGER.debug(
                            "The connection [{}] (with aggregator [{}]) is not in the CommonReferenceQueryResponse anymore. State"
                                    + " will be closed.", entry.getValue().getConnection().getEntityAddress(),
                            entry.getValue().getAggregator().getDomain());
                    entry.getValue().setValidUntil(modificationDate);
                });

        // for each connection in the current states for which the aggregator changed, close it and create a new one
        currentStates
                .stream()
                .collect(Collectors.toMap(state -> state.getConnection().getEntityAddress(), Function.identity()))
                .entrySet()
                .stream()
                .forEach(
                        entry -> xmlConnections
                                .stream()
                                .filter(xmlConnection -> xmlConnection.getEntityAddress().equals(entry.getKey()))
                                .filter(xmlConnection -> !xmlConnection.getAGRDomain().equals(
                                        entry.getValue().getAggregator().getDomain()))
                                .findAny()
                                .ifPresent(
                                        xmlConnection -> {
                                            LOGGER.debug(
                                                    "Aggregator has changed from [{}] to [{}] for connection [{}]. Previous state will be closed "
                                                            + "and a new one will be created.",
                                                    entry.getValue().getAggregator().getDomain(), xmlConnection.getAGRDomain(),
                                                    xmlConnection.getEntityAddress());
                                            entry.getValue().setValidUntil(modificationDate);
                                            createAggregatorOnConnectionState(xmlConnection.getEntityAddress(),
                                                    xmlConnection.getAGRDomain(),
                                                    croDomain, modificationDate);
                                        }));
    }

    /**
     * Save the state a of a received {@link CommonReferenceQueryState}.
     *
     * @param state {@link CommonReferenceQueryState}.
     */
    public void storeCommonReferenceQueryState(CommonReferenceQueryState state) {
        commonReferenceQueryStateRepository.persist(state);
    }

    /**
     * Creates a new {@link AggregatorConnection} with the given parameters.
     *
     * @param connectionEntityAddress {@link String} entity address of the connection.
     * @param aggregatorDomain {@link String} domain name of the aggregator.
     * @param croDomain {@link String} CRO domain name.
     * @param validFrom {@link org.joda.time.LocalDateTime} valid from date.
     */
    private void createAggregatorOnConnectionState(String connectionEntityAddress, String aggregatorDomain, String croDomain,
            LocalDate validFrom) {
        Aggregator aggregator = aggregatorRepository.findOrCreate(aggregatorDomain);
        CommonReferenceOperator commonReferenceOperator = commonReferenceOperatorRepository.find(croDomain);
        Connection connection = mdcConnectionRepository.find(connectionEntityAddress);
        AggregatorConnection state = new AggregatorConnection();
        state.setAggregator(aggregator);
        state.setCommonReferenceOperator(commonReferenceOperator);
        state.setConnection(connection);
        state.setValidFrom(validFrom);
        aggregatorConnectionRepository.persist(state);
    }

    /**
     * This method finds the current ConnectionState.
     *
     * @param date The date to look for data.
     * @param connectionEntityAddress The list of connection for which data is requested.
     * @return
     */
    public Map<String, String> findConnectionState(LocalDate date, List<String> connectionEntityAddress) {
        return aggregatorConnectionRepository.findAggregatorForEachConnection(date, connectionEntityAddress);
    }

    /**
     * Find the DistributionSystemOperator.
     *
     * @param dsoDomain
     * @return
     */
    public DistributionSystemOperator findDistributionSystemOperator(String dsoDomain) {
        return distributionSystemOperatorRepository.find(dsoDomain);
    }

    /**
     * Find the DistributionSystemOperator.
     *
     * @param brpDomain
     * @return
     */
    public BalanceResponsibleParty findBalanceResponsibleParty(String brpDomain) {
        return balanceResponsibleParty.find(brpDomain);
    }

}
