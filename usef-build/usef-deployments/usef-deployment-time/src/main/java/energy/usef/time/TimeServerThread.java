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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The actual TimeServerThread class. It handles 2 UDP messages: TIME and TIMEFACTOR.
 * 
 */
public class TimeServerThread implements Runnable {

    private static final String UDP_TIME = "TIME";
    private static final String UDP_TIMEFACTOR = "TIMEFACTOR";

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeServerThread.class);

    private long baseTime;
    private long timeFactor = 1;

    private volatile boolean running = true;

    private DatagramSocket socket;

    /**
     * @param timeFactor
     * @param port
     * @throws SocketException
     */
    public TimeServerThread(long timeFactor, int port) throws SocketException {
        this.baseTime = System.currentTimeMillis();
        this.timeFactor = timeFactor;
        socket = new DatagramSocket(port);
    }

    /**
     * Handles the actual Receive/Response logic.
     */
    @Override
    public void run() {
        LOGGER.info("Started USEF TimeServer with BaseTime: {} and TimeFactor: {} ", new LocalDateTime(baseTime).toString(),
                timeFactor);
        while (running) {
            try {
                byte[] buf = new byte[128];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String data = new String(packet.getData()).trim();
                LOGGER.info("RECEIVED: {} ", data);
                if (UDP_TIME.equals(data)) {
                    sendMessage(getTime().toString(), packet);
                } else if (UDP_TIMEFACTOR.equals(data)) {
                    sendMessage("" + timeFactor, packet);
                }
            } catch (IOException e) {
                LOGGER.error("ERROR: {} ", e.getMessage());
            }
        }

    }

    private void sendMessage(String message, DatagramPacket originalPacket) throws IOException {
        byte[] time = message.getBytes();
        LOGGER.debug("SENDING: {} ", new String(time));
        DatagramSocket sendingSocket = new DatagramSocket();
        sendingSocket.send(new DatagramPacket(time, time.length, originalPacket.getAddress(), originalPacket.getPort()));
        sendingSocket.disconnect();
        sendingSocket.close();
    }

    private LocalDateTime getTime() {
        long timePassed = System.currentTimeMillis() - baseTime;
        long usefInstant = baseTime + (timePassed * timeFactor);
        return new LocalDateTime(usefInstant);
    }

    /**
     * Closes the socket to stop the thread.
     */
    public void shutdown() {
        LOGGER.info("USEF TimeServer shut down");
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
