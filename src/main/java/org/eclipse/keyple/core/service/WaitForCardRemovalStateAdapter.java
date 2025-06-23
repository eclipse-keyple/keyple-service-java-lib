/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
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
final class WaitForCardRemovalStateAdapter extends AbstractObservableStateAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(WaitForCardRemovalStateAdapter.class);

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  WaitForCardRemovalStateAdapter(ObservableLocalReaderAdapter reader) {
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
  WaitForCardRemovalStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_CARD_REMOVAL, reader, monitoringJob, executorService);
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
          "Internal event [{}] received for reader [{}] in current state [{}]",
          event,
          getReader().getName(),
          getMonitoringState());
    }
    /*
     * Process InternalEvent
     */
    switch (event) {
      case CARD_REMOVED:
        // the card has been removed, we close all channels and return to
        // the currentState of waiting
        // for insertion
        // We notify the application of the CARD_REMOVED event.
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
        if (logger.isTraceEnabled()) {
          logger.trace("Event ignored");
        }
        break;
    }
  }
}
