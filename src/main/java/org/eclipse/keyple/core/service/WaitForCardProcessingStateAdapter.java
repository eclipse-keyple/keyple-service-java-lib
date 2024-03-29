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
 * Wait for card processing state implementation.
 *
 * <p>The state during which the card is being processed by the application.
 *
 * <ul>
 *   <li>Upon CARD_PROCESSED event, the machine changes state for WAIT_FOR_CARD_REMOVAL or
 *       WAIT_FOR_CARD_DETECTION according to the {@link ObservableCardReader.DetectionMode}
 *       setting.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_CARD_DETECTION.
 * </ul>
 *
 * @since 2.0.0
 */
final class WaitForCardProcessingStateAdapter extends AbstractObservableStateAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(WaitForCardProcessingStateAdapter.class);

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  WaitForCardProcessingStateAdapter(ObservableLocalReaderAdapter reader) {
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
  WaitForCardProcessingStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_CARD_PROCESSING, reader, monitoringJob, executorService);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
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
      case CARD_PROCESSED:
        if (this.getReader().getDetectionMode() == ObservableCardReader.DetectionMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_CARD_REMOVAL);
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

        // FIXME bug if mode REPEATING and if user execute the stopCardDetection in the same thread
        // during the processCardRemoved method.
        getReader().processCardRemoved();
        if (getReader().getDetectionMode() == ObservableCardReader.DetectionMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_CARD_INSERTION);
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
