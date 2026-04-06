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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.os890.cdi.addon.dev.demo.DuplicateBean;
import org.os890.cdi.addon.dynamictestbean.EnableTestBeans;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link DuplicateClassVetoExtension} correctly detects duplicate
 * classes during CDI deployment.
 *
 * <p>The extension is tested by directly invoking its observer method with
 * mock {@link ProcessAnnotatedType} events, simulating the scenario where two
 * bean archives (demo-lib-a and demo-lib-b) contain the same fully-qualified
 * {@link DuplicateBean} class.</p>
 */
@EnableTestBeans
class DuplicateClassVetoExtensionTest {

    private DuplicateClassVetoExtension extension;

    private final List<String> logMessages =
            Collections.synchronizedList(new ArrayList<>());

    private Handler logHandler;

    /**
     * Sets up a fresh extension instance and log handler before each test.
     */
    @BeforeEach
    void setUp() {
        extension = new DuplicateClassVetoExtension();
        logMessages.clear();

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logMessages.add(record.getMessage());
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };
        Logger.getLogger(DuplicateClassVetoExtension.class.getName())
                .addHandler(logHandler);
    }

    /**
     * Verifies that when the same class is reported twice (same path, same
     * classloader), the extension vetoes the second occurrence and logs an
     * info message.
     */
    @SuppressWarnings("unchecked")
    @Test
    void vetoesWhenSameClassScannedTwice() {
        ProcessAnnotatedType<DuplicateBean> firstEvent = createMockEvent(DuplicateBean.class);
        ProcessAnnotatedType<DuplicateBean> secondEvent = createMockEvent(DuplicateBean.class);

        extension.logPathsOfDuplicatedClasses(firstEvent);
        Mockito.verify(firstEvent, Mockito.never()).veto();

        extension.logPathsOfDuplicatedClasses(secondEvent);
        Mockito.verify(secondEvent).veto();

        boolean vetoLogged = logMessages.stream()
                .anyMatch(msg -> msg.contains("loaded twice"));
        assertTrue(vetoLogged,
                "Expected 'loaded twice' in log messages, got: " + logMessages);

        Logger.getLogger(DuplicateClassVetoExtension.class.getName())
                .removeHandler(logHandler);
    }

    /**
     * Verifies that the extension does not veto or log anything for
     * classes that appear only once during scanning.
     */
    @SuppressWarnings("unchecked")
    @Test
    void doesNotFlagUniqueClasses() {
        ProcessAnnotatedType<DuplicateBean> event = createMockEvent(DuplicateBean.class);

        extension.logPathsOfDuplicatedClasses(event);
        Mockito.verify(event, Mockito.never()).veto();

        assertTrue(logMessages.isEmpty(),
                "Expected no log messages for a unique class, got: " + logMessages);

        Logger.getLogger(DuplicateClassVetoExtension.class.getName())
                .removeHandler(logHandler);
    }

    @SuppressWarnings("unchecked")
    private <T> ProcessAnnotatedType<T> createMockEvent(Class<T> beanClass) {
        ProcessAnnotatedType<T> event = Mockito.mock(ProcessAnnotatedType.class);
        AnnotatedType<T> annotatedType = Mockito.mock(AnnotatedType.class);
        Mockito.when(event.getAnnotatedType()).thenReturn(annotatedType);
        Mockito.when(annotatedType.getJavaClass()).thenReturn(beanClass);
        return event;
    }
}
