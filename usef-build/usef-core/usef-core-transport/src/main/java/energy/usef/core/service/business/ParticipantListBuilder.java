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

package energy.usef.core.service.business;

import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.TechnicalException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * This service class builds a participant lists from a YAML file.
 *
 * Syntax for such a YAML file is the following:
 * <ul>
 * <li>top element is <code>participants</code></li>
 * <li>for each participant in the configuration file:</li>
 * <ul>
 * <li> <code>domain-name:</code> <i>domain name</i></li>
 * <li> <code>spec-version:</code> <i>version of the specification in use</i></li>
 * <li> <code>[aggregator|dso|brp|cro]-role:</code></li>
 * <ul>
 * <li> <code>entity-address:</code> <i>address of the entity (EA)</i></li>
 * <li> <code>public-keys:["key1","key2"]</code> <i>the two keys key1 and key2</i></li>
 * <li> <code>url:</code> <i>the url</i></li>
 * </ul>
 * </ul> </ul>
 */
@Stateless
public class ParticipantListBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantListBuilder.class);

    private static final String PARTICIPANTS = "participants";
    private static final String DOMAIN_NAME = "domain-name";
    private static final String SPEC_VERSION = "spec-version";

    private static final String PUBLIC_KEYS = "public-keys";
    private static final String URL = "url";

    private static final String DSO_ROLE = "dso-role";
    private static final String AGR_ROLE = "agr-role";
    private static final String BRP_ROLE = "brp-role";
    private static final String CRO_ROLE = "cro-role";
    private static final String MDC_ROLE = "mdc-role";

    /**
     * Builds a participant list from the YAML resource file given in parameter.
     *
     * @param resourcePath - path of the YAML resource. This is the full path inclusive the configuration folder where it is
     *            located. For example /home/<user>/.usef/participants_dns_info.yaml
     * @return a {@link List} of {@link Participant}
     */
    public List<Participant> buildParticipantList(String resourcePath) {
        List<Participant> participants = new ArrayList<>();
        List<LinkedHashMap<String, Object>> participantMaps = loadYamlFile(resourcePath);
        if (participantMaps == null || participantMaps.isEmpty()) {
            return participants;
        }
        participants.addAll(participantMaps.stream().map(this::buildParticipantFromMap).collect(Collectors.toList()));
        return participants;
    }

    @SuppressWarnings("unchecked")
    private List<LinkedHashMap<String, Object>> loadYamlFile(String fileName) {
        Yaml yaml = new Yaml();
        InputStream is = null;

        File file = new File(fileName);
        if (!file.exists()) {
            LOGGER.warn("The participant DNS info file {} can not be found", fileName);
        } else {
            LOGGER.warn("The application is using participant DNS info file {}", file.getAbsolutePath());
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                LOGGER.error("Impossible to load the file {}.", file.getName(), e);
            }
        }

        if (is == null) {
            throw new TechnicalException("Impossible to find the configuration file containing the participant information.");
        }
        Object object = yaml.load(is);

        Map<String, ?> participantsMap = (LinkedHashMap<String, ?>) object;

        return (ArrayList<LinkedHashMap<String, Object>>) participantsMap.get(PARTICIPANTS);
    }

    private Participant buildParticipantFromMap(
            Map<String, ?> participantMap) {
        if (participantMap == null) {
            return null;
        }
        Participant participant = new Participant();
        participant.setDomainName((String) participantMap.get(DOMAIN_NAME));
        participant.setSpecVersion((String) participantMap.get(SPEC_VERSION));
        participant.setPublicKeysFromString((String) participantMap.get(PUBLIC_KEYS));
        participant.setRoles(buildParticipantRoles(participantMap));
        return participant;
    }

    private List<ParticipantRole> buildParticipantRoles(Map<String, ?> participantMap) {
        if (participantMap == null) {
            return new ArrayList<>();
        }
        List<ParticipantRole> roles = new ArrayList<>();

        // for each role loop over the content
        if (participantMap.containsKey(AGR_ROLE)) {
            roles.add(buildAgrParticipantRoles(participantMap));
        }
        if (participantMap.containsKey(BRP_ROLE)) {
            roles.add(buildBrpParticipantRoles(participantMap));
        }
        if (participantMap.containsKey(DSO_ROLE)) {
            roles.add(buildDsoParticipantRoles(participantMap));
        }
        if (participantMap.containsKey(CRO_ROLE)) {
            roles.add(buildCroParticipantRoles(participantMap));
        }
        if (participantMap.containsKey(MDC_ROLE)) {
            roles.add(buildMdcParticipantRoles(participantMap));
        }
        return roles;
    }

    @SuppressWarnings("unchecked")
    private ParticipantRole buildAgrParticipantRoles(Map<String, ?> participantMap) {
        ParticipantRole role = new ParticipantRole(USEFRole.AGR);
        Map<String, ?> aggregator = ((List<LinkedHashMap<String, ?>>) participantMap.get(AGR_ROLE)).get(0);
        role.setUrl(aggregator.containsKey(URL) ? (String) aggregator.get(URL) : null);
        role.setPublicKeysFromString((String) aggregator.get(PUBLIC_KEYS));
        return role;
    }

    @SuppressWarnings("unchecked")
    private ParticipantRole buildBrpParticipantRoles(Map<String, ?> participantMap) {
        ParticipantRole role = new ParticipantRole(USEFRole.BRP);
        Map<String, ?> brp = ((List<Map<String, ?>>) participantMap.get(BRP_ROLE)).get(0);
        role.setUrl(brp.containsKey(URL) ? (String) brp.get(URL) : null);
        role.setPublicKeysFromString((String) brp.get(PUBLIC_KEYS));
        return role;
    }

    @SuppressWarnings("unchecked")
    private ParticipantRole buildDsoParticipantRoles(Map<String, ?> participantMap) {
        ParticipantRole role = new ParticipantRole(USEFRole.DSO);
        Map<String, ?> dso = ((List<LinkedHashMap<String, ?>>) participantMap.get(DSO_ROLE)).get(0);
        role.setUrl(dso.containsKey(URL) ? (String) dso.get(URL) : null);
        role.setPublicKeysFromString((String) dso.get(PUBLIC_KEYS));
        return role;
    }

    @SuppressWarnings("unchecked")
    private ParticipantRole buildCroParticipantRoles(Map<String, ?> participantMap) {
        ParticipantRole role = new ParticipantRole(USEFRole.CRO);
        Map<String, ?> cro = ((List<LinkedHashMap<String, ?>>) participantMap.get(CRO_ROLE)).get(0);
        role.setUrl(cro.containsKey(URL) ? (String) cro.get(URL) : null);
        role.setPublicKeysFromString((String) cro.get(PUBLIC_KEYS));
        return role;
    }

    @SuppressWarnings("unchecked")
    private ParticipantRole buildMdcParticipantRoles(Map<String, ?> participantMap) {
        ParticipantRole role = new ParticipantRole(USEFRole.MDC);
        Map<String, ?> mdc = ((List<LinkedHashMap<String, ?>>) participantMap.get(MDC_ROLE)).get(0);
        role.setUrl(mdc.containsKey(URL) ? (String) mdc.get(URL) : null);
        role.setPublicKeysFromString((String) mdc.get(PUBLIC_KEYS));
        return role;
    }

}
