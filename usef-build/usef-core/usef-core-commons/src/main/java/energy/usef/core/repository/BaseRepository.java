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

package energy.usef.core.repository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;

/**
 * Base repository class. This class implements basic repository functionality. All specific repository classes should extend this
 * class.
 *
 * @param <T> entity class type managed by this repository
 */
public abstract class BaseRepository<T> {

    public static final int BATCH_FLUSH_SIZE = 20;
    protected final Class<T> clazz;
    @PersistenceContext(unitName = "ApplicationPersistenceUnit")
    protected EntityManager entityManager;

    /**
     * Default constructor.
     */
    public BaseRepository() {
        this.clazz = determineGenericClass();
    }

    /**
     * Verify initialisation of the {@link BaseRepository}.
     */
    @PostConstruct
    public void init() {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager is not initialized");
        }
    }

    /**
     * Determine the {@link ParameterizedType} of this repository. This allows us to detect with entity class this repository is
     * managing.
     *
     * @return Entity class this managed by this repository
     */
    @SuppressWarnings("unchecked")
    private Class<T> determineGenericClass() {
        Class<T> result = null;
        Type genericSuperClass = this.getClass().getGenericSuperclass();

        if (genericSuperClass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericSuperClass;
            Type[] fieldArgTypes = pt.getActualTypeArguments();
            result = (Class<T>) fieldArgTypes[0];
        }

        return result;
    }

    /**
     * Gets EntityManager. This method is supposed to be used by tests.
     *
     * @return EntityManager
     */
    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    /**
     * Sets EntityManager. This method is supposed to be used by tests.
     *
     * @param entityManager EntityManager
     */
    public void setEntityManager(EntityManager entityManager) {
        if (this.entityManager == null) {
            this.entityManager = entityManager;
        }
    }

    /**
     * Synchronize the persistence context to the underlying database.
     *
     * @throws TransactionRequiredException if there is no transaction
     * @throws PersistenceException         if the flush fails
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * Persist the entity in the database.
     *
     * @param entity
     */
    public void persist(T entity) {
        entityManager.persist(entity);
    }

    /**
     * Delete the linked entity.
     *
     * @param entity
     */
    public void delete(T entity) {
        entityManager.remove(entity);
    }

    /**
     * Update the entity in the database.
     *
     * @param entity
     */
    public void update(T entity) {
        entityManager.merge(entity);
    }

    /**
     * Find an T object with id.
     *
     * @param id
     * @return
     */
    public T find(Object id) {
        return entityManager.find(clazz, id);
    }

    /**
     * Adds the parameters based on name->value Map.
     *
     * @param query - The Query object
     * @return
     */
    protected Query addNamedParams(Query query, Map<String, Object> parameters) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    public void persistBatch(List<Object> toBePersisted) {
        int count = 0;
        for (Object entity : toBePersisted) {
            count++;
            getEntityManager().persist(entity);
            if (count % BATCH_FLUSH_SIZE == 0) {
                getEntityManager().flush();
                getEntityManager().clear();
            }
        }
    }
}
