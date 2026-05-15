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
 * Monitoring job that delegates card detection to a blocking SPI call, allowing the thread to
 * remain idle until the hardware signals an event.
 *
 * <p>This strategy is used for readers that implement one of the following blocking SPIs:
 *
 * <ul>
 *   <li>{@link CardInsertionWaiterBlockingSpi} — for card insertion detection;
 *   <li>{@link CardPresenceMonitorBlockingSpi} — for card presence monitoring during processing;
 *   <li>{@link CardRemovalWaiterBlockingSpi} — for card removal detection.
 * </ul>
 *
 * <p>PC/SC readers typically have this capability, as they can natively detect card
 * insertion/removal events without polling.
 *
 * <p>The blocking SPI call runs in a dedicated thread. When the call returns normally, the
 * appropriate {@link FsmService.Trigger} ({@link FsmService.Trigger#CARD_INSERTED} or {@link
 * FsmService.Trigger#CARD_REMOVED}) is fired. If the call is cancelled (throws {@link
 * org.eclipse.keyple.core.plugin.TaskCanceledException}), the job exits silently. Any other
 * exception is forwarded to the application through the configured observation exception handler.
 *
 * @since 2.0.0
 */
final class FsmJobPassive implements FsmJob {

  private static final Logger logger = LoggerFactory.getLogger(FsmJobPassive.class);

  private static final String JOB_ID = "PASSIVE";
  private FsmState state;
  private FsmState.StateId stateId;
  private ObservableReaderSpi readerSpi;

  FsmJobPassive() {}

  /**
   * {@inheritDoc}
   *
   * @since 4.0.0
   */
  @Override
  public void initialize(FsmState state, ObservableReaderSpi readerSpi) {
    this.state = state;
    this.stateId = state.getStateId();
    this.readerSpi = readerSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Runnable getTask() {
    return new Runnable() {
      /**
       * Invokes the appropriate blocking SPI method for the current state, then dispatches the
       * corresponding {@link FsmService.Trigger} via {@link FsmState#fire(FsmService.Trigger)} when
       * the blocking call returns.
       *
       * <p>Triggers are dispatched through {@link FsmState#fire(FsmService.Trigger)} rather than
       * {@link FsmState#onTrigger(FsmService.Trigger)} directly, so that they are serialized
       * through the FSM service's synchronized dispatch method and cannot race with triggers
       * originating from external threads.
       *
       * <p>A {@link org.eclipse.keyple.core.plugin.TaskCanceledException} causes the job to exit
       * silently. Any other {@link ReaderIOException} or {@link RuntimeException} is forwarded to
       * the application through the configured observation exception handler.
       */
      @Override
      public void run() {
        try {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsm={}] Monitoring job started [job={}, state={}]",
                state.getServiceId(),
                JOB_ID,
                stateId);
          }
          switch (stateId) {
            case WAIT_FOR_CARD_INSERTION:
              ((CardInsertionWaiterBlockingSpi) readerSpi).waitForCardInsertion();
              if (logger.isTraceEnabled()) {
                logger.trace("[fsm={}] Card detected [job={}]", state.getServiceId(), JOB_ID);
              }
              state.fire(FsmService.Trigger.CARD_INSERTED);
              return;
            case WAIT_FOR_CARD_PROCESSING:
              ((CardPresenceMonitorBlockingSpi) readerSpi).monitorCardPresenceDuringProcessing();
              if (logger.isTraceEnabled()) {
                logger.trace("[fsm={}] Card removed [job={}]", state.getServiceId(), JOB_ID);
              }
              state.fire(FsmService.Trigger.CARD_REMOVED);
              return;
            case WAIT_FOR_CARD_REMOVAL:
              ((CardRemovalWaiterBlockingSpi) readerSpi).waitForCardRemoval();
              if (logger.isTraceEnabled()) {
                logger.trace("[fsm={}] Card removed [job={}]", state.getServiceId(), JOB_ID);
              }
              state.fire(FsmService.Trigger.CARD_REMOVED);
              return;
            default:
          }
        } catch (TaskCanceledException e) {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsm={}] Monitoring job stopped [job={}, reason={}]",
                state.getServiceId(),
                JOB_ID,
                e.getMessage());
          }
        } catch (ReaderIOException | RuntimeException e) {
          logger.warn(
              "[fsm={}] Monitoring job failure [job={}, reason={}]",
              state.getServiceId(),
              JOB_ID,
              e.toString());
          state.onError(e);
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
          "[fsm={}] Stop requested [job={}, state={}]", state.getServiceId(), JOB_ID, stateId);
    }
    switch (stateId) {
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
