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
 * <p>By default a delay of 100 ms is inserted between each APDU sending .
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0.0
 */
final class CardRemovalActiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardRemovalActiveMonitoringJobAdapter.class);

  private final AtomicBoolean loop = new AtomicBoolean();
  private final long sleepDurationMillis;

  /**
   * Create a job monitor job that ping the card with the method isCardPresentPing()
   *
   * @param reader reference to the reader
   * @param sleepDurationMillis delay between each APDU sending
   * @since 2.0.0
   */
  public CardRemovalActiveMonitoringJobAdapter(
      ObservableLocalReaderAdapter reader, long sleepDurationMillis) {
    super(reader);
    this.sleepDurationMillis = sleepDurationMillis;
  }

  /**
   * Gets the monitoring process.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  @Override
  Runnable getMonitoringJob(final AbstractObservableStateAdapter monitoringState) {

    return new Runnable() {

      /**
       * Monitoring loop
       *
       * <p>Sends a neutral command to the card and loops as long as the card responds. <br>
       * Triggers a CARD_REMOVED event and exits as soon as the communication with the card is lost.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
       */
      @Override
      public void run() {
        try {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "Start monitoring job polling process using 'isCardPresentPing()' method on reader [{}]",
                getReader().getName());
          }
          // re-init loop value to true
          loop.set(true);
          while (loop.get()) {
            if (!getReader().isCardPresentPing()) {
              if (logger.isTraceEnabled()) {
                logger.trace("Card stop responding");
              }
              break;
            }
            // wait a bit
            try {
              Thread.sleep(sleepDurationMillis);
            } catch (InterruptedException ignored) {
              // Restore interrupted state...
              Thread.currentThread().interrupt();
              loop.set(false);
            }
          }
          if (logger.isTraceEnabled()) {
            logger.trace("Monitoring job polling process stopped");
          }
        } catch (RuntimeException e) {
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
        } finally {
          monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
        }
      }
    };
  }

  /**
   * Terminates the monitoring process.
   *
   * @since 2.0.0
   */
  @Override
  void stop() {
    loop.set(false);
  }
}
