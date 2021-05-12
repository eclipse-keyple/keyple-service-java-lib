/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ObservableLocalPluginAdapterTest {

  ObservablePluginSpi observablePluginSpiMock;
  String PLUGIN_NAME = "name";
  ObservableLocalPluginAdapter pluginAdapter;
  PluginObserverSpi observer;
  PluginObservationExceptionHandlerSpi exceptionHandler;

  @Before
  public void seTup() {
    observablePluginSpiMock = Mockito.mock(ObservablePluginSpi.class);
    when(observablePluginSpiMock.getName()).thenReturn(PLUGIN_NAME);

    observer = Mockito.mock(PluginObserverSpi.class);
    exceptionHandler = Mockito.mock(PluginObservationExceptionHandlerSpi.class);
    pluginAdapter = new ObservableLocalPluginAdapter(observablePluginSpiMock);
  }

  @After
  public void tearDown() {
    if (pluginAdapter.isMonitoring()) {
      pluginAdapter.unregister();
      assertThat(pluginAdapter.isMonitoring()).isFalse();
    }
  }

  /*
   * Abstract Observable Local PLugin Adapter
   */
  @Test(expected = IllegalStateException.class)
  public void addObserver_onUnregisteredPlugin_throwISE() {
    pluginAdapter.addObserver(observer);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addObserver_withNullObserver_throwIAE() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.addObserver(null);
  }

  @Test(expected = IllegalStateException.class)
  public void addObserver_withoutExceptionHandler_throwISE() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.addObserver(observer);
  }

  @Test
  public void notifyObservers_withEventNotificationExecutorService_isAsync() throws Throwable {}

  @Test
  public void notifyObservers_withoutEventNotificationExecutorService_sync() {}

  @Test
  public void notifyObserver_throwException_isPassedTo_exceptionHandler() {}

  /*
   * Observable Local PLugin Adapter
   */
  @Test
  public void addFirstObserver_shouldStartEventThread() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.setPluginObservationExceptionHandler(exceptionHandler);
    pluginAdapter.addObserver(observer);
    assertThat(pluginAdapter.isMonitoring()).isTrue();
  }

  @Test
  public void removeLastObserver_shouldStopEventThread() throws Throwable {
    addFirstObserver_shouldStartEventThread();
    pluginAdapter.removeObserver(observer);
    assertThat(pluginAdapter.isMonitoring()).isFalse();
  }

  @Test
  public void whileMonitoring_readerName_appears_shouldNotify_andCreateObsverableReader() {}

  @Test
  public void whileMonitoring_readerNames_appears_shouldNotify_andCreateReaders() {}

  @Test
  public void whileMonitoring_readerNames_disappears_shouldNotify_andRemoveReaders() {}

  @Test
  public void whileMonitoring_multipleReaderNamesChange_shouldNotify_multipleEvents() {}

  @Test
  public void whileMonitoring_pluginThrowException_isPassedTo_exceptionHandler() {}
}
