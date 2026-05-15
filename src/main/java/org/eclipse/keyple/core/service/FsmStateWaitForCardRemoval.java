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
 * FSM state implementation for the {@link FsmState.StateId#WAIT_FOR_CARD_REMOVAL} phase.
 *
 * <p>In this state the card is still physically present in the reader and the machine waits for it
 * to be removed before resuming detection. This state is entered either after processing has ended
 * (in {@link ObservableCardReader.DetectionMode#REPEATING} mode) or after a card was inserted but
 * did not match the configured selection filter.
 *
 * <ul>
 *   <li>Upon {@link FsmService.Trigger#CARD_REMOVED}, the machine transitions to {@link
 *       FsmState.StateId#WAIT_FOR_CARD_INSERTION} when the detection mode is {@link
 *       ObservableCardReader.DetectionMode#REPEATING}, or to {@link
 *       FsmState.StateId#WAIT_FOR_START_DETECTION} otherwise. The card removal is also notified to
 *       observers.
 *   <li>Upon {@link FsmService.Trigger#CARD_DETECTION_STOP_REQUESTED}, the machine transitions to
 *       {@link FsmState.StateId#WAIT_FOR_START_DETECTION}.
 *   <li>All other triggers are silently ignored.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForCardRemoval extends FsmState {

  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForCardRemoval.class);

  static final StateId STATE_ID = StateId.WAIT_FOR_CARD_REMOVAL;

  /**
   * Creates an instance without a background monitoring job.
   *
   * @param service The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter; must not be null.
   * @since 2.0.0
   */
  FsmStateWaitForCardRemoval(FsmService service, ObservableLocalReaderAdapter reader) {
    this(service, reader, null, null);
  }

  /**
   * Creates an instance with an optional background monitoring job.
   *
   * @param service The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter; must not be null.
   * @param monitoringJob The background monitoring job, or {@code null} if none is required.
   * @param executorService The executor service used to submit the job, or {@code null} when {@code
   *     monitoringJob} is {@code null}.
   * @since 2.0.0
   */
  FsmStateWaitForCardRemoval(
      FsmService service,
      ObservableLocalReaderAdapter reader,
      FsmJob monitoringJob,
      ExecutorService executorService) {
    super(STATE_ID, service, reader, monitoringJob, executorService);
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
          "[fsm={}] Processing trigger [state={}, trigger={}]",
          getServiceId(),
          getStateId(),
          trigger);
    }
    switch (trigger) {
      case CARD_REMOVED:
        if (getReader().getDetectionMode() == ObservableCardReader.DetectionMode.REPEATING) {
          switchState(StateId.WAIT_FOR_CARD_INSERTION);
        } else {
          switchState(StateId.WAIT_FOR_START_DETECTION);
        }
        getReader().processCardRemoved();
        break;

      case CARD_DETECTION_STOP_REQUESTED:
        switchState(StateId.WAIT_FOR_START_DETECTION);
        break;

      default:
        if (logger.isTraceEnabled()) {
          logger.trace("[fsm={}] Trigger ignored [state={}]", getServiceId(), getStateId());
        }
        break;
    }
  }
}
