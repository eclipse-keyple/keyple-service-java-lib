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

import static org.eclipse.keyple.core.service.FsmState.State.*;

import java.util.EnumMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.*;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the internal state of an {@link ObservableLocalReaderAdapter} Process Trigger against the
 * current state
 *
 * @since 2.0.0
 */
final class FsmService {

  private static final Logger logger = LoggerFactory.getLogger(FsmService.class);

  private final ObservableLocalReaderAdapter reader;
  private final ObservableReaderSpi readerSpi;

  /** Executor service to provide a unique thread used by the various monitoring jobs */
  private final ExecutorService executorService;

  private final EnumMap<FsmState.State, FsmState> states;

  private FsmState currentState;

  /**
   * Initializes the states according to the interfaces implemented by the provided reader.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  FsmService(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
    readerSpi = reader.getObservableReaderSpi();

    states = new EnumMap<>(FsmState.State.class);
    executorService = Executors.newSingleThreadExecutor();

    // initialize states for each case:

    // wait for start
    states.put(WAIT_FOR_START_DETECTION, new FsmStateWaitForStartDetection(this.reader));

    // insertion
    if (readerSpi instanceof CardInsertionWaiterAsynchronousSpi) {
      states.put(WAIT_FOR_CARD_INSERTION, new FsmStateWaitForCardInsertion(this.reader));
    } else if (readerSpi instanceof CardInsertionWaiterNonBlockingSpi) {
      int sleepDurationMillis =
          ((CardInsertionWaiterNonBlockingSpi) readerSpi).getCardInsertionMonitoringSleepDuration();
      FsmJobCardPresenceActiveMonitoring fsmJobCardPresenceActiveMonitoring =
          new FsmJobCardPresenceActiveMonitoring(
              reader, sleepDurationMillis, WAIT_FOR_CARD_INSERTION);
      states.put(
          WAIT_FOR_CARD_INSERTION,
          new FsmStateWaitForCardInsertion(
              this.reader, fsmJobCardPresenceActiveMonitoring, executorService));
    } else if (readerSpi instanceof CardInsertionWaiterBlockingSpi) {
      final FsmJobCardPresencePassiveMonitoring fsmJobCardPresencePassiveMonitoring =
          new FsmJobCardPresencePassiveMonitoring(reader, WAIT_FOR_CARD_INSERTION);
      states.put(
          WAIT_FOR_CARD_INSERTION,
          new FsmStateWaitForCardInsertion(
              this.reader, fsmJobCardPresencePassiveMonitoring, executorService));
    } else {
      throw new IllegalStateException(
          "Cannot cast the provided reader extension to a valid WaitForCardInsertion interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    // processing
    if (readerSpi instanceof CardPresenceMonitorBlockingSpi) {
      final FsmJobCardPresencePassiveMonitoring fsmJobCardPresencePassiveMonitoring =
          new FsmJobCardPresencePassiveMonitoring(reader, WAIT_FOR_CARD_PROCESSING);
      states.put(
          WAIT_FOR_CARD_PROCESSING,
          new FsmStateWaitForCardProcessing(
              this.reader, fsmJobCardPresencePassiveMonitoring, executorService));
    } else {
      states.put(WAIT_FOR_CARD_PROCESSING, new FsmStateWaitForCardProcessing(this.reader));
    }

    // removal
    if (readerSpi instanceof CardRemovalWaiterAsynchronousSpi) {
      states.put(WAIT_FOR_CARD_REMOVAL, new FsmStateWaitForCardRemoval(this.reader));

    } else if (readerSpi instanceof CardRemovalWaiterNonBlockingSpi) {
      int sleepDurationMillis =
          ((CardRemovalWaiterNonBlockingSpi) readerSpi).getCardRemovalMonitoringSleepDuration();
      FsmJobCardPresenceActiveMonitoring fsmJobCardPresenceActiveMonitoring =
          new FsmJobCardPresenceActiveMonitoring(
              this.reader, sleepDurationMillis, WAIT_FOR_CARD_REMOVAL);
      states.put(
          WAIT_FOR_CARD_REMOVAL,
          new FsmStateWaitForCardRemoval(
              this.reader, fsmJobCardPresenceActiveMonitoring, executorService));
    } else if (readerSpi instanceof CardRemovalWaiterBlockingSpi) {
      final FsmJobCardPresencePassiveMonitoring fsmJobCardPresencePassiveMonitoring =
          new FsmJobCardPresencePassiveMonitoring(reader, WAIT_FOR_CARD_REMOVAL);
      states.put(
          WAIT_FOR_CARD_REMOVAL,
          new FsmStateWaitForCardRemoval(
              this.reader, fsmJobCardPresencePassiveMonitoring, executorService));
    } else {
      throw new IllegalStateException(
          "Cannot cast the provided reader extension to a valid WaitForCardRemoval interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    switchState(WAIT_FOR_START_DETECTION);
  }

  /**
   * Thread safe method to communicate an internal event to this reader Use this method to inform
   * the reader of external event like a tag discovered or a card inserted
   *
   * @param trigger internal event
   * @since 2.0.0
   */
  synchronized void onTrigger(Trigger trigger) {
    switch (trigger) {
      case CARD_INSERTED:
      case CARD_REMOVED:
      case CARD_PROCESSING_ENDED:
      case CARD_DETECTION_STOP_REQUESTED: // Manage during the switchState() method call
        break;
      case CARD_DETECTION_START_REQUESTED:
        readerSpi.onStartDetection();
        break;
    }
    currentState.onTrigger(trigger);
  }

  /**
   * Thread safe method to switch the state of this reader should only be invoked by this reader or
   * its state
   *
   * @param stateId next state to onActivate
   * @since 2.0.0
   */
  synchronized void switchState(FsmState.State stateId) {

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
    if (stateId == WAIT_FOR_START_DETECTION) {
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
   * @return the current FSM state.
   * @since 2.0.0
   */
  synchronized FsmState.State getCurrentState() {
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

  /**
   * The events that drive the card's observation state machine.
   *
   * @since 2.0.0
   */
  enum Trigger {
    /**
     * A card has been inserted
     *
     * @since 2.0.0
     */
    CARD_INSERTED,
    /**
     * The card has been removed
     *
     * @since 2.0.0
     */
    CARD_REMOVED,
    /**
     * The application has completed the processing of the card
     *
     * @since 2.0.0
     */
    CARD_PROCESSING_ENDED,
    /**
     * The application has requested the start of card detection
     *
     * @since 2.0.0
     */
    CARD_DETECTION_START_REQUESTED,
    /**
     * The application has requested that card detection is to be stopped.
     *
     * @since 2.0.0
     */
    CARD_DETECTION_STOP_REQUESTED,
  }
}
