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
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This monitoring job polls the {@link CardReader#isCardPresent()} method to detect a card
 * insertion or a card removal.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the {@link
 * org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi} mechanism.
 *
 * @since 2.0.0
 */
final class CardInsertionActiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardInsertionActiveMonitoringJobAdapter.class);

  private final long sleepDurationMillis;
  private final boolean monitorInsertion;
  private final CardReader reader;
  private final AtomicBoolean loop = new AtomicBoolean();

  /**
   * Build a monitoring job to detect the card insertion
   *
   * @param reader reader that will be polled with the method isCardPresent()
   * @param sleepDurationMillis time interval between two presence polls.
   * @param monitorInsertion if true, polls for CARD_INSERTED, else CARD_REMOVED
   * @since 2.0.0
   */
  public CardInsertionActiveMonitoringJobAdapter(
      ObservableLocalReaderAdapter reader, long sleepDurationMillis, boolean monitorInsertion) {
    super(reader);
    this.sleepDurationMillis = sleepDurationMillis;
    this.reader = reader;
    this.monitorInsertion = monitorInsertion;
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
      long retries = 0;

      /**
       * Monitoring loop
       *
       * <p>Polls for the presence of a card and loops until no card responds. <br>
       * Triggers a CARD_INSERTED event and exits as soon as a communication with a card is
       * established.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
       */
      @Override
      public void run() {
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}] Polling from isCardPresent", reader.getName());
          }
          // re-init loop value to true
          loop.set(true);
          while (loop.get()) {
            // polls for CARD_INSERTED
            if (monitorInsertion && reader.isCardPresent()) {
              if (logger.isDebugEnabled()) {
                logger.debug("[{}] The card is present ", reader.getName());
              }
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_INSERTED);
              return;
            }
            // polls for CARD_REMOVED
            if (!monitorInsertion && !reader.isCardPresent()) {
              if (logger.isDebugEnabled()) {
                logger.debug("[{}] The card is not present ", reader.getName());
              }
              loop.set(false);
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
              return;
            }
            retries++;

            if (logger.isTraceEnabled()) {
              logger.trace("[{}] isCardPresent polling retries : {}", reader.getName(), retries);
            }
            try {
              // wait a bit
              Thread.sleep(sleepDurationMillis);
            } catch (InterruptedException ignored) {
              // Restore interrupted state...
              Thread.currentThread().interrupt();
              loop.set(false);
            }
          }
          if (logger.isTraceEnabled()) {
            logger.trace("[{}] Looping has been stopped", reader.getName());
          }
        } catch (RuntimeException e) {
          ((ObservableLocalReaderAdapter) reader)
              .getObservationExceptionHandler()
              .onReaderObservationError(
                  ((ObservableLocalReaderAdapter) reader).getPluginName(), reader.getName(), e);
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
    if (logger.isDebugEnabled()) {
      logger.debug("[{}] Stop polling ", reader.getName());
    }
    loop.set(false);
  }
}
