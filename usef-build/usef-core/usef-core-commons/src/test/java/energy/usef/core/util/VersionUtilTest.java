package energy.usef.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class VersionUtilTest {
    @Test
    public void compare() throws Exception {
        assertTrue(VersionUtil.compareVersions("1.0.8", "1.0.8") <= 0);
        assertTrue(VersionUtil.compareVersions("1.0.8", "1.0.9") <= 0);
        assertTrue(VersionUtil.compareVersions("1.0.8", "1.0.10") <= 0);
        assertTrue(VersionUtil.compareVersions("1.0.8", "1.0.11") <= 0);

        assertTrue(VersionUtil.compareVersions("1.0.8", "1.0.8") >= 0);
        assertTrue(VersionUtil.compareVersions("1.0.9", "1.0.8") >= 0);
        assertTrue(VersionUtil.compareVersions("1.0.10", "1.0.8") >= 0);
        assertTrue(VersionUtil.compareVersions("1.0.11", "1.0.8") >= 0);

    }

}
