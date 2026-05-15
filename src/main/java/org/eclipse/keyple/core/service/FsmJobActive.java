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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitoring job that detects card insertion or removal by repeatedly polling {@link
 * org.eclipse.keypop.reader.CardReader#isCardPresent()}.
 *
 * <p>This strategy is used for readers that implement {@link
 * org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterNonBlockingSpi}
 * or {@link
 * org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterNonBlockingSpi}.
 * The poll interval is provided at construction time by the corresponding SPI method.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and forwarded
 * to the application through the {@link
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
   * Creates a polling monitoring job.
   *
   * @param sleepDurationMillis The time interval in milliseconds between two consecutive {@code
   *     isCardPresent()} polls; must be positive.
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
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Runnable getRunnableTask() {
    return new Runnable() {
      /**
       * Runs the polling loop until a card insertion or removal is detected, or until {@link
       * #stop()} is called.
       *
       * <p>In state {@link FsmState.State#WAIT_FOR_CARD_INSERTION}, the loop fires {@link
       * FsmService.Trigger#CARD_INSERTED} as soon as {@code isCardPresent()} returns {@code true}.
       * In state {@link FsmState.State#WAIT_FOR_CARD_REMOVAL}, the loop fires {@link
       * FsmService.Trigger#CARD_REMOVED} as soon as {@code isCardPresent()} returns {@code false}.
       *
       * <p>{@link ReaderIOException} and {@link RuntimeException} are caught and forwarded to the
       * application through the configured observation exception handler.
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
   * {@inheritDoc}
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
