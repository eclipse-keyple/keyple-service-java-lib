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
final class WaitForStartDetectStateAdapter extends AbstractObservableStateAdapter {

  /** logger */
  private static final Logger logger =
      LoggerFactory.getLogger(WaitForStartDetectStateAdapter.class);

  /**
   * Creates an instance.
   *
   * @param reader The observable local reader adapter.
   * @since 2.0.0
   */
  WaitForStartDetectStateAdapter(ObservableLocalReaderAdapter reader) {
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
  WaitForStartDetectStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_START_DETECTION, reader, monitoringJob, executorService);
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
      case START_DETECT:
        switchState(MonitoringState.WAIT_FOR_CARD_INSERTION);
        break;
      default:
        if (logger.isTraceEnabled()) {
          logger.trace("Event ignored");
        }
        break;
    }
  }
}
