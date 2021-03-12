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
 * Wait for card removal state implementation.
 *
 * <p>The state in which the card is still present and awaiting removal.
 *
 * <ul>
 *   <li>Upon CARD_REMOVED event, the machine changes state for WAIT_FOR_SE_INSERTION or
 *       WAIT_FOR_SE_DETECTION according to the {@link ObservableReader.PollingMode} setting.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_SE_DETECTION.
 * </ul>
 *
 * @since 2.0
 */
class WaitForCardRemovalStateAdapter extends AbstractObservableStateAdapter {

  /** logger */
  private static final Logger logger =
      LoggerFactory.getLogger(WaitForCardRemovalStateAdapter.class);

  WaitForCardRemovalStateAdapter(ObservableLocalReaderAdapter reader) {
    super(MonitoringState.WAIT_FOR_SE_REMOVAL, reader);
  }

  WaitForCardRemovalStateAdapter(
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJob monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_SE_REMOVAL, reader, monitoringJob, executorService);
  }

  @Override
  void onEvent(ObservableLocalReaderAdapter.InternalEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] onEvent => Event {} received in currentState {}", reader.getName(), event, state);
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
        reader.processCardRemoved();
        if (reader.getPollingMode() == ObservableReader.PollingMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_SE_INSERTION);
        } else {
          switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        }
        break;

      case STOP_DETECT:
        reader.processCardRemoved();
        switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        break;

      default:
        logger.warn(
            "[{}] Ignore =>  Event {} received in currentState {}", reader.getName(), event, state);
        break;
    }
  }
}
