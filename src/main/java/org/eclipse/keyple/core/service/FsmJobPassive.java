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

import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect the card removal thanks to the method {@link
 * CardRemovalWaiterBlockingSpi#waitForCardRemoval()} or {@link
 * CardPresenceMonitorBlockingSpi#monitorCardPresenceDuringProcessing()} depending on the provided
 * SPI.
 *
 * <p>This method is invoked in another thread
 *
 * <p>This job should be used by readers who have the ability to natively detect the disappearance
 * of the card during a communication session with an ES (between two APDU exchanges).
 *
 * <p>PC/SC readers have this capability.
 *
 * <p>If the card is removed during processing, then an internal CARD_REMOVED event is triggered.
 *
 * <p>If a communication problem with the reader occurs (KeypleReaderIOException) an internal
 * STOP_DETECT event is fired.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0.0
 */
final class FsmJobPassive implements FsmJob {

  private static final Logger logger = LoggerFactory.getLogger(FsmJobPassive.class);

  private static final String JOB_ID = "PASSIVE";
  private FsmState fsmState;
  private FsmState.State state;
  private ObservableReaderSpi readerSpi;

  /**
   * Constructor.
   *
   * @since 2.0.0
   */
  FsmJobPassive() {}

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
       * Monitoring loop
       *
       * <p>Waits for the removal of the card until no card is absent. <br>
       * Triggers a CARD_REMOVED event and exits when the card is no longer present.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
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
          switch (state) {
            case WAIT_FOR_CARD_INSERTION:
              ((CardInsertionWaiterBlockingSpi) readerSpi).waitForCardInsertion();
              if (logger.isTraceEnabled()) {
                logger.trace(
                    "[fsmJob={}, fsmService={}] Card detected", JOB_ID, fsmState.getFsmServiceId());
              }
              fsmState.onTrigger(FsmService.Trigger.CARD_INSERTED);
              return;
            case WAIT_FOR_CARD_PROCESSING:
              ((CardPresenceMonitorBlockingSpi) readerSpi).monitorCardPresenceDuringProcessing();
              if (logger.isTraceEnabled()) {
                logger.trace(
                    "[fsmJob={}, fsmService={}] Card removed", JOB_ID, fsmState.getFsmServiceId());
              }
              fsmState.onTrigger(FsmService.Trigger.CARD_REMOVED);
              return;
            case WAIT_FOR_CARD_REMOVAL:
              ((CardRemovalWaiterBlockingSpi) readerSpi).waitForCardRemoval();
              if (logger.isTraceEnabled()) {
                logger.trace(
                    "[fsmJob={}, fsmService={}] Card removed", JOB_ID, fsmState.getFsmServiceId());
              }
              fsmState.onTrigger(FsmService.Trigger.CARD_REMOVED);
              return;
            default:
          }
        } catch (TaskCanceledException e) {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsmJob={}, fsmService={}] Monitoring job stopped [reason={}]",
                JOB_ID,
                fsmState.getFsmServiceId(),
                e.getMessage());
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
          "[fsmJob={}, fsmService={}] Monitoring job stop requested [state={}]",
          JOB_ID,
          fsmState.getFsmServiceId(),
          state);
    }
    switch (state) {
      case WAIT_FOR_CARD_INSERTION:
        ((CardInsertionWaiterBlockingSpi) readerSpi).stopWaitForCardInsertion();
        break;
      case WAIT_FOR_CARD_PROCESSING:
        ((CardPresenceMonitorBlockingSpi) readerSpi).stopCardPresenceMonitoringDuringProcessing();
        break;
      case WAIT_FOR_CARD_REMOVAL:
        ((CardRemovalWaiterBlockingSpi) readerSpi).stopWaitForCardRemoval();
        break;
      default:
    }
  }
}
