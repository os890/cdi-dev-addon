/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.os890.cdi.addon.dev;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DuplicateClassVetoExtension implements Extension {
    private static final Logger LOG = Logger.getLogger(DuplicateClassVetoExtension.class.getName());

    private Map<String, Class> foundClasses = new HashMap<String, Class>();

    protected void logPathsOfDuplicatedClasses(@Observes ProcessAnnotatedType processAnnotatedType) {
        Class beanClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        String className = beanClass.getName();

        if (foundClasses.containsKey(className)) {
            String resourceName = className.replace(".", "/") + ".class";

            Class firstBeanClass = foundClasses.get(className);
            String firstPath = firstBeanClass.getClassLoader().getResource(resourceName).toString();
            String secondPath = beanClass.getClassLoader().getResource(resourceName).toString();
            if (firstPath.equals(secondPath) && firstBeanClass.getClassLoader() == beanClass.getClassLoader() /*needed for weld*/) {
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

    protected void cleanup(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        foundClasses.clear();
    }
}
