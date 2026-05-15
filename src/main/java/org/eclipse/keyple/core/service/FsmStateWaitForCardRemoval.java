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
import org.eclipse.keypop.reader.ObservableCardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wait for card removal state implementation.
 *
 * <p>The state in which the card is still present and awaiting removal.
 *
 * <ul>
 *   <li>Upon CARD_REMOVED event, the machine changes state for WAIT_FOR_CARD_INSERTION or
 *       WAIT_FOR_CARD_DETECTION according to the {@link ObservableCardReader.DetectionMode}
 *       setting.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_CARD_DETECTION.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForCardRemoval extends FsmState {

  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForCardRemoval.class);

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  FsmStateWaitForCardRemoval(ObservableLocalReaderAdapter reader) {
    this(reader, null, null);
  }

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @param monitoringJob The monitoring job.
   * @param executorService The executor service to use.
   * @since 2.0.0
   */
  FsmStateWaitForCardRemoval(
      ObservableLocalReaderAdapter reader, FsmJob monitoringJob, ExecutorService executorService) {
    super(State.WAIT_FOR_CARD_REMOVAL, reader, monitoringJob, executorService);
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
          "[fsmState={}, reader={}] Processing internal event [type={}]",
          getMonitoringState(),
          getReader().getName(),
          trigger);
    }
    switch (trigger) {
      case CARD_REMOVED:
        if (getReader().getDetectionMode() == ObservableCardReader.DetectionMode.REPEATING) {
          switchState(State.WAIT_FOR_CARD_INSERTION);
        } else {
          switchState(State.WAIT_FOR_START_DETECTION);
        }
        getReader().processCardRemoved();
        break;

      case CARD_DETECTION_STOP_REQUESTED:
        switchState(State.WAIT_FOR_START_DETECTION);
        break;

      default:
        if (logger.isTraceEnabled()) {
          logger.trace(
              "[fsmState={}, reader={}] Internal event ignored",
              getMonitoringState(),
              getReader().getName());
        }
        break;
    }
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmState={}, reader={}] Internal event processed [type={}]",
          getMonitoringState(),
          getReader().getName(),
          trigger);
    }
  }
}
