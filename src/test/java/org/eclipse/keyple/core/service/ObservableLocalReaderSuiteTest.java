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
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.READER_NAME_1;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.slf4j.Logger;

public class ObservableLocalReaderSuiteTest {

  private ObservableLocalReaderAdapter reader;
  private ObservableReaderSpiMock readerSpi;
  private ReaderObserverSpiMock observer;
  private CardReaderObservationExceptionHandlerSpi handler;
  private Logger logger;

  ObservableLocalReaderSuiteTest(
      ObservableLocalReaderAdapter reader,
      ObservableReaderSpiMock readerSpi,
      ReaderObserverSpiMock observer,
      CardReaderObservationExceptionHandlerSpi handler,
      Logger logger) {
    this.reader = reader;
    this.readerSpi = readerSpi;
    this.observer = observer;
    this.handler = handler;
    this.logger = logger;
  }

  // @Test
  public void initReader_addObserver_startDetection() {

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION);

    reader.setReaderObservationExceptionHandler(handler);
    reader.addObserver(observer);
    reader.startCardDetection(ObservableReader.PollingMode.REPEATING);
    assertThat(reader.countObservers()).isEqualTo(1);

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  // @Test
  public void removeObserver() {
    initReader_addObserver_startDetection();
    reader.removeObserver(observer);
    assertThat(reader.countObservers()).isEqualTo(0);

    // state is not changed
    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  // @Test
  public void clearObservers() {
    initReader_addObserver_startDetection();
    reader.clearObservers();
    assertThat(reader.countObservers()).isEqualTo(0);
  }

  // @Test
  public void insertCard_onWaitForCard_shouldNotify_CardInsertedEvent() {
    initReader_addObserver_startDetection();
    logger.debug("Insert card...");
    readerSpi.setCardPresent(true);

    await()
        .atMost(2, TimeUnit.SECONDS)
        .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING));

    // check event is well formed
    CardReaderEvent event = observer.getLastEventOfType(CardReaderEvent.Type.CARD_INSERTED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
  }

  // @Test
  public void finalizeCardProcessing_afterInsert_switchState() {
    insertCard_onWaitForCard_shouldNotify_CardInsertedEvent();

    logger.debug("Finalize processing...");
    reader.finalizeCardProcessing();

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL));
  }

  // @Test
  public void removeCard_afterFinalize_shouldNotify_CardRemoved() {
    finalizeCardProcessing_afterInsert_switchState();

    logger.debug("Remove card...");
    readerSpi.setCardPresent(false);

    await()
        .atMost(1, TimeUnit.SECONDS)
        .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION));

    // check event is well formed
    CardReaderEvent event = observer.getLastEventOfType(ReaderEvent.Type.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
  }

  // @Test
  public void removeCard_beforeFinalize_shouldNotify_CardRemoved() {
    insertCard_onWaitForCard_shouldNotify_CardInsertedEvent();

    logger.debug("Remove card...");
    readerSpi.setCardPresent(false);

    await()
        .atMost(2, TimeUnit.SECONDS)
        .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION));

    // check event is well formed
    CardReaderEvent event = observer.getLastEventOfType(CardReaderEvent.Type.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
  }

  /*
   * Callables
   */

  private Callable<Boolean> eventOfTypeIsReceived(final ReaderEvent.Type eventType) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return observer.hasReceived(eventType);
      }
    };
  }

  private Callable<Boolean> stateIs(
      final AbstractObservableStateAdapter.MonitoringState monitoringState) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        logger.trace(
            "TEST ... wait for {} is {}", reader.getCurrentMonitoringState(), monitoringState);
        return reader.getCurrentMonitoringState().equals(monitoringState);
      }
    };
  }

  void setObserver(ReaderObserverSpiMock observer) {
    this.observer = observer;
  }
}
