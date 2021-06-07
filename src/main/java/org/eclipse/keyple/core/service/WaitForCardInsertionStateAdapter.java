/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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
 * (package-private)<br>
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
 * @since 2.0
 */
final class WaitForCardInsertionStateAdapter extends AbstractObservableStateAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(WaitForCardInsertionStateAdapter.class);

  /**
   * (package-private)<br>
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0
   */
  WaitForCardInsertionStateAdapter(ObservableLocalReaderAdapter reader) {
    this(reader, null, null);
  }

  /**
   * (package-private)<br>
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @param monitoringJob The monitoring job.
   * @param executorService The executor service to use.
   * @since 2.0
   */
  WaitForCardInsertionStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_CARD_INSERTION, reader, monitoringJob, executorService);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  void onEvent(ObservableLocalReaderAdapter.InternalEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] onInternalEvent => Event {} received in currentState {}",
          getReader().getName(),
          event,
          getMonitoringState());
    }
    /*
     * Process InternalEvent
     */
    switch (event) {
      case CARD_INSERTED:
        // process default selection if any, return an event, can be null
        ReaderEvent cardEvent = this.getReader().processCardInserted();
        if (cardEvent != null) {
          // switch internal state
          switchState(MonitoringState.WAIT_FOR_CARD_PROCESSING);
          // notify the external observer of the event
          getReader().notifyObservers(cardEvent);
        } else {
          // if none event was sent to the application, back to card detection
          // stay in the same state, however switch to WAIT_FOR_CARD_INSERTION to relaunch
          // the monitoring job
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[{}] onInternalEvent => Inserted card hasn't matched", getReader().getName());
          }
          switchState(MonitoringState.WAIT_FOR_CARD_REMOVAL);
        }
        break;

      case STOP_DETECT:
        switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        break;

      case CARD_REMOVED:
        // the card has been removed during default selection
        if (getReader().getPollingMode() == ObservableReader.DetectionMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_CARD_INSERTION);
        } else {
          switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        }
        break;

      default:
        logger.warn(
            "[{}] Ignore =>  Event {} received in currentState {}",
            getReader().getName(),
            event,
            getMonitoringState());
        break;
    }
  }
}
