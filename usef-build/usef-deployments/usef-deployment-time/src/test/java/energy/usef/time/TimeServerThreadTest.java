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

package energy.usef.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TimeServerThreadTest {

    @Test
    public void testTimeServerThread() {
        try {
            int timeFactor = 2;
            int port = 41234;

            long baseTime = System.currentTimeMillis();
            TimeServerThread thread = new TimeServerThread(timeFactor, port);
            new Thread(thread).start();

            assertEquals(timeFactor, Integer.valueOf(getUDPInfo("TIMEFACTOR", port)).intValue());
            long newMillis = new DateTime(getUDPInfo("TIME", port)).getMillis();

            long newTime = System.currentTimeMillis();
            long difference = newTime - baseTime;
            long maxCalculated = baseTime + (difference * timeFactor);

            assertTrue(newMillis > newTime);
            assertTrue(newMillis <= maxCalculated);

            thread.shutdown();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private String getUDPInfo(String message, int port) throws IOException {
        byte[] buf = message.getBytes();
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramSocket socket = new DatagramSocket();
        socket.send(new DatagramPacket(buf, buf.length, address, port));
        DatagramPacket result = new DatagramPacket(new byte[128], 128);
        socket.disconnect();
        socket.receive(result);
        socket.disconnect();
        socket.close();
        return new String(result.getData()).trim();
    }
}
