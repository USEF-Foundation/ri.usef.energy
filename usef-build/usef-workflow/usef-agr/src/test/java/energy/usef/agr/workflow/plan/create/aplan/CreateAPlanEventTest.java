package energy.usef.agr.workflow.plan.create.aplan;

import static org.junit.Assert.*;

import org.joda.time.LocalDate;
import org.junit.Test;

import energy.usef.core.util.DateTimeUtil;

/**
 *
 */
public class CreateAPlanEventTest {

    public static final String USEF_ENERGY = "usef.energy";

    @Test
    public void testTodaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate();

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertFalse(event.isExpired());
    }

    @Test
    public void testTomorrowsEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().plusDays(1);

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertFalse(event.isExpired());
    }

    @Test
    public void testYesterdaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().minusDays(1);

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertTrue(event.isExpired());
    }
}
