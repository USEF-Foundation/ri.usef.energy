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

package energy.usef.core.service.notification;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.model.MessageType;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.NotificationHelperService;
import energy.usef.core.util.DateTimeUtil;

/**
 * Simple implementation of the failed message notification hook.
 */
@Stateless
public class NotificationHelperServiceImpl implements NotificationHelperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationHelperServiceImpl.class);

    private static final double MILLIS_PER_MINUTE = 60000.0;

    @Resource
    private TimerService timerService;

    @Inject
    private MessageService ingoingMessageService;

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    public void notifyMessageNotSent(String xml, Message message) {
        MessageMetadata messageMetadata = message.getMessageMetadata();
        if (MessagePrecedence.CRITICAL.equals(messageMetadata.getPrecedence())) {
            LOGGER.warn("CRITICAL MESSAGE IS NOT SENT, MESSAGE ID: {}", messageMetadata.getMessageID());
            jmsService.sendMessageToNotSentQueue(xml);
        } else if (MessagePrecedence.TRANSACTIONAL.equals(messageMetadata.getPrecedence())) {
            jmsService.sendMessageToNotSentQueue(xml);
            LOGGER.warn("TRANSACTIONAL MESSAGE IS NOT SENT, MESSAGE ID: {}", messageMetadata.getMessageID());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyNoMessageResponse(String xml, Message message) {
        MessageMetadata messageMetadata = message.getMessageMetadata();
        // The number of milliseconds per minute, for conversion to a Timer's delay
        long delay;
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        double criticalMessageNotificationFactor = config.getDoubleProperty(ConfigParam.CRITICAL_MESSAGE_NOTIFICATION_FACTOR);
        double transactionalMessageNotificationFactor = config
                .getDoubleProperty(ConfigParam.TRANSACTIONAL_MESSAGE_NOTIFICATION_FACTOR);
        if (MessagePrecedence.CRITICAL.equals(messageMetadata.getPrecedence())) {
            delay = Math.round(MILLIS_PER_MINUTE * ptuDuration * criticalMessageNotificationFactor);
            setTimer(delay, messageMetadata, xml);
        } else if (MessagePrecedence.TRANSACTIONAL.equals(messageMetadata.getPrecedence())) {
            delay = Math.round(MILLIS_PER_MINUTE * ptuDuration * transactionalMessageNotificationFactor);
            setTimer(delay, messageMetadata, xml);
        }
    }

    private void setTimer(long delay, MessageMetadata messageMetadata, String xml) {
        LOGGER.info("Setting a programmatic timeout for {} milliseconds from now.", delay);
        NotificationInfo info = new NotificationInfo(MessageType.fromValue(messageMetadata.getPrecedence()),
                messageMetadata.getConversationID(), xml);
        long factoredDelay = delay / DateTimeUtil.getTimeFactor();
        timerService.createTimer(factoredDelay, info);
    }

    /**
     * Action to perform when timer expires.
     * 
     * @param timer a s{@link Timer}.
     *
     */
    @Timeout
    public void timeoutAction(Timer timer) {
        NotificationInfo info = (NotificationInfo) timer.getInfo();
        if (ingoingMessageService.getMessageResponseByConversationId(info.getConversationId()) != null) {
            LOGGER.info("HOOK: Response for {} message with Conversation Id {} received", info.getMessageType(),
                    info.getConversationId());
            return;
        }
        jmsService.sendMessageToNotSentQueue(info.getXml());
        LOGGER.warn("HOOK: No response found for {} message with Conversation Id {} received", info.getMessageType(),
                info.getConversationId());
    }

}
