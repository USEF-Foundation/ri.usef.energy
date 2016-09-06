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

package energy.usef.core.util;

import java.util.Iterator;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * The current container utility, the utility provides methods to access injected bean instances.
 */
public class CDIUtil {

    private CDIUtil() {
    }

    /**
     * Gets an instance of an injected bean by class.
     *
     * @param clazz class
     * @return injected bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        BeanManager beanManager = CDI.current().getBeanManager();
        Iterator<Bean<?>> it = beanManager.getBeans(clazz).iterator();
        if (it.hasNext()) {
            Bean<?> bean = it.next();
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
            return (T) beanManager.getReference(bean, clazz, creationalContext);
        } else {
            return null;
        }
    }
}
