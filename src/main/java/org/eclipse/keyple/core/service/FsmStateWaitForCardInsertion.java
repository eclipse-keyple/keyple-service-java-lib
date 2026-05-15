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

import java.util.concurrent.ExecutorService;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FSM state implementation for the {@link FsmState.StateId#WAIT_FOR_CARD_INSERTION} phase.
 *
 * <p>In this state the reader actively monitors for a card to be presented. On activation, {@link
 * org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi#onStartDetection()} is
 * called to notify the underlying hardware that detection has started.
 *
 * <ul>
 *   <li>Upon {@link FsmService.Trigger#CARD_INSERTED}, the configured default card selection is
 *       executed if one is set. If a {@link org.eclipse.keypop.reader.CardReaderEvent} is produced,
 *       the machine transitions to {@link FsmState.StateId#WAIT_FOR_CARD_PROCESSING} and observers
 *       are notified. If no event is produced (card did not match the selection filter), the
 *       machine transitions to {@link FsmState.StateId#WAIT_FOR_CARD_REMOVAL} to wait for the
 *       unmatched card to be removed before resuming detection.
 *   <li>Upon {@link FsmService.Trigger#CARD_DETECTION_STOP_REQUESTED}, the machine transitions to
 *       {@link FsmState.StateId#WAIT_FOR_START_DETECTION}.
 *   <li>All other triggers are silently ignored.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForCardInsertion extends FsmState {

  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForCardInsertion.class);

  static final StateId STATE_ID = StateId.WAIT_FOR_CARD_INSERTION;

  /**
   * Creates an instance without a background monitoring job.
   *
   * @param fsmService The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter; must not be null.
   * @since 2.0.0
   */
  FsmStateWaitForCardInsertion(FsmService fsmService, ObservableLocalReaderAdapter reader) {
    this(fsmService, reader, null, null);
  }

  /**
   * Creates an instance with an optional background monitoring job.
   *
   * @param fsmService The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter; must not be null.
   * @param monitoringJob The background monitoring job, or {@code null} if none is required.
   * @param executorService The executor service used to submit the job, or {@code null} when {@code
   *     monitoringJob} is {@code null}.
   * @since 2.0.0
   */
  FsmStateWaitForCardInsertion(
      FsmService fsmService,
      ObservableLocalReaderAdapter reader,
      FsmJob monitoringJob,
      ExecutorService executorService) {
    super(STATE_ID, fsmService, reader, monitoringJob, executorService);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also calls {@link
   * org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi#onStartDetection()} to
   * notify the underlying hardware that card detection has started.
   *
   * @since 4.0.0
   */
  @Override
  void onActivate() {
    super.onActivate();
    getReader().getObservableReaderSpi().onStartDetection();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  void onTrigger(FsmService.Trigger trigger) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmState={}, fsmService={}] Processing internal event [type={}]",
          getStateId(),
          getServiceId(),
          trigger);
    }
    switch (trigger) {
      case CARD_INSERTED:
        // process default selection if any, return an event, can be null
        CardReaderEvent cardEvent = getReader().processCardInserted();
        if (cardEvent != null) {
          // switch internal state
          switchState(StateId.WAIT_FOR_CARD_PROCESSING);
          // notify the external observer of the event
          getReader().notifyObservers(cardEvent);
        } else {
          // if none event was sent to the application, back to card detection
          // stay in the same state, however switch to WAIT_FOR_CARD_INSERTION to relaunch
          // the monitoring job
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsmState={}, fsmService={}] Inserted card hasn't matched",
                getStateId(),
                getServiceId());
          }
          switchState(StateId.WAIT_FOR_CARD_REMOVAL);
        }
        break;

      case CARD_DETECTION_STOP_REQUESTED:
        switchState(StateId.WAIT_FOR_START_DETECTION);
        break;

      default:
        if (logger.isTraceEnabled()) {
          logger.trace(
              "[fsmState={}, fsmService={}] Internal event ignored", getStateId(), getServiceId());
        }
        break;
    }
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmState={}, fsmService={}] Internal event processed [type={}]",
          getStateId(),
          getServiceId(),
          trigger);
    }
  }
}
