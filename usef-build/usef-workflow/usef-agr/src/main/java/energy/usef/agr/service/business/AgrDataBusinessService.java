package energy.usef.agr.service.business;

import energy.usef.agr.dto.CommonReferenceOperatorDto;
import energy.usef.agr.dto.SynchronisationConnectionDto;
import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.model.SynchronisationConnectionStatus;
import energy.usef.agr.model.SynchronisationConnectionStatusType;
import energy.usef.agr.repository.CommonReferenceOperatorRepository;
import energy.usef.agr.repository.SynchronisationConnectionRepository;
import energy.usef.agr.repository.SynchronisationConnectionStatusRepository;
import energy.usef.core.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class in charge of  operations for the Aggregator.
 */
public class AgrDataBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDataBusinessService.class);
    public static final String ACTION = "action";
    public static final String DOMAIN = "domain";

    public static final String ENTITY_ADDRESS = "entityAddress";
    public static final String IS_CUSTOMER = "isCustomer";

    public static final String CREATE_OBJECT = "create";
    public static final String DELETE_OBJECT = "delete";

    public static final String DOMAIN_PATTERN = "([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}";
    public static final String ENTITY_ADDRESS_PATTERN = "(ea1\\.[0-9]{4}-[0-9]{2}\\..{1,244}:.{1,244}|ean\\.[0-9]{12,34})";

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Inject
    SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    public AgrDataBusinessService() {
    }

    /**
     * Process the json {@link String} containing common reference operator .
     *
     * @param jsonText a json {@link String} containing update informmation for {@Link CommonReferenceOperator}s.
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateCommonReferenceOperators(String jsonText) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            InputStream jsonStream = IOUtils.toInputStream(jsonText);

            JsonNode root = objectMapper.readTree(jsonStream);
            Iterator<JsonNode> croIterator = root.iterator();

            List<CommonReferenceOperator> existingCommonReferenceOperator = commonReferenceOperatorRepository.findAll();

            List<SynchronisationConnection> existingSynchronisationConnections = synchronisationConnectionRepository.findAll();

            while (croIterator.hasNext()) {
                JsonNode croNode = croIterator.next();

                if (isValidCommonReferenceOperatorNode(croNode)) {
                    String action = croNode.get(ACTION).asText();
                    String domain = croNode.get(DOMAIN).asText();

                    processCommonReferenceOperatorNode(existingCommonReferenceOperator, existingSynchronisationConnections, action,
                            domain);
                }
            }

        } catch (IOException e) {
            LOGGER.info("updateCommonReferenceOperators failed");
        } finally {
            LOGGER.info("updateCommonReferenceOperators finished");
        }
    }

    private void processCommonReferenceOperatorNode(List<CommonReferenceOperator> existingCommonReferenceOperator,
            List<SynchronisationConnection> existingSynchronisationConnections, String action, String domain) {
        if (!isValidDomain(domain)) {
            LOGGER.info("{} is not a valid internet domain, ignored", domain);
            return;
        }
        Optional<CommonReferenceOperator> object = existingCommonReferenceOperator.stream()
                .filter(e -> e.getDomain().equalsIgnoreCase(domain)).findFirst();

        switch (action) {
        case CREATE_OBJECT:
            if (object.isPresent()) {
                LOGGER.info("CommonReferenceOperator {} already exists, ignored", domain);
            } else {
                // Create the CommonReferenceOperator and the required SynchronisationConnectionStatus objects
                CommonReferenceOperator commonReferenceOperator = new CommonReferenceOperator();
                commonReferenceOperator.setDomain(domain);
                commonReferenceOperatorRepository.persist(commonReferenceOperator);

                existingSynchronisationConnections.forEach(e -> {
                    SynchronisationConnectionStatus synchronisationConnectionStatus = new SynchronisationConnectionStatus();
                    synchronisationConnectionStatus.setCommonReferenceOperator(commonReferenceOperator);
                    synchronisationConnectionStatus.setStatus(SynchronisationConnectionStatusType.MODIFIED);
                    synchronisationConnectionStatusRepository.persist(synchronisationConnectionStatus);
                });

                LOGGER.info("CommonReferenceOperator {} created", domain);
            }
            break;
        case DELETE_OBJECT:
            if (object.isPresent()) {
                // Delete existing SynchronisationConnectionStatus objects and the CommonReferenceOperator
                synchronisationConnectionStatusRepository.deleteAll(object.get());
                commonReferenceOperatorRepository.delete(object.get());
                LOGGER.info("CommonReferenceOperator {} deleted", domain);
            } else {
                LOGGER.info("CommonReferenceOperator {} doesn't exists, ignored", domain);
            }

            break;
        default:
            LOGGER.error("Action {} not allowed for CommonReferenceOperator {}", action, domain);
        }
    }

    /**
     * Process the json {@link String} containing synchronisation connection information.
     *
     * @param jsonText a json {@link String} containing update information for {@Link SynchronisationConnection}s.
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateSynchronisationConnections(String jsonText) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            InputStream jsonStream = IOUtils.toInputStream(jsonText);

            JsonNode root = objectMapper.readTree(jsonStream);
            Iterator<JsonNode> connectionIterator = root.iterator();

            List<SynchronisationConnection> existingSynchronisationConnection = synchronisationConnectionRepository.findAll();

            while (connectionIterator.hasNext()) {
                JsonNode connectionNode = connectionIterator.next();

                if (isValidConnectionNode(connectionNode)) {
                    String action = connectionNode.get(ACTION).asText();
                    String entityAddress = connectionNode.get(ENTITY_ADDRESS).asText();
                    Boolean isCustomer = connectionNode.get(IS_CUSTOMER).asBoolean(false);
                    processSynchronisationConnectionNode(existingSynchronisationConnection, action, entityAddress, isCustomer);
                }
            }

        } catch (IOException e) {
            LOGGER.info("updateSynchronisationConnections failed {}", e);
        } finally {
            LOGGER.info("updateSynchronisationConnections finished");
        }
    }

    private void processSynchronisationConnectionNode(List<SynchronisationConnection> existingSynchronisationConnection,
            String action, String entityAddress, Boolean isCustomer) {
        if (!isValidEntityAddress(entityAddress)) {
            LOGGER.info("{} is not a valid entity address, ignored", entityAddress);
            return;
        }

        Optional<SynchronisationConnection> object = existingSynchronisationConnection.stream()
                .filter(e -> e.getEntityAddress().equalsIgnoreCase(entityAddress)).findFirst();

        switch (action) {
        case CREATE_OBJECT:
            if (object.isPresent()) {
                SynchronisationConnection synchronisationConnection = object.get();
                if (isCustomer == synchronisationConnection.isCustomer()) {
                    LOGGER.info("SynchronisationConnection {} already exists, updated", entityAddress);
                } else {
                    synchronisationConnection.setCustomer(isCustomer);
                    synchronisationConnection.setLastModificationTime(DateTimeUtil.getCurrentDateTime());
                    synchronisationConnection.setLastSynchronisationTime(null);
                }

            } else {
                SynchronisationConnection synchronisationConnection = new SynchronisationConnection();
                synchronisationConnection.setEntityAddress(entityAddress);
                synchronisationConnection.setCustomer(isCustomer);
                synchronisationConnection.setLastModificationTime(DateTimeUtil.getCurrentDateTime());
                synchronisationConnection.setLastSynchronisationTime(null);
                synchronisationConnectionRepository.persist(synchronisationConnection);
            }
            break;
        case DELETE_OBJECT:
            if (object.isPresent()) {
                synchronisationConnectionRepository.delete(object.get());
            } else {
                LOGGER.info("SynchronisationConnection {} doesn't exists, ignored", entityAddress);
            }

            break;
        default:
            LOGGER.error("Action {} not allowed for SynchronisationConnection {}", action, entityAddress);
        }
    }

    private boolean isValidCommonReferenceOperatorNode(JsonNode node) {
        List<String> fieldNames = getFieldNames(node);
        Collection<String> requiredFields = asCollection(ACTION, DOMAIN);

        if (fieldNames.size() == requiredFields.size() && fieldNames.containsAll(requiredFields) && isValidAction(
                node.get(ACTION).asText())
                && isValidDomain(node.get(DOMAIN).asText())) {
            return true;
        }
        LOGGER.info("Invalid CommonReferenceOparetor node {}", node);
        return false;
    }

    private boolean isValidConnectionNode(JsonNode node) {
        List<String> fieldNames = getFieldNames(node);
        Collection<String> requiredFields = asCollection(ACTION, ENTITY_ADDRESS, IS_CUSTOMER);

        if (fieldNames.size() == requiredFields.size() && fieldNames.containsAll(requiredFields) && isValidAction(
                node.get(ACTION).asText())
                && isValidEntityAddress(node.get(ENTITY_ADDRESS).asText())) {
            return true;
        }
        LOGGER.info("Invalid SynchronisationConnection node {}", node);
        return false;
    }

    private Set<String> asCollection(String... entries) {
        return Arrays.stream(entries).collect(Collectors.toCollection(HashSet::new));
    }

    private List getFieldNames(JsonNode node) {
        List<String> fieldNames = new ArrayList<>();
        node.getFieldNames().forEachRemaining(fieldName -> fieldNames.add(fieldName));

        return fieldNames;
    }

    private boolean isValidAction(String action) {
        return (CREATE_OBJECT.equalsIgnoreCase(action) || DELETE_OBJECT.equalsIgnoreCase(action));
    }

    private boolean isValidDomain(String domain) {
        Pattern pattern = Pattern.compile(DOMAIN_PATTERN);
        Matcher matcher = pattern.matcher(domain);
        return matcher.matches();
    }

    private boolean isValidEntityAddress(String entityAddress) {
        Pattern pattern = Pattern.compile(ENTITY_ADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(entityAddress);
        return matcher.matches();
    }

    /**
     * Return all {@Link CommonReferenceOperator}s in json format.
     */

    public String getCommonReferenceOperators() {
        String jsonText = "";
        List<CommonReferenceOperatorDto> domains = commonReferenceOperatorRepository.findAll().stream().map(o -> new CommonReferenceOperatorDto(o.getDomain())).collect(Collectors.toList());

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonText = mapper.writeValueAsString(domains);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonText;
    }

    /**
     * Return all {@Link SynchronisationConnection}s in json format.
     */
    public String getSynchronisationConnections() {
        String jsonText = "";
        List<SynchronisationConnectionDto> domains = synchronisationConnectionRepository.findAll().stream().map(o -> new SynchronisationConnectionDto(o.getEntityAddress(), o.isCustomer())).collect(Collectors.toList());

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonText = mapper.writeValueAsString(domains);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonText;
    }
}
