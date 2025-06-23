/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.READER_NAME;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.service.util.ControllableReaderSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.slf4j.Logger;

public class ObservableLocalReaderSuite {

  private ObservableLocalReaderAdapter reader;
  private ControllableReaderSpiMock readerSpi;
  private ReaderObserverSpiMock observer;
  private CardReaderObservationExceptionHandlerSpi handler;
  private Logger logger;

  ObservableLocalReaderSuite(
      ObservableLocalReaderAdapter reader,
      ControllableReaderSpiMock readerSpi,
      ReaderObserverSpiMock observer,
      CardReaderObservationExceptionHandlerSpi handler,
      Logger logger) {
    this.reader = reader;
    this.readerSpi = readerSpi;
    this.observer = observer;
    this.handler = handler;
    this.logger = logger;
  }

  public void addFirstObserver_should_startDetection() {

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION);

    reader.setReaderObservationExceptionHandler(handler);
    reader.addObserver(observer);
    reader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);
    assertThat(reader.countObservers()).isEqualTo(1);

    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  public void removeLastObserver_shoul_StopDetection() {
    addFirstObserver_should_startDetection();
    reader.removeObserver(observer);
    assertThat(reader.countObservers()).isZero();

    // state is not changed
    assertThat(reader.getCurrentMonitoringState())
        .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  public void clearObservers_shouldRemove_allObservers() {
    addFirstObserver_should_startDetection();
    reader.clearObservers();
    assertThat(reader.countObservers()).isZero();
  }

  public void insertCard_onWaitForCard_shouldNotify_CardInsertedEvent() {
    addFirstObserver_should_startDetection();
    logger.debug("Insert card...");
    readerSpi.setCardPresent(true);

    await()
        .atMost(2, TimeUnit.SECONDS)
        .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING));

    // check event is well formed
    CardReaderEvent event = observer.getLastEventOfType(CardReaderEvent.Type.CARD_INSERTED);
    assertThat(event).isNotNull();
    assertThat(event.getReaderName()).isEqualTo(READER_NAME);
  }

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
    CardReaderEvent event = observer.getLastEventOfType(CardReaderEvent.Type.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME);
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
    assertThat(event.getReaderName()).isEqualTo(READER_NAME);
  }

  /*
   * Callables
   */

  private Callable<Boolean> eventOfTypeIsReceived(final CardReaderEvent.Type eventType) {
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
