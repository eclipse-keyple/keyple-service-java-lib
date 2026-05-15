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
final class FsmJobActive implements FsmJob {

  private static final Logger logger = LoggerFactory.getLogger(FsmJobActive.class);

  private static final String JOB_ID = "ACTIVE";

  private final long sleepDurationMillis;
  private FsmState fsmState;
  private FsmState.State state;
  private ObservableReaderSpi readerSpi;
  private final AtomicBoolean loop = new AtomicBoolean();

  /**
   * Build a monitoring job to detect a card insertion or a card removal.
   *
   * @param sleepDurationMillis time interval between two presence polls.
   * @since 2.0.0
   */
  FsmJobActive(long sleepDurationMillis) {
    this.sleepDurationMillis = sleepDurationMillis;
  }

  /**
   * {@inheritDoc}
   *
   * @since 4.0.0
   */
  @Override
  public void init(FsmState fsmState, ObservableReaderSpi readerSpi) {
    this.fsmState = fsmState;
    this.state = fsmState.getState();
    this.readerSpi = readerSpi;
  }

  /**
   * Gets the monitoring process.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  @Override
  public Runnable getRunnableTask() {
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
                "[fsmJob={}, fsmService={}] Monitoring job started [state={}]",
                JOB_ID,
                fsmState.getFsmServiceId(),
                state);
          }
          // re-init loop value to true
          loop.set(true);
          while (loop.get()) {
            // polls for CARD_INSERTED
            if (state == FsmState.State.WAIT_FOR_CARD_INSERTION && readerSpi.isCardPresent()) {
              if (logger.isTraceEnabled()) {
                logger.trace(
                    "[fsmJob={}, fsmService={}] Card detected", JOB_ID, fsmState.getFsmServiceId());
              }
              fsmState.onTrigger(FsmService.Trigger.CARD_INSERTED);
              return;
            }
            // polls for CARD_REMOVED
            if (state == FsmState.State.WAIT_FOR_CARD_REMOVAL && !readerSpi.isCardPresent()) {
              if (logger.isTraceEnabled()) {
                logger.trace(
                    "[fsmJob={}, fsmService={}] Card removed", JOB_ID, fsmState.getFsmServiceId());
              }
              fsmState.onTrigger(FsmService.Trigger.CARD_REMOVED);
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
                "[fsmJob={}, fsmService={}] Monitoring job stopped",
                JOB_ID,
                fsmState.getFsmServiceId());
          }
        } catch (ReaderIOException | RuntimeException e) {
          logger.warn(
              "[fsmJob={}, fsmService={}] Monitoring job failure [reason={}]",
              JOB_ID,
              fsmState.getFsmServiceId(),
              e.getMessage());
          fsmState.onError(e);
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
  public void stop() {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmJob={}, fsmService={}] Stopping monitoring job [state={}]",
          JOB_ID,
          fsmState.getFsmServiceId(),
          state);
    }
    loop.set(false);
  }
}
