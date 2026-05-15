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
   * @return the FSM service id
   * @since 4.0.0
   */
  int getId() {
    return id;
  }

  /**
   * Thread safe method to communicate an internal event to this reader Use this method to inform
   * the reader of external event like a tag discovered or a card inserted
   *
   * @param trigger internal event
   * @since 2.0.0
   */
  synchronized void fire(Trigger trigger) {
    currentFsmState.onTrigger(trigger);
  }

  /**
   * Thread safe method to switch the state of this reader should only be invoked by this reader or
   * its state
   *
   * @param state next state to onActivate
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
