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
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
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
final class CardPresenceActiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardPresenceActiveMonitoringJobAdapter.class);

  private static final String JOB_ID = "ACTIVE_MONITOR";

  private final long sleepDurationMillis;
  private final AbstractObservableStateAdapter.MonitoringState state;
  private final AtomicBoolean loop = new AtomicBoolean();
  private final ObservableReaderSpi readerSpi;

  /**
   * Build a monitoring job to detect a card insertion or a card removal.
   *
   * @param reader reader that will be polled with the method isCardPresent()
   * @param sleepDurationMillis time interval between two presence polls.
   * @param state the associated monitoring state
   * @since 2.0.0
   */
  public CardPresenceActiveMonitoringJobAdapter(
      ObservableLocalReaderAdapter reader,
      long sleepDurationMillis,
      AbstractObservableStateAdapter.MonitoringState state) {
    super(reader);
    this.sleepDurationMillis = sleepDurationMillis;
    readerSpi = reader.getObservableReaderSpi();
    this.state = state;
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
       * Executes the monitoring loop for the card reader.
       *
       * <p>The method continuously polls the card reader to detect card insertion or removal
       * events. If a card is detected or removed based on the current monitoring state, the
       * respective event is triggered, and the monitoring loop exits.
       *
       * <p>Exceptions: - Handles {@link ReaderIOException} and {@link RuntimeException}, notifying
       * the application through the configured exception handler.
       */
      @Override
      public void run() {
        try {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsmJob={}, reader={}] Monitoring job started", JOB_ID, getReader().getName());
          }
          // re-init loop value to true
          loop.set(true);
          while (loop.get()) {
            // polls for CARD_INSERTED
            if (state == AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION
                && readerSpi.isCardPresent()) {
              if (logger.isTraceEnabled()) {
                logger.trace("[fsmJob={}, reader={}] Card detected", JOB_ID, getReader().getName());
              }
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_INSERTED);
              return;
            }
            // polls for CARD_REMOVED
            if (state == AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL
                && !readerSpi.isCardPresent()) {
              if (logger.isTraceEnabled()) {
                logger.trace("[fsmJob={}, reader={}] Card removed", JOB_ID, getReader().getName());
              }
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
              return;
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
            logger.trace(
                "[fsmJob={}, reader={}] Monitoring job stopped", JOB_ID, getReader().getName());
          }
        } catch (ReaderIOException | RuntimeException e) {
          logger.warn(
              "[fsmJob={}, reader={}] Monitoring job failure [reason={}]",
              JOB_ID,
              getReader().getName(),
              e.getMessage());
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
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
    if (logger.isTraceEnabled()) {
      logger.trace("[fsmJob={}, reader={}] Stopping monitoring job", JOB_ID, getReader().getName());
    }
    loop.set(false);
  }
}
