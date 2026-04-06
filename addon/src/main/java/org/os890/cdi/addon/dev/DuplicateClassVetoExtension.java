/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.os890.cdi.addon.dev;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * CDI portable extension that detects and vetoes duplicate bean classes.
 *
 * <p>When the CDI container discovers the same fully-qualified class name more than once
 * during deployment, this extension either vetoes the duplicate (if it originates from the
 * same classpath location and classloader) or logs a warning (if the paths differ, indicating
 * the class exists in multiple JARs).</p>
 *
 * <p>Register this extension via
 * {@code META-INF/services/jakarta.enterprise.inject.spi.Extension}.</p>
 */
public class DuplicateClassVetoExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(DuplicateClassVetoExtension.class.getName());

    private Map<String, Class<?>> foundClasses = new HashMap<>();

    /**
     * Observes each annotated type discovered by the CDI container and checks
     * whether its class name has already been seen.
     *
     * <p>If the same class is loaded from the same path and classloader, the
     * duplicate is vetoed. If the paths differ, a warning is logged.</p>
     *
     * @param processAnnotatedType the CDI lifecycle event for the discovered type
     */
    protected void logPathsOfDuplicatedClasses(@Observes ProcessAnnotatedType<?> processAnnotatedType) {
        Class<?> beanClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        String className = beanClass.getName();

        if (foundClasses.containsKey(className)) {
            String resourceName = className.replace(".", "/") + ".class";

            Class<?> firstBeanClass = foundClasses.get(className);
            String firstPath = firstBeanClass.getClassLoader().getResource(resourceName).toString();
            String secondPath = beanClass.getClassLoader().getResource(resourceName).toString();
            if (firstPath.equals(secondPath)
                    && firstBeanClass.getClassLoader() == beanClass.getClassLoader()) {
                processAnnotatedType.veto();
                LOG.info(firstPath + " was loaded twice and therefore a veto was triggered");
            } else {
                String msg = "\nfirst path:\n" + firstPath + "\n" + "second path:\n" + secondPath;
                LOG.warning(msg);
            }
        } else {
            foundClasses.put(className, beanClass);
        }
    }

    /**
     * Cleans up the internal class registry after deployment validation completes.
     *
     * @param afterDeploymentValidation the CDI lifecycle event signaling deployment is complete
     */
    protected void cleanup(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        foundClasses.clear();
    }
}
