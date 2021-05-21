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
import static org.awaitility.Awaitility.await;
import static org.eclipse.keyple.core.service.PluginEvent.EventType.READER_CONNECTED;
import static org.eclipse.keyple.core.service.PluginEvent.EventType.READER_DISCONNECTED;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.service.util.ObservableLocalPluginSpiMock;
import org.eclipse.keyple.core.service.util.PluginExceptionHandlerMock;
import org.eclipse.keyple.core.service.util.PluginObserverSpiMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObservableLocalPluginAdapterTest {

  ObservableLocalPluginAdapter pluginAdapter;

  PluginExceptionHandlerMock exceptionHandlerMock;
  ObservableLocalPluginSpiMock observablePluginMock;
  PluginObserverSpiMock observerMock;

  @Before
  public void seTup() {
    observablePluginMock = new ObservableLocalPluginSpiMock(PLUGIN_NAME, null);
    observerMock = new PluginObserverSpiMock(null);
    exceptionHandlerMock = new PluginExceptionHandlerMock(null);

    pluginAdapter = new ObservableLocalPluginAdapter(observablePluginMock);
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
    pluginAdapter.addObserver(observerMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addObserver_withNullObserver_throwIAE() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.addObserver(null);
  }

  @Test(expected = IllegalStateException.class)
  public void addObserver_withoutExceptionHandler_throwISE() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.addObserver(observerMock);
  }

  @Test
  public void notifyObservers_withEventNotificationExecutorService_isAsync() throws Throwable {
    addFirstObserver_shouldStartEventThread();
    pluginAdapter.setEventNotificationExecutorService(Executors.newCachedThreadPool());
    // add reader name
    observablePluginMock.addReaderName(READER_NAME_1);

    await().atMost(1, TimeUnit.SECONDS).until(eventOfTypeIsReceived(READER_CONNECTED));

    // check if exception has been thrown
    assertThat(exceptionHandlerMock.getPluginName()).isNull();
    assertThat(exceptionHandlerMock.getE()).isNull();
  }

  @Test
  public void notifyObserver_throwException_isPassedTo_exceptionHandler() throws Throwable {
    RuntimeException exception = new RuntimeException();
    exceptionHandlerMock = new PluginExceptionHandlerMock(new RuntimeException());
    observerMock = new PluginObserverSpiMock(exception);

    // start plugin
    addFirstObserver_shouldStartEventThread();

    // add reader name
    observablePluginMock.addReaderName(READER_NAME_1);

    await().atMost(1, TimeUnit.SECONDS).until(handlerIsInvoked());
    // when exception handler fails, no error is thrown only logs

  }

  /*
   * Observable Local PLugin Adapter
   */
  @Test
  public void addFirstObserver_shouldStartEventThread() throws Throwable {
    pluginAdapter.register();
    pluginAdapter.setPluginObservationExceptionHandler(exceptionHandlerMock);
    pluginAdapter.addObserver(observerMock);
    assertThat(pluginAdapter.countObservers()).isEqualTo(1);
    assertThat(pluginAdapter.isMonitoring()).isTrue();
  }

  @Test
  public void removeLastObserver_shouldStopEventThread() throws Throwable {
    addFirstObserver_shouldStartEventThread();
    pluginAdapter.removeObserver(observerMock);
    assertThat(pluginAdapter.countObservers()).isEqualTo(0);
    assertThat(pluginAdapter.isMonitoring()).isFalse();
  }

  @Test
  public void clearObserver_shouldStopEventThread() throws Throwable {
    addFirstObserver_shouldStartEventThread();
    pluginAdapter.clearObservers();
    assertThat(pluginAdapter.countObservers()).isEqualTo(0);
    assertThat(pluginAdapter.isMonitoring()).isFalse();
  }

  @Test
  public void whileMonitoring_readerNames_appears_shouldNotify_andCreateReaders() throws Throwable {
    // start plugin
    addFirstObserver_shouldStartEventThread();

    // add reader name
    observablePluginMock.addReaderName(READER_NAME_1);

    await().atMost(1, TimeUnit.SECONDS).until(eventOfTypeIsReceived(READER_CONNECTED));

    // check event is well formed
    PluginEvent event = observerMock.getLastEventOfType(READER_CONNECTED);
    assertThat(event.getReaderNames()).size().isEqualTo(1);
    assertThat(event.getReaderNames()).contains(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);

    // check reader is created
    assertThat(pluginAdapter.getReaderNames()).contains(READER_NAME_1);
  }

  @Test
  public void whileMonitoring_readerNames_disappears_shouldNotify_andRemoveReaders()
      throws Throwable {
    whileMonitoring_readerNames_appears_shouldNotify_andCreateReaders();

    // remove reader name
    observablePluginMock.removeReaderName(READER_NAME_1);

    await().atMost(1, TimeUnit.SECONDS).until(eventOfTypeIsReceived(READER_DISCONNECTED));

    // check event is well formed
    PluginEvent event = observerMock.getLastEventOfType(READER_DISCONNECTED);
    assertThat(event.getReaderNames()).size().isEqualTo(1);
    assertThat(event.getReaderNames()).contains(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void whileMonitoring_observerThrowException_isPassedTo_exceptionHandler()
      throws Throwable {
    RuntimeException exception = new RuntimeException();
    observerMock = new PluginObserverSpiMock(exception);

    // start plugin
    addFirstObserver_shouldStartEventThread();

    // add reader name
    observablePluginMock.addReaderName(READER_NAME_1);

    await().atMost(1, TimeUnit.SECONDS).until(handlerIsInvoked());

    // check if exception has been thrown
    assertThat(exceptionHandlerMock.getPluginName()).isEqualTo(PLUGIN_NAME);
    assertThat(exceptionHandlerMock.getE()).isEqualTo(exception);
  }

  @Test
  public void whileMonitoring_pluginThrowException_isPassedTo_exceptionHandler() throws Throwable {
    PluginIOException exception = new PluginIOException("error");
    observablePluginMock = new ObservableLocalPluginSpiMock(PLUGIN_NAME, exception);
    pluginAdapter = new ObservableLocalPluginAdapter(observablePluginMock);

    // start plugin
    pluginAdapter.register();
    pluginAdapter.setPluginObservationExceptionHandler(exceptionHandlerMock);
    pluginAdapter.addObserver(observerMock);

    await().atMost(1, TimeUnit.SECONDS).until(handlerIsInvoked());

    // check if exception has been thrown
    assertThat(exceptionHandlerMock.getPluginName()).isEqualTo(PLUGIN_NAME);
    assertThat(exceptionHandlerMock.getE().getCause()).isEqualTo(exception);
  }

  /*
   * Callables
   */
  private Callable<Boolean> eventOfTypeIsReceived(final PluginEvent.EventType eventType) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return observerMock.hasReceived(eventType);
      }
    };
  }

  private Callable<Boolean> handlerIsInvoked() {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return exceptionHandlerMock.isInvoked();
      }
    };
  }
}
