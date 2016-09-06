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

package energy.usef.core.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;

import org.joda.time.LocalDateTime;

/**
 * Entity representing the hash of a incoming signed message.
 *
 */
@Entity
@Table(name = "SIGNED_MESSAGE_HASH")
public class SignedMessageHash {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "CREATION_TIME", nullable = false)
    @Temporal(TIMESTAMP)
    private Date creationTime;

    @Lob
    @Column(name = "HASHED_CONTENT", nullable = false)
    private byte[] hashedContent;

    public SignedMessageHash() {
        // empty constructor, do nothing.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreationTime() {
        if (creationTime == null) {
            return null;
        }
        return LocalDateTime.fromDateFields(creationTime);
    }

    public void setCreationTime(LocalDateTime creationTime) {
        if (creationTime == null) {
            this.creationTime = null;
        } else {
            this.creationTime = creationTime.toDateTime().toDate();
        }
    }


    public byte[] getHashedContent() {
        return hashedContent == null ? new byte[0] : Arrays.copyOf(hashedContent, hashedContent.length);
    }

    public void setHashedContent(byte[] hashedContent) {
        this.hashedContent = hashedContent;
    }

    @Override
    public String toString() {
        return "SignedMessageHash" + "[" +
                "id=" + id +
                ", hashedContent=" + Arrays.toString(hashedContent) +
                "]";
    }
}
