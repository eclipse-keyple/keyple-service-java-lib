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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wait for start the card detection state implementation.
 *
 * <p>The state during which the reader does not wait for a card to be inserted but for a signal
 * from the application to do so (switch to the WAIT_FOR_CARD_INSERTION state).
 *
 * <ul>
 *   <li>Upon START_DETECT event, the machine changes state for WAIT_FOR_CARD_INSERTION.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForStartDetection extends FsmState {

  /** logger */
  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForStartDetection.class);

  static final State STATE = State.WAIT_FOR_START_DETECTION;

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  FsmStateWaitForStartDetection(FsmService fsmService, ObservableLocalReaderAdapter reader) {
    this(fsmService, reader, null, null);
  }

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @param monitoringJob The monitoring job.
   * @param executorService The executor service to use.
   * @since 2.0.0
   */
  FsmStateWaitForStartDetection(
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
    getReader().getObservableReaderSpi().onStopDetection();
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
      case CARD_DETECTION_START_REQUESTED:
        switchState(State.WAIT_FOR_CARD_INSERTION);
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
