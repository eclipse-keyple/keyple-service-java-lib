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
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.READER_NAME_1;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderAutonomousSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.junit.Before;
import org.junit.Test;

public class ObservableLocalReaderAdapterTest {

  ObservableLocalReaderAdapter reader;
  ObservableReaderAutonomousSpiMock readerSpi;
  ReaderObserverSpiMock observer;
  ReaderObservationExceptionHandlerSpi handler;

  /*
   *  With ObservableReaderAutonomousSpi
   */
  @Before
  public void seTup() {
    readerSpi = new ObservableReaderAutonomousSpiMock(READER_NAME_1);
    handler = mock(ReaderObservationExceptionHandlerSpi.class);
    reader = new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME);
    observer = new ReaderObserverSpiMock(null);
  }

  @Test
  public void initReader_addObserver_shoudAddObserver() {
    reader.register();
    reader.setReaderObservationExceptionHandler(handler);
    reader.addObserver(observer);
    reader.startCardDetection(ObservableReader.PollingMode.REPEATING);
    assertThat(reader.countObservers()).isEqualTo(1);
  }

  @Test
  public void removeReader() {
    initReader_addObserver_shoudAddObserver();
    reader.removeObserver(observer);
    assertThat(reader.countObservers()).isEqualTo(0);
  }

  @Test
  public void clearObservers() {
    initReader_addObserver_shoudAddObserver();
    reader.clearObservers();
    assertThat(reader.countObservers()).isEqualTo(0);
  }

  @Test
  public void insertCard_shouldNotify_CardInsertedEvent() {
    initReader_addObserver_shoudAddObserver();
    readerSpi.setCardPresent(true);

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING);

    await()
        .atMost(1, TimeUnit.SECONDS)
        .until(eventOfTypeIsReceived(ReaderEvent.EventType.CARD_INSERTED));

    // check event is well formed
    ReaderEvent event = observer.getLastEventOfType(ReaderEvent.EventType.CARD_INSERTED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void removeCard_shouldNotify_CardRemoved() {
    insertCard_shouldNotify_CardInsertedEvent();
    readerSpi.setCardPresent(false);

    await()
        .atMost(1, TimeUnit.SECONDS)
        .until(eventOfTypeIsReceived(ReaderEvent.EventType.CARD_REMOVED));

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);

    // check event is well formed
    ReaderEvent event = observer.getLastEventOfType(ReaderEvent.EventType.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void finalizeCardProcessing_switchState() {
    insertCard_shouldNotify_CardInsertedEvent();
    reader.finalizeCardProcessing();
    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL);
  }

  /*
   * Callables
   */
  private Callable<Boolean> eventOfTypeIsReceived(final ReaderEvent.EventType eventType) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return observer.hasReceived(eventType);
      }
    };
  }
}
