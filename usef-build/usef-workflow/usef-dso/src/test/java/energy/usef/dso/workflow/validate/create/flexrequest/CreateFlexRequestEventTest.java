package energy.usef.dso.workflow.validate.create.flexrequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.LocalDate;
import org.junit.Test;

import energy.usef.core.util.DateTimeUtil;

/**
 *
 */
public class CreateFlexRequestEventTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
    private Integer[] ptuIndexes = new Integer[]{};

    @Test
    public void testTodaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate();

        CreateFlexRequestEvent event = new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, period, ptuIndexes);
        assertEquals(period, event.getPtuDate());
        assertEquals(CONGESTION_POINT_ENTITY_ADDRESS, event.getCongestionPointEntityAddress());
        assertFalse(event.isExpired());
    }

    @Test
    public void testTomorrowsEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().plusDays(1);

        CreateFlexRequestEvent event = new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, period, ptuIndexes);
        assertEquals(period, event.getPtuDate());
        assertEquals(CONGESTION_POINT_ENTITY_ADDRESS, event.getCongestionPointEntityAddress());
        assertFalse(event.isExpired());
    }

    @Test
    public void testYesterdaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().minusDays(1);

        CreateFlexRequestEvent event = new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, period, ptuIndexes);
        assertEquals(period, event.getPtuDate());
        assertEquals(CONGESTION_POINT_ENTITY_ADDRESS, event.getCongestionPointEntityAddress());
        assertTrue(event.isExpired());
    }
}
