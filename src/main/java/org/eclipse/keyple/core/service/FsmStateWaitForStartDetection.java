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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FSM state implementation for the {@link FsmState.State#WAIT_FOR_START_DETECTION} phase.
 *
 * <p>In this idle state the reader does not monitor for card presence; it waits for the application
 * to request the start of card detection. On activation, {@link
 * org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi#onStopDetection()} is
 * called to notify the underlying hardware that detection is paused.
 *
 * <ul>
 *   <li>Upon {@link FsmService.Trigger#CARD_DETECTION_START_REQUESTED}, the machine transitions to
 *       {@link FsmState.State#WAIT_FOR_CARD_INSERTION}.
 *   <li>All other triggers are silently ignored.
 * </ul>
 *
 * @since 2.0.0
 */
final class FsmStateWaitForStartDetection extends FsmState {

  private static final Logger logger = LoggerFactory.getLogger(FsmStateWaitForStartDetection.class);

  static final State STATE = State.WAIT_FOR_START_DETECTION;

  /**
   * Creates an instance without a background monitoring job.
   *
   * @param fsmService The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter; must not be null.
   * @since 2.0.0
   */
  FsmStateWaitForStartDetection(FsmService fsmService, ObservableLocalReaderAdapter reader) {
    super(STATE, fsmService, reader, null, null);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also calls {@link
   * org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi#onStopDetection()} to
   * notify the underlying hardware that card detection is now paused.
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
