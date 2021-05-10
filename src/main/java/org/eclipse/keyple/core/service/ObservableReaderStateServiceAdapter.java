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

import java.util.EnumMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionAutonomousSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionNonBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.DontWaitForCardRemovalDuringProcessingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.WaitForCardRemovalDuringProcessingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalAutonomousSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalNonBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Manages the internal state of an {@link ObservableLocalReaderAdapter} Process InternalEvent
 * against the current state
 *
 * @since 2.0
 */
final class ObservableReaderStateServiceAdapter {

  /** logger */
  private static final Logger logger =
      LoggerFactory.getLogger(ObservableReaderStateServiceAdapter.class);

  /** ObservableLocalReaderAdapter to manage event and states */
  private final ObservableLocalReaderAdapter reader;

  private final ObservableReaderSpi readerSpi;

  /** Executor service to provide a unique thread used by the various monitoring jobs */
  private final ExecutorService executorService;

  /** Map of all instantiated states possible */
  private final EnumMap<
          AbstractObservableStateAdapter.MonitoringState, AbstractObservableStateAdapter>
      states;

  /** Current currentState of the Observable Reader */
  private AbstractObservableStateAdapter currentState;

  /**
   * (package-private)<br>
   * Initializes the states according to the interfaces implemented by the provided reader.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0
   */
  ObservableReaderStateServiceAdapter(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
    this.readerSpi = reader.getObservableReaderSpi();

    this.states =
        new EnumMap<AbstractObservableStateAdapter.MonitoringState, AbstractObservableStateAdapter>(
            AbstractObservableStateAdapter.MonitoringState.class);
    this.executorService = Executors.newSingleThreadExecutor();

    // initialize states for each cases:

    // wait for start
    this.states.put(
        AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION,
        new WaitForStartDetectStateAdapter(this.reader));

    // insertion
    if (readerSpi instanceof WaitForCardInsertionAutonomousSpi) {
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(this.reader));
    } else if (readerSpi instanceof WaitForCardInsertionNonBlockingSpi) {
      CardInsertionActiveMonitoringJobAdapter cardInsertionActiveMonitoringJobAdapter =
          new CardInsertionActiveMonitoringJobAdapter(reader, 200, true);
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(
              this.reader, cardInsertionActiveMonitoringJobAdapter, this.executorService));
    } else if (readerSpi instanceof WaitForCardInsertionBlockingSpi) {
      final CardInsertionPassiveMonitoringJobAdapter cardInsertionPassiveMonitoringJobAdapter =
          new CardInsertionPassiveMonitoringJobAdapter(reader);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(
              this.reader, cardInsertionPassiveMonitoringJobAdapter, this.executorService));
    } else {
      throw new IllegalStateException(
          "Reader should implement implement a WaitForCardInsertion interface.");
    }

    // processing
    if (readerSpi instanceof WaitForCardRemovalDuringProcessingSpi) {
      final CardRemovalPassiveMonitoringJobAdapter cardRemovalPassiveMonitoringJobAdapter =
          new CardRemovalPassiveMonitoringJobAdapter(reader);
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING,
          new WaitForCardProcessingStateAdapter(
              this.reader, cardRemovalPassiveMonitoringJobAdapter, this.executorService));
    } else if (readerSpi instanceof DontWaitForCardRemovalDuringProcessingSpi) {
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING,
          new WaitForCardProcessingStateAdapter(this.reader));
    } else {
      throw new IllegalStateException(
          "Reader should implement implement a Wait/DontWait ForCardRemovalDuringProcessing interface.");
    }

    // removal
    if (readerSpi instanceof WaitForCardRemovalAutonomousSpi) {
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(this.reader));

    } else if (readerSpi instanceof WaitForCardRemovalNonBlockingSpi) {
      CardRemovalActiveMonitoringJobAdapter cardRemovalActiveMonitoringJobAdapter =
          new CardRemovalActiveMonitoringJobAdapter(this.reader, 200);
      this.states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(
              this.reader, cardRemovalActiveMonitoringJobAdapter, this.executorService));
    } else if (readerSpi instanceof WaitForCardRemovalBlockingSpi) {
      final CardRemovalPassiveMonitoringJobAdapter cardRemovalPassiveMonitoringJobAdapter =
          new CardRemovalPassiveMonitoringJobAdapter(reader);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(
              this.reader, cardRemovalPassiveMonitoringJobAdapter, this.executorService));
    } else {
      throw new IllegalStateException(
          "Reader should implement implement a WaitForCardRemoval interface.");
    }

    switchState(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION);
  }

  /**
   * (package-private)<br>
   * Thread safe method to communicate an internal event to this reader Use this method to inform
   * the reader of external event like a tag discovered or a card inserted
   *
   * @param event internal event
   * @since 2.0
   */
  synchronized void onEvent(ObservableLocalReaderAdapter.InternalEvent event) {
    switch (event) {
      case CARD_INSERTED:
      case CARD_REMOVED:
      case CARD_PROCESSED:
      case TIME_OUT:
        break;
      case START_DETECT:
        readerSpi.onStartDetection();
        break;
      case STOP_DETECT:
        readerSpi.onStopDetection();
        break;
    }
    this.currentState.onEvent(event);
  }

  /**
   * (package-private)<br>
   * Thread safe method to switch the state of this reader should only be invoked by this reader or
   * its state
   *
   * @param stateId next state to onActivate
   * @since 2.0
   */
  synchronized void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {

    if (currentState != null) {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}] Switch currentState from {} to {}",
            this.reader.getName(),
            this.currentState.getMonitoringState(),
            stateId);
      }
      currentState.onDeactivate();
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] Switch to a new currentState {}", this.reader.getName(), stateId);
      }
    }

    // switch currentState
    currentState = this.states.get(stateId);

    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] New currentState {}", this.reader.getName(), currentState.getMonitoringState());
    }
    // onActivate the new current state
    currentState.onActivate();
  }

  /**
   * (package-private)<br>
   * Get reader current state
   *
   * @return reader current state
   * @since 2.0
   */
  synchronized AbstractObservableStateAdapter getCurrentState() {
    return currentState;
  }

  /**
   * (package-private)<br>
   * Get the reader current monitoring state
   *
   * @return current monitoring state
   * @since 2.0
   */
  synchronized AbstractObservableStateAdapter.MonitoringState getCurrentMonitoringState() {
    return this.currentState.getMonitoringState();
  }

  /**
   * (package-private)<br>
   * Shuts down the {@link ExecutorService} of this reader.
   *
   * <p>This method should be invoked when the reader monitoring ends in order to stop any remaining
   * threads.
   *
   * @since 2.0
   */
  void shutdown() {
    executorService.shutdown();
  }
}
