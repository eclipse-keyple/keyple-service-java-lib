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

import java.util.EnumMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.*;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.WaitForCardRemovalDuringProcessingBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the internal state of an {@link ObservableLocalReaderAdapter} Process InternalEvent
 * against the current state
 *
 * @since 2.0.0
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
   * Initializes the states according to the interfaces implemented by the provided reader.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  ObservableReaderStateServiceAdapter(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
    readerSpi = reader.getObservableReaderSpi();

    states = new EnumMap<>(AbstractObservableStateAdapter.MonitoringState.class);
    executorService = Executors.newSingleThreadExecutor();

    // initialize states for each case:

    // wait for start
    states.put(
        AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION,
        new WaitForStartDetectStateAdapter(this.reader));

    // insertion
    if (readerSpi instanceof CardInsertionWaiterAsynchronousSpi
        || readerSpi instanceof WaitForCardInsertionAutonomousSpi) {
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(this.reader));
    } else if (readerSpi instanceof CardInsertionWaiterNonBlockingSpi
        || readerSpi instanceof WaitForCardInsertionNonBlockingSpi) {
      int sleepDurationMillis =
          readerSpi instanceof CardInsertionWaiterNonBlockingSpi
              ? ((CardInsertionWaiterNonBlockingSpi) readerSpi)
                  .getCardInsertionMonitoringSleepDuration()
              : 100;
      CardInsertionActiveMonitoringJobAdapter cardInsertionActiveMonitoringJobAdapter =
          new CardInsertionActiveMonitoringJobAdapter(reader, sleepDurationMillis, true);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(
              this.reader, cardInsertionActiveMonitoringJobAdapter, executorService));
    } else if (readerSpi instanceof CardInsertionWaiterBlockingSpi
        || readerSpi instanceof WaitForCardInsertionBlockingSpi) {
      final CardInsertionPassiveMonitoringJobAdapter cardInsertionPassiveMonitoringJobAdapter =
          new CardInsertionPassiveMonitoringJobAdapter(reader);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION,
          new WaitForCardInsertionStateAdapter(
              this.reader, cardInsertionPassiveMonitoringJobAdapter, executorService));
    } else {
      throw new IllegalStateException(
          "Cannot cast provided reader extension to a valid WaitForCardInsertion interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    // processing
    if (readerSpi instanceof CardPresenceMonitorBlockingSpi
        || readerSpi instanceof WaitForCardRemovalDuringProcessingBlockingSpi) {
      final CardRemovalPassiveMonitoringJobAdapter cardRemovalPassiveMonitoringJobAdapter =
          new CardRemovalPassiveMonitoringJobAdapter(reader);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING,
          new WaitForCardProcessingStateAdapter(
              this.reader, cardRemovalPassiveMonitoringJobAdapter, executorService));
    } else {
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING,
          new WaitForCardProcessingStateAdapter(this.reader));
    }

    // removal
    if (readerSpi instanceof CardRemovalWaiterAsynchronousSpi
        || readerSpi instanceof WaitForCardRemovalAutonomousSpi) {
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(this.reader));

    } else if (readerSpi instanceof CardRemovalWaiterNonBlockingSpi
        || readerSpi instanceof WaitForCardRemovalNonBlockingSpi) {
      int sleepDurationMillis =
          readerSpi instanceof CardRemovalWaiterNonBlockingSpi
              ? ((CardRemovalWaiterNonBlockingSpi) readerSpi)
                  .getCardRemovalMonitoringSleepDuration()
              : 100;
      CardRemovalActiveMonitoringJobAdapter cardRemovalActiveMonitoringJobAdapter =
          new CardRemovalActiveMonitoringJobAdapter(this.reader, sleepDurationMillis);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(
              this.reader, cardRemovalActiveMonitoringJobAdapter, executorService));
    } else if (readerSpi instanceof CardRemovalWaiterBlockingSpi
        || readerSpi instanceof WaitForCardRemovalBlockingSpi) {
      final CardRemovalPassiveMonitoringJobAdapter cardRemovalPassiveMonitoringJobAdapter =
          new CardRemovalPassiveMonitoringJobAdapter(reader);
      states.put(
          AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL,
          new WaitForCardRemovalStateAdapter(
              this.reader, cardRemovalPassiveMonitoringJobAdapter, executorService));
    } else {
      throw new IllegalStateException(
          "Cannot cast provided reader extension to a valid WaitForCardRemoval interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    switchState(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION);
  }

  /**
   * Thread safe method to communicate an internal event to this reader Use this method to inform
   * the reader of external event like a tag discovered or a card inserted
   *
   * @param event internal event
   * @since 2.0.0
   */
  synchronized void onEvent(ObservableLocalReaderAdapter.InternalEvent event) {
    switch (event) {
      case CARD_INSERTED:
      case CARD_REMOVED:
      case CARD_PROCESSED:
      case STOP_DETECT: // Manage during the switchState() method call
      case TIME_OUT:
        break;
      case START_DETECT:
        readerSpi.onStartDetection();
        break;
    }
    currentState.onEvent(event);
  }

  /**
   * Thread safe method to switch the state of this reader should only be invoked by this reader or
   * its state
   *
   * @param stateId next state to onActivate
   * @since 2.0.0
   */
  synchronized void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {

    if (currentState != null) {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[fsmService={}] Switching state [from={}, to={}]",
            reader.getName(),
            currentState.getMonitoringState(),
            stateId);
      }
      currentState.onDeactivate();
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[fsmService={}] Switching state [from=null, to={}]", reader.getName(), stateId);
      }
    }

    // switch currentState
    currentState = states.get(stateId);

    // As soon as the state machine returns to the WAIT_FOR_START_DETECTION state,
    // we deactivate card detection in the plugin.
    if (stateId == AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION) {
      readerSpi.onStopDetection();
    }

    // onActivate the new current state
    currentState.onActivate();

    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmService={}] State switched [current={}, expected={}]",
          reader.getName(),
          currentState.getMonitoringState(),
          stateId);
    }
  }

  /**
   * Get the reader current monitoring state
   *
   * @return current monitoring state
   * @since 2.0.0
   */
  synchronized AbstractObservableStateAdapter.MonitoringState getCurrentMonitoringState() {
    return currentState.getMonitoringState();
  }

  /**
   * Shuts down the {@link ExecutorService} of this reader.
   *
   * <p>This method should be invoked when the reader monitoring ends in order to stop any remaining
   * threads.
   *
   * @since 2.0.0
   */
  void shutdown() {
    executorService.shutdown();
  }
}
