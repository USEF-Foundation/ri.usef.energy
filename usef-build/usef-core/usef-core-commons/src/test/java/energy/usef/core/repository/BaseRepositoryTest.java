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

import static org.junit.Assert.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.junit.Test;

/**
 * Unit test to test the abstract base repository.
 */
public class BaseRepositoryTest {

    private class MyType implements Type {

    }

    private class MyEntityManager implements EntityManager {

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#clear()
         */
        @Override
        public void clear() {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#close()
         */
        @Override
        public void close() {
            // Auto-generated method step

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#contains(java.lang.Object)
         */
        @Override
        public boolean contains(Object arg0) {
            // Auto-generated method step
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createEntityGraph(java.lang.Class)
         */
        @Override
        public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createEntityGraph(java.lang.String)
         */
        @Override
        public EntityGraph<?> createEntityGraph(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String)
         */
        @Override
        public Query createNamedQuery(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String, java.lang.Class)
         */
        @Override
        public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNamedStoredProcedureQuery(java.lang.String)
         */
        @Override
        public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String)
         */
        @Override
        public Query createNativeQuery(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.Class)
         */
        @Override
        public Query createNativeQuery(String arg0, @SuppressWarnings("rawtypes") Class arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.String)
         */
        @Override
        public Query createNativeQuery(String arg0, String arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createQuery(java.lang.String)
         */
        @Override
        public Query createQuery(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaQuery)
         */
        @Override
        public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaUpdate)
         */
        @Override
        public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaDelete)
         */
        @Override
        public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createQuery(java.lang.String, java.lang.Class)
         */
        @Override
        public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createStoredProcedureQuery(java.lang.String)
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createStoredProcedureQuery(java.lang.String, java.lang.Class[])
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String arg0, @SuppressWarnings("rawtypes") Class... arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#createStoredProcedureQuery(java.lang.String, java.lang.String[])
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#detach(java.lang.Object)
         */
        @Override
        public void detach(Object arg0) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object)
         */
        @Override
        public <T> T find(Class<T> arg0, Object arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, java.util.Map)
         */
        @Override
        public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType)
         */
        @Override
        public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType,
         * java.util.Map)
         */
        @Override
        public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#flush()
         */
        @Override
        public void flush() {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getCriteriaBuilder()
         */
        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getDelegate()
         */
        @Override
        public Object getDelegate() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getEntityGraph(java.lang.String)
         */
        @Override
        public EntityGraph<?> getEntityGraph(String arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getEntityGraphs(java.lang.Class)
         */
        @Override
        public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getEntityManagerFactory()
         */
        @Override
        public EntityManagerFactory getEntityManagerFactory() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getFlushMode()
         */
        @Override
        public FlushModeType getFlushMode() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getLockMode(java.lang.Object)
         */
        @Override
        public LockModeType getLockMode(Object arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getMetamodel()
         */
        @Override
        public Metamodel getMetamodel() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getProperties()
         */
        @Override
        public Map<String, Object> getProperties() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getReference(java.lang.Class, java.lang.Object)
         */
        @Override
        public <T> T getReference(Class<T> arg0, Object arg1) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#getTransaction()
         */
        @Override
        public EntityTransaction getTransaction() {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#isJoinedToTransaction()
         */
        @Override
        public boolean isJoinedToTransaction() {
            // Auto-generated method step
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#isOpen()
         */
        @Override
        public boolean isOpen() {
            // Auto-generated method step
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#joinTransaction()
         */
        @Override
        public void joinTransaction() {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType)
         */
        @Override
        public void lock(Object arg0, LockModeType arg1) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
         */
        @Override
        public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
            // Auto-generated method step

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#merge(java.lang.Object)
         */
        @Override
        public <T> T merge(T arg0) {
            // Auto-generated method step
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#persist(java.lang.Object)
         */
        @Override
        public void persist(Object arg0) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#refresh(java.lang.Object)
         */
        @Override
        public void refresh(Object arg0) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#refresh(java.lang.Object, java.util.Map)
         */
        @Override
        public void refresh(Object arg0, Map<String, Object> arg1) {
            // Auto-generated method step

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType)
         */
        @Override
        public void refresh(Object arg0, LockModeType arg1) {
            // Auto-generated method step

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
         */
        @Override
        public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
            // Auto-generated method step

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#remove(java.lang.Object)
         */
        @Override
        public void remove(Object arg0) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#setFlushMode(javax.persistence.FlushModeType)
         */
        @Override
        public void setFlushMode(FlushModeType arg0) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#setProperty(java.lang.String, java.lang.Object)
         */
        @Override
        public void setProperty(String arg0, Object arg1) {
            // Auto-generated method step
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.persistence.EntityManager#unwrap(java.lang.Class)
         */
        @Override
        public <T> T unwrap(Class<T> arg0) {
            // Auto-generated method step
            return null;
        }

    }

    private class MyBaseRepository extends BaseRepository<Object> implements ParameterizedType {

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
         */
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[0];
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.reflect.ParameterizedType#getRawType()
         */
        @Override
        public Type getRawType() {
            return new MyType();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.reflect.ParameterizedType#getOwnerType()
         */
        @Override
        public Type getOwnerType() {
            return new MyType();
        }

    }

    @Test
    public void testBaseRepository() {
        MyBaseRepository rep = new MyBaseRepository();

        try {
            rep.init();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
        rep.setEntityManager(null);
        assertNull(rep.getEntityManager());
        rep.setEntityManager(new MyEntityManager());

        rep.init();
        rep.addNamedParams(null, new HashMap<>());
        rep.flush();
        rep.persist(null);
        rep.delete(null);
        rep.update(null);
        rep.find(null);

        assertNotNull(rep.getEntityManager());
    }

}
