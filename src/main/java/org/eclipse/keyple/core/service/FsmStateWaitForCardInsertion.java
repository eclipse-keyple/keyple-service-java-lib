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
 * Wait for card insertion state implementation.
 *
 * <p>The state during which the insertion of a card is expected.
 *
 * <ul>
 *   <li>Upon CARD_INSERTED event, the default selection is processed if required and if the
 *       conditions are met (ALWAYS or CARD_MATCHED) the machine changes state for
 *       WAIT_FOR_CARD_PROCESSING.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_CARD_DETECTION.
 *   <li>Upon CARD_REMOVED event, the machine changes state for WAIT_FOR_CARD_DETECTION.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForCardInsertion extends FsmState {

  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForCardInsertion.class);

  static final State STATE = State.WAIT_FOR_CARD_INSERTION;

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  FsmStateWaitForCardInsertion(FsmService fsmService, ObservableLocalReaderAdapter reader) {
    this(fsmService, reader, null, null);
  }

  /**
   * Creates an instance.
   *
   * @param fsmService
   * @param reader The observable local reader adapter.
   * @param monitoringJob The monitoring job.
   * @param executorService The executor service to use.
   * @since 2.0.0
   */
  FsmStateWaitForCardInsertion(
      FsmService fsmService,
      ObservableLocalReaderAdapter reader,
      FsmJob monitoringJob,
      ExecutorService executorService) {
    super(STATE, fsmService, reader, monitoringJob, executorService);
  }

  /**
   * {@inheritDoc}
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
          getState(),
          getFsmServiceId(),
          trigger);
    }
    switch (trigger) {
      case CARD_INSERTED:
        // process default selection if any, return an event, can be null
        CardReaderEvent cardEvent = getReader().processCardInserted();
        if (cardEvent != null) {
          // switch internal state
          switchState(State.WAIT_FOR_CARD_PROCESSING);
          // notify the external observer of the event
          getReader().notifyObservers(cardEvent);
        } else {
          // if none event was sent to the application, back to card detection
          // stay in the same state, however switch to WAIT_FOR_CARD_INSERTION to relaunch
          // the monitoring job
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsmState={}, fsmService={}] Inserted card hasn't matched",
                getState(),
                getFsmServiceId());
          }
          switchState(State.WAIT_FOR_CARD_REMOVAL);
        }
        break;

      case CARD_DETECTION_STOP_REQUESTED:
        switchState(State.WAIT_FOR_START_DETECTION);
        break;

      default:
        if (logger.isTraceEnabled()) {
          logger.trace(
              "[fsmState={}, fsmService={}] Internal event ignored", getState(), getFsmServiceId());
        }
        break;
    }
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmState={}, fsmService={}] Internal event processed [type={}]",
          getState(),
          getFsmServiceId(),
          trigger);
    }
  }
}
