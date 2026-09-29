/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.service.util.ObservableRemotePluginSpiMock;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObservableRemotePluginAdapterTest {

  private static final String PLUGIN_NAME = "remotePlugin";
  private static final String READER_1 = "reader1";
  private static final String READER_2 = "reader2";
  private static final String READER_3 = "reader3";

  private ObservableRemotePluginSpiMock pluginSpi;
  private ObservableRemotePluginAdapter pluginAdapter;
  private List<String> notifiedEvents;
  private Map<String, Boolean> readerAvailableAtNotification;

  @Before
  public void setUp() throws Exception {
    pluginSpi = new ObservableRemotePluginSpiMock(PLUGIN_NAME);
    pluginSpi.addServerReader(READER_1, true);
    pluginSpi.addServerReader(READER_2, false);
    pluginAdapter = new ObservableRemotePluginAdapter(pluginSpi);
    pluginAdapter.register();

    notifiedEvents = new CopyOnWriteArrayList<String>();
    readerAvailableAtNotification = new ConcurrentHashMap<String, Boolean>();
    pluginAdapter.setPluginObservationExceptionHandler((pluginName, e) -> {});
    pluginAdapter.addObserver(
        event -> {
          for (String readerName : event.getReaderNames()) {
            String key = event.getType() + ":" + readerName;
            readerAvailableAtNotification.put(key, pluginAdapter.getReader(readerName) != null);
            notifiedEvents.add(key);
          }
        });
  }

  @After
  public void tearDown() {
    pluginAdapter.unregister();
  }

  private void sendPluginEvent(String readerName, PluginEvent.Type type) {
    JsonObject json = new JsonObject();
    json.add(
        "pluginEvent",
        JsonUtil.getParser().toJsonTree(new PluginEventAdapter(PLUGIN_NAME, readerName, type)));
    pluginAdapter.onPluginEvent(json.toString());
  }

  private void awaitNotification(String readerName, PluginEvent.Type type) {
    await()
        .atMost(2, TimeUnit.SECONDS)
        .until(() -> notifiedEvents.contains(type + ":" + readerName));
  }

  @Test
  public void register_shouldCreateTheReadersProvidedByTheServer() {
    assertThat(pluginAdapter.getReaders()).hasSize(2);
    assertThat(pluginAdapter.getReader(READER_1)).isInstanceOf(ObservableCardReader.class);
    assertThat(pluginAdapter.getReader(READER_2))
        .isInstanceOf(RemoteReaderAdapter.class)
        .isNotInstanceOf(ObservableCardReader.class);
    assertThat(pluginSpi.getGetReadersCount()).isEqualTo(1);
  }

  @Test
  public void
      onPluginEvent_whenObservableReaderIsConnected_shouldRegisterItBeforeNotifyingObservers() {
    pluginSpi.addServerReader(READER_3, true);

    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);

    assertThat(readerAvailableAtNotification.get("READER_CONNECTED:" + READER_3)).isTrue();
    assertThat(pluginAdapter.getReader(READER_3)).isInstanceOf(ObservableCardReader.class);
    assertThat(pluginAdapter.getReaders()).hasSize(3);
  }

  @Test
  public void
      onPluginEvent_whenNonObservableReaderIsConnected_shouldRegisterANonObservableReader() {
    pluginSpi.addServerReader(READER_3, false);

    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);

    assertThat(pluginAdapter.getReader(READER_3))
        .isInstanceOf(RemoteReaderAdapter.class)
        .isNotInstanceOf(ObservableCardReader.class);
  }

  @Test
  public void onPluginEvent_whenReaderIsConnected_shouldKeepTheExistingReaders() {
    CardReader existingReader1 = pluginAdapter.getReader(READER_1);
    CardReader existingReader2 = pluginAdapter.getReader(READER_2);
    pluginSpi.addServerReader(READER_3, true);

    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);

    assertThat(pluginAdapter.getReader(READER_1)).isSameAs(existingReader1);
    assertThat(pluginAdapter.getReader(READER_2)).isSameAs(existingReader2);
    assertThat(pluginSpi.getCreatedReaderNames())
        .containsExactlyInAnyOrder(READER_1, READER_2, READER_3);
  }

  @Test
  public void
      onPluginEvent_whenAlreadyKnownReaderIsConnected_shouldNotifyObserversWithoutRecreatingIt() {
    CardReader existingReader1 = pluginAdapter.getReader(READER_1);

    sendPluginEvent(READER_1, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_1, PluginEvent.Type.READER_CONNECTED);

    assertThat(pluginAdapter.getReader(READER_1)).isSameAs(existingReader1);
    assertThat(pluginSpi.getGetReadersCount()).isEqualTo(1);
    assertThat(pluginSpi.getCreatedReaderNames()).containsExactlyInAnyOrder(READER_1, READER_2);
  }

  @Test
  public void
      onPluginEvent_whenConnectedReaderIsNotProvidedByTheServer_shouldNotifyObserversWithoutCreatingIt() {
    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);

    assertThat(pluginAdapter.getReader(READER_3)).isNull();
    assertThat(pluginAdapter.getReaders()).hasSize(2);
  }

  @Test
  public void
      onPluginEvent_whenReaderIsDisconnected_shouldUnregisterItBeforeNotifyingObserversAndKeepTheOthers() {
    CardReader existingReader1 = pluginAdapter.getReader(READER_1);
    pluginSpi.removeServerReader(READER_2);

    sendPluginEvent(READER_2, PluginEvent.Type.READER_DISCONNECTED);
    awaitNotification(READER_2, PluginEvent.Type.READER_DISCONNECTED);

    assertThat(readerAvailableAtNotification.get("READER_DISCONNECTED:" + READER_2)).isFalse();
    assertThat(pluginAdapter.getReader(READER_2)).isNull();
    assertThat(pluginAdapter.getReader(READER_1)).isSameAs(existingReader1);
    assertThat(pluginAdapter.getReaders()).hasSize(1);
  }

  @Test
  public void onPluginEvent_whenReaderIsConnectedThenDisconnected_shouldProcessTheEventsInOrder() {
    pluginSpi.addServerReader(READER_3, true);
    // The processing of the connection takes longer than the one of the disconnection
    pluginSpi.setGetReadersDelayMillis(200);

    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    sendPluginEvent(READER_3, PluginEvent.Type.READER_DISCONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_DISCONNECTED);

    assertThat(pluginAdapter.getReader(READER_3)).isNull();
    assertThat(pluginAdapter.getReaders()).hasSize(2);
  }

  @Test
  public void onPluginEvent_whenReadersCannotBeRetrieved_shouldStillNotifyObservers() {
    pluginSpi.addServerReader(READER_3, true);
    pluginSpi.setGetReadersFailing(true);

    sendPluginEvent(READER_3, PluginEvent.Type.READER_CONNECTED);
    awaitNotification(READER_3, PluginEvent.Type.READER_CONNECTED);

    assertThat(pluginAdapter.getReader(READER_3)).isNull();
    assertThat(pluginAdapter.getReaders()).hasSize(2);
  }
}
