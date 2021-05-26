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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.AutonomousObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.util.AutonomousObservablePluginSpiMock;
import org.eclipse.keyple.core.service.util.PluginExceptionHandlerMock;
import org.eclipse.keyple.core.service.util.PluginObserverSpiMock;
import org.junit.Before;
import org.junit.Test;

public class AutonomousObservableLocalPluginAdapterTest {

  AutonomousObservablePluginSpi pluginSpi;
  AutonomousObservableLocalPluginAdapter plugin;
  PluginObserverSpiMock observer;
  PluginExceptionHandlerMock exceptionHandler;

  @Before
  public void seTup() throws PluginIOException {
    pluginSpi = new AutonomousObservablePluginSpiMock();
    plugin = new AutonomousObservableLocalPluginAdapter(pluginSpi);
    observer = new PluginObserverSpiMock(null);
    exceptionHandler = new PluginExceptionHandlerMock(null);

    plugin.register();
    plugin.setPluginObservationExceptionHandler(exceptionHandler);
    plugin.addObserver(observer);
  }

  @Test
  public void onReaderConnected_shouldNotify_andCreateReaders() throws Throwable {
    // start plugin
    Set<ReaderSpi> readers = new HashSet<ReaderSpi>();
    readers.add(readerSpi1);

    // register readers
    plugin.onReaderConnected(readers);

    await().atMost(1, TimeUnit.SECONDS).until(eventOfTypeIsReceived(READER_CONNECTED));

    // check event is well formed
    PluginEvent event = observer.getLastEventOfType(READER_CONNECTED);
    assertThat(event.getReaderNames()).size().isEqualTo(1);
    assertThat(event.getReaderNames()).contains(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);

    assertThat(plugin.getReaderNames().size()).isEqualTo(1);
    assertThat(plugin.getReaderNames()).contains(READER_NAME_1);
  }

  @Test
  public void onReaderDisconnected_shouldNotify_andRemoveReaders() throws Throwable {
    onReaderConnected_shouldNotify_andCreateReaders();

    // start plugin
    Set<String> readerNames = new HashSet<String>();
    readerNames.add(READER_NAME_1);

    // register readers
    plugin.onReaderDisconnected(readerNames);

    await().atMost(1, TimeUnit.SECONDS).until(eventOfTypeIsReceived(READER_DISCONNECTED));

    // check event is well formed
    PluginEvent event = observer.getLastEventOfType(READER_DISCONNECTED);
    assertThat(event.getReaderNames()).size().isEqualTo(1);
    assertThat(event.getReaderNames()).contains(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);

    assertThat(plugin.getReaderNames().size()).isEqualTo(0);
  }

  /*
   * Private Helpers
   */
  private Callable<Boolean> eventOfTypeIsReceived(final PluginEvent.EventType eventType) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return observer.hasReceived(eventType);
      }
    };
  }
}
