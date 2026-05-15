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
 * Manages the finite state machine (FSM) that drives the card monitoring lifecycle of an {@link
 * ObservableLocalReaderAdapter}.
 *
 * <p>This service instantiates and owns the four FSM states ({@link FsmStateWaitForStartDetection},
 * {@link FsmStateWaitForCardInsertion}, {@link FsmStateWaitForCardProcessing}, {@link
 * FsmStateWaitForCardRemoval}), selects the appropriate monitoring strategy ({@link FsmJobActive}
 * or {@link FsmJobPassive}) based on the SPI interfaces implemented by the underlying reader, and
 * dispatches {@link Trigger} events to the currently active state.
 *
 * @since 2.0.0
 */
final class FsmService {

  private static final Logger logger = LoggerFactory.getLogger(FsmService.class);

  private final int id;
  private final ExecutorService executorService;
  private final EnumMap<FsmState.State, FsmState> states;
  private FsmState currentFsmState;

  /**
   * Initializes the states according to the interfaces implemented by the provided reader.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  FsmService(ObservableLocalReaderAdapter reader) {
    id = reader.getName().hashCode();
    states = new EnumMap<>(FsmState.State.class);
    executorService = Executors.newSingleThreadExecutor();

    // initialize states for each case:
    ObservableReaderSpi readerSpi = reader.getObservableReaderSpi();

    /*
     * START DETECTION
     */
    states.put(
        FsmStateWaitForStartDetection.STATE, new FsmStateWaitForStartDetection(this, reader));

    /*
     * INSERTION
     */
    if (readerSpi instanceof CardInsertionWaiterAsynchronousSpi) {

      states.put(
          FsmStateWaitForCardInsertion.STATE, new FsmStateWaitForCardInsertion(this, reader));

    } else if (readerSpi instanceof CardInsertionWaiterNonBlockingSpi) {

      int sleepDurationMillis =
          ((CardInsertionWaiterNonBlockingSpi) readerSpi).getCardInsertionMonitoringSleepDuration();
      states.put(
          FsmStateWaitForCardInsertion.STATE,
          new FsmStateWaitForCardInsertion(
              this, reader, new FsmJobActive(sleepDurationMillis), executorService));

    } else if (readerSpi instanceof CardInsertionWaiterBlockingSpi) {

      states.put(
          FsmStateWaitForCardInsertion.STATE,
          new FsmStateWaitForCardInsertion(this, reader, new FsmJobPassive(), executorService));

    } else {
      throw new IllegalStateException(
          "Cannot cast the provided reader extension to a valid WaitForCardInsertion interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    /*
     * PROCESSING
     */
    if (readerSpi instanceof CardPresenceMonitorBlockingSpi) {

      states.put(
          FsmStateWaitForCardProcessing.STATE,
          new FsmStateWaitForCardProcessing(this, reader, new FsmJobPassive(), executorService));

    } else {
      states.put(
          FsmStateWaitForCardProcessing.STATE, new FsmStateWaitForCardProcessing(this, reader));
    }

    /*
     * REMOVAL
     */
    if (readerSpi instanceof CardRemovalWaiterAsynchronousSpi) {

      states.put(FsmStateWaitForCardRemoval.STATE, new FsmStateWaitForCardRemoval(this, reader));

    } else if (readerSpi instanceof CardRemovalWaiterNonBlockingSpi) {

      int sleepDurationMillis =
          ((CardRemovalWaiterNonBlockingSpi) readerSpi).getCardRemovalMonitoringSleepDuration();
      states.put(
          FsmStateWaitForCardRemoval.STATE,
          new FsmStateWaitForCardRemoval(
              this, reader, new FsmJobActive(sleepDurationMillis), executorService));

    } else if (readerSpi instanceof CardRemovalWaiterBlockingSpi) {

      states.put(
          FsmStateWaitForCardRemoval.STATE,
          new FsmStateWaitForCardRemoval(this, reader, new FsmJobPassive(), executorService));

    } else {
      throw new IllegalStateException(
          "Cannot cast the provided reader extension to a valid WaitForCardRemoval interface. "
              + "Actual type: "
              + readerSpi.getClass().getName());
    }

    switchState(WAIT_FOR_START_DETECTION);

    if (logger.isTraceEnabled()) {
      logger.trace("[fsmService={}] FSM service initialized [reader={}]", id, reader.getName());
    }
  }

  /**
   * Returns the unique identifier of this FSM service.
   *
   * <p>The identifier is derived from the hash code of the reader name and is used in log traces to
   * correlate FSM events with a specific reader instance.
   *
   * @return A non-zero integer.
   * @since 4.0.0
   */
  int getId() {
    return id;
  }

  /**
   * Thread-safe method that dispatches the given trigger to the currently active state.
   *
   * @param trigger The trigger event to dispatch; must not be null.
   * @since 2.0.0
   */
  synchronized void fire(Trigger trigger) {
    currentFsmState.onTrigger(trigger);
  }

  /**
   * Thread-safe method that deactivates the current state and activates the target state.
   *
   * <p>This method should only be invoked by the FSM service itself or by one of its states.
   *
   * @param state The target state to activate; must not be null.
   * @since 2.0.0
   */
  synchronized void switchState(FsmState.State state) {
    if (currentFsmState != null) {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[fsmService={}] Switching state [from={}, to={}]",
            id,
            currentFsmState.getState(),
            state);
      }
      currentFsmState.onDeactivate();
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("[fsmService={}] Switching state [from=null, to={}]", id, state);
      }
    }
    currentFsmState = states.get(state);
    currentFsmState.onActivate();
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmService={}] State switched [current={}, expected={}]",
          id,
          currentFsmState.getState(),
          state);
    }
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
   * Defines the set of internal events that drive the card observation state machine.
   *
   * @since 2.0.0
   */
  enum Trigger {
    /**
     * A card has been inserted into the reader.
     *
     * @since 2.0.0
     */
    CARD_INSERTED,
    /**
     * The card has been removed from the reader.
     *
     * @since 2.0.0
     */
    CARD_REMOVED,
    /**
     * The application has finished processing the card and released it for further observation.
     *
     * @since 2.0.0
     */
    CARD_PROCESSING_ENDED,
    /**
     * The application has requested that card detection be started.
     *
     * @since 2.0.0
     */
    CARD_DETECTION_START_REQUESTED,
    /**
     * The application has requested that card detection be stopped.
     *
     * @since 2.0.0
     */
    CARD_DETECTION_STOP_REQUESTED,
  }
}
