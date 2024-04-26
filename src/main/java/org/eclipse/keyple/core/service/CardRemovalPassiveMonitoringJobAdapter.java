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
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.WaitForCardRemovalDuringProcessingBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect the card removal thanks to the method {@link
 * CardRemovalWaiterBlockingSpi#waitForCardRemoval()} or {@link
 * WaitForCardRemovalBlockingSpi#waitForCardRemoval()} or {@link
 * CardPresenceMonitorBlockingSpi#monitorCardPresenceDuringProcessing()} or {@link
 * WaitForCardRemovalDuringProcessingBlockingSpi#waitForCardRemovalDuringProcessing()} depending on
 * the provided SPI.
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
final class CardRemovalPassiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardRemovalPassiveMonitoringJobAdapter.class);

  private final ObservableReaderSpi readerSpi;

  /**
   * Constructor.
   *
   * @param reader reference to the reader
   * @since 2.0.0
   */
  public CardRemovalPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
    readerSpi = reader.getObservableReaderSpi();
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
       * <p>Waits for the removal of the card until no card is absent. <br>
       * Triggers a CARD_REMOVED event and exits when the card is no longer present.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
       */
      @Override
      public void run() {
        try {
          if (readerSpi instanceof CardRemovalWaiterBlockingSpi) {
            ((CardRemovalWaiterBlockingSpi) readerSpi).waitForCardRemoval();
          } else if (readerSpi instanceof WaitForCardRemovalBlockingSpi) {
            ((WaitForCardRemovalBlockingSpi) readerSpi).waitForCardRemoval();
          } else if (readerSpi instanceof CardPresenceMonitorBlockingSpi) {
            ((CardPresenceMonitorBlockingSpi) readerSpi).monitorCardPresenceDuringProcessing();
          } else if (readerSpi instanceof WaitForCardRemovalDuringProcessingBlockingSpi) {
            ((WaitForCardRemovalDuringProcessingBlockingSpi) readerSpi)
                .waitForCardRemovalDuringProcessing();
          }
        } catch (ReaderIOException e) {
          // just warn as it can be a disconnection of the reader.
          logger.warn(
              "Monitoring job error while processing card removal event on reader [{}]: {}",
              getReader().getName(),
              e.getMessage());
        } catch (TaskCanceledException e) {
          logger.warn("Monitoring job process cancelled: {}", e.getMessage());
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
    if (logger.isTraceEnabled()) {
      logger.trace("Stop monitoring job process");
    }
    if (readerSpi instanceof CardRemovalWaiterBlockingSpi) {
      ((CardRemovalWaiterBlockingSpi) readerSpi).stopWaitForCardRemoval();
    } else if (readerSpi instanceof WaitForCardRemovalBlockingSpi) {
      ((WaitForCardRemovalBlockingSpi) readerSpi).stopWaitForCardRemoval();
    } else if (readerSpi instanceof CardPresenceMonitorBlockingSpi) {
      ((CardPresenceMonitorBlockingSpi) readerSpi).stopCardPresenceMonitoringDuringProcessing();
    } else if (readerSpi instanceof WaitForCardRemovalDuringProcessingBlockingSpi) {
      ((WaitForCardRemovalDuringProcessingBlockingSpi) readerSpi)
          .stopWaitForCardRemovalDuringProcessing();
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Monitoring job process stopped");
    }
  }
}
