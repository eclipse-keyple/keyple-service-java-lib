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

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ping the card to detect removal thanks to the method {@link
 * ObservableLocalReaderAdapter#isCardPresentPing()}.
 *
 * <p>This method is invoked in another thread.
 *
 * <p>This job should be used by readers who do not have the ability to natively detect the
 * disappearance of the card at the end of the transaction.
 *
 * <p>It is based on sending a neutral APDU command as long as the card is responding, an internal
 * CARD_REMOVED event is fired when the card is no longer responding.
 *
 * <p>By default a delay of 200 ms is inserted between each APDU sending .
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0
 */
class CardRemovalActiveMonitoringJobAdapter extends AbstractMonitoringJob {

  private static final Logger logger =
      LoggerFactory.getLogger(CardRemovalActiveMonitoringJobAdapter.class);

  private final AtomicBoolean loop = new AtomicBoolean();
  private long removalWait = 200;

  /**
   * Create a job monitor job that ping the card with the method isCardPresentPing()
   *
   * @param reader reference to the reader
   */
  public CardRemovalActiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
  }

  /**
   * Create a job monitor job that ping the card with the method isCardPresentPing()
   *
   * @param reader reference to the reader
   * @param removalWait delay between between each APDU sending
   */
  public CardRemovalActiveMonitoringJobAdapter(
      ObservableLocalReaderAdapter reader, long removalWait) {
    super(reader);
    this.removalWait = removalWait;
  }

  /** (package-private)<br> */
  @Override
  Runnable getMonitoringJob(final AbstractObservableStateAdapter state) {

    /*
     * Loop until one the following condition is met : -
     * ObservableLocalReaderAdapter#isCardPresentPing returns false, meaning that the card ping has
     * failed - InterruptedException is caught
     */
    return new Runnable() {
      long retries = 0;

      @Override
      public void run() {
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}] Polling from isCardPresentPing", getReader().getName());
          }
          // re-init loop value to true
          loop.set(true);
          while (loop.get()) {
            if (!getReader().isCardPresentPing()) {
              if (logger.isDebugEnabled()) {
                logger.debug("[{}] the card stopped responding", getReader().getName());
              }
              state.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
              return;
            }
            retries++;

            if (logger.isTraceEnabled()) {
              logger.trace("[{}] Polling retries : {}", getReader().getName(), retries);
            }
            try {
              // wait a bit
              Thread.sleep(removalWait);
            } catch (InterruptedException ignored) {
              // Restore interrupted state...
              Thread.currentThread().interrupt();
              loop.set(false);
            }
          }

          if (logger.isDebugEnabled()) {
            logger.debug("[{}] Polling loop has been stopped", getReader().getName());
          }
        } catch (RuntimeException e) {
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
        }
      }
    };
  }

  /** (package-private)<br> */
  @Override
  void stop() {
    if (logger.isDebugEnabled()) {
      logger.debug("[{}] Stop Polling ", getReader().getName());
    }
    loop.set(false);
  }
}
