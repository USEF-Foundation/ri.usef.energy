package energy.usef.agr.service.business;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.model.SynchronisationConnectionStatus;
import energy.usef.agr.repository.CommonReferenceOperatorRepository;
import energy.usef.agr.repository.SynchronisationConnectionRepository;
import energy.usef.agr.repository.SynchronisationConnectionStatusRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
public class AgrDataBusinessServiceTest {

    AgrDataBusinessService service;

    @Mock
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Mock
    SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    @Before
    public void setUp() throws Exception {
        service = new AgrDataBusinessService();
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Mockito.doNothing().when(commonReferenceOperatorRepository).persist(Matchers.any(CommonReferenceOperator.class));
        Mockito.doNothing().when(commonReferenceOperatorRepository).delete(Matchers.any(CommonReferenceOperator.class));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(getExistingCommonReferenceOperators());

        Whitebox.setInternalState(service, "synchronisationConnectionRepository", synchronisationConnectionRepository);
        Mockito.doNothing().when(synchronisationConnectionRepository).persist(Matchers.any(SynchronisationConnection.class));
        Mockito.doNothing().when(synchronisationConnectionRepository).delete(Matchers.any(SynchronisationConnection.class));
        Mockito.when(synchronisationConnectionRepository.findAll()).thenReturn(getExistingSynchronisationConnections());

        Whitebox.setInternalState(service, "synchronisationConnectionStatusRepository", synchronisationConnectionStatusRepository);
        Mockito.doNothing().when(synchronisationConnectionStatusRepository).persist(Matchers.any(SynchronisationConnectionStatus.class));
        Mockito.doNothing().when(synchronisationConnectionStatusRepository).deleteAll(Matchers.any(CommonReferenceOperator.class));

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testUpdateCommonReferenceOperators() {
        service.updateCommonReferenceOperators(createCommonReferenceOperatorRequest());

        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).persist(Matchers.any(CommonReferenceOperator.class));
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).delete(Matchers.any(CommonReferenceOperator.class));
    }

    @Test
    public void testUpdateSynchronisationConnections() {
        service.updateSynchronisationConnections(createSynchronisationConnectionRequest());

        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).persist(Matchers.any(SynchronisationConnection.class));
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).delete(Matchers.any(SynchronisationConnection.class));
    }

    @Test
    public void testGetCommonReferenceOperators() {
        Assert.assertEquals("[{\"domain\":\"cro0.usef-example.com\"},{\"domain\":\"cro2.usef-example.com\"},{\"domain\":\"cro3.usef-example.com\"}]", service.getCommonReferenceOperators());
    }

    @Test
    public void testGetSynchronisationConnections() {
        Assert.assertEquals("[{\"domain\":\"cro0.usef-example.com\"},{\"domain\":\"cro2.usef-example.com\"},{\"domain\":\"cro3.usef-example.com\"}]", service.getSynchronisationConnections());
    }

    private String createCommonReferenceOperatorRequest() {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("{");
        sb.append("\"action\": \"create\",");
        sb.append("\"domain\": \"cro1.usef-example.com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"create\",");
        sb.append("\"domain\": \"cro2.usef-example.com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"cro0.usef-example.com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"cro4.usef-example.com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"somethingwrong\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"cro4..com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \".com\"");
        sb.append("}");
        sb.append("]");
        return sb.toString();
    }

    private String createSynchronisationConnectionRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("{");
        sb.append("\"action\": \"create\",");
        sb.append("\"entityAddress\": \"ea1.2015-00.A:1\",");
        sb.append("\"isCustomer\": \"true\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"create\",");
        sb.append("\"entityAddress\": \"ea1.2015-00.A:2\",");
        sb.append("\"isCustomer\": \"true\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"entityAddress\": \"ea1.2015-00.A:2\",");
        sb.append("\"isCustomer\": \"true\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"cro4.usef-example.com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"somethingwrong\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \"cro4..com\"");
        sb.append("},");
        sb.append("{");
        sb.append("\"action\": \"delete\",");
        sb.append("\"domain\": \".com\"");
        sb.append("}");
        sb.append("]");
        return sb.toString();
    }


    private List<CommonReferenceOperator> getExistingCommonReferenceOperators() {
        List<CommonReferenceOperator> list = new ArrayList<>();

        list.add(createCommonReferenceOperator(-1L, "cro0.usef-example.com"));
        list.add(createCommonReferenceOperator(-2L, "cro2.usef-example.com"));
        list.add(createCommonReferenceOperator(-3L, "cro3.usef-example.com"));

        return list;
    }

    private CommonReferenceOperator createCommonReferenceOperator(Long id, String domain) {
        CommonReferenceOperator object = new CommonReferenceOperator();
        object.setId(id);
        object.setDomain(domain);
        return object;
    }


    private List<SynchronisationConnection> getExistingSynchronisationConnections() {
        List<SynchronisationConnection> list = new ArrayList<>();

        list.add(createSynchronisationConnection(-1L, "ea1.2015-00.A:0", true));
        list.add(createSynchronisationConnection(-2L, "ea1.2015-00.A:2", true));
        list.add(createSynchronisationConnection(-3L, "ea1.2015-00.A:3", true));

        return list;
    }

    private SynchronisationConnection createSynchronisationConnection(Long id, String entittyAddress, boolean customer) {
        SynchronisationConnection object = new SynchronisationConnection();
        object.setId(id);
        object.setCustomer(customer);
        object.setEntityAddress(entittyAddress);

        return object;
    }


}
