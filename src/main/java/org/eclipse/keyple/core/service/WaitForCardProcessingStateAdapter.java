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
 * Wait for card processing state implementation.
 *
 * <p>The state during which the card is being processed by the application.
 *
 * <ul>
 *   <li>Upon SE_PROCESSED event, the machine changes state for WAIT_FOR_SE_REMOVAL or
 *       WAIT_FOR_SE_DETECTION according to the {@link ObservableReader.PollingMode} setting.
 *   <li>Upon CARD_REMOVED event, the machine changes state for WAIT_FOR_SE_INSERTION or
 *       WAIT_FOR_SE_DETECTION according to the {@link ObservableReader.PollingMode} setting.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_SE_DETECTION.
 * </ul>
 *
 * @since 2.0
 */
final class WaitForCardProcessingStateAdapter extends AbstractObservableStateAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(WaitForCardProcessingStateAdapter.class);

  /**
   * (package-private)<br>
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0
   */
  WaitForCardProcessingStateAdapter(ObservableLocalReaderAdapter reader) {
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
  WaitForCardProcessingStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_SE_PROCESSING, reader, monitoringJob, executorService);
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
          "[{}] onEvent => Event {} received in currentState {}",
          getReader().getName(),
          event,
          getMonitoringState());
    }
    /*
     * Process InternalEvent
     */
    switch (event) {
      case SE_PROCESSED:
        if (this.getReader().getPollingMode() == ObservableReader.PollingMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_SE_REMOVAL);
        } else {
          // We close the channels now and notify the application of
          // the CARD_REMOVED event.
          this.getReader().processCardRemoved();
          switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        }
        break;

      case CARD_REMOVED:
        // the card has been removed, we close all channels and return to
        // the currentState of waiting
        // for insertion
        // We notify the application of the CARD_REMOVED event.
        getReader().processCardRemoved();
        if (getReader().getPollingMode() == ObservableReader.PollingMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_SE_INSERTION);
        } else {
          switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        }
        break;

      case STOP_DETECT:
        getReader().processCardRemoved();
        switchState(MonitoringState.WAIT_FOR_START_DETECTION);
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
