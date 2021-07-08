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
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.WaitForCardRemovalBlockingDuringProcessingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Detect the card removal thanks to the method {@link
 * WaitForCardRemovalBlockingSpi#waitForCardRemoval()} or {@link
 * WaitForCardRemovalBlockingDuringProcessingSpi#waitForCardRemovalDuringProcessing()} depending of
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
 * @since 2.0
 */
final class CardRemovalPassiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardRemovalPassiveMonitoringJobAdapter.class);

  private final WaitForCardRemovalBlockingSpi readerSpi;
  private final WaitForCardRemovalBlockingDuringProcessingSpi readerProcessingSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param reader reference to the reader
   * @since 2.0
   */
  public CardRemovalPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
    if (reader.getObservableReaderSpi() instanceof WaitForCardRemovalBlockingSpi) {
      this.readerSpi = (WaitForCardRemovalBlockingSpi) reader.getObservableReaderSpi();
      this.readerProcessingSpi = null;
    } else {
      this.readerSpi = null;
      this.readerProcessingSpi =
          (WaitForCardRemovalBlockingDuringProcessingSpi) reader.getObservableReaderSpi();
    }
  }

  /**
   * (package-private)<br>
   * Gets the monitoring process.
   *
   * @return A not null reference.
   * @since 2.0
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
          while (!Thread.currentThread().isInterrupted()) {
            try {
              if (readerSpi != null) {
                readerSpi.waitForCardRemoval();
              } else {
                readerProcessingSpi.waitForCardRemovalDuringProcessing();
              }
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
              break;
            } catch (ReaderIOException e) {
              // just warn as it can be a disconnection of the reader.
              logger.warn(
                  "[{}] waitForCardAbsentNative => Error while processing card removal event",
                  getReader().getName());
              break;
            } catch (TaskCanceledException e) {
              break;
            }
          }
        } catch (RuntimeException e) {
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
        }
      }
    };
  }

  /**
   * (package-private)<br>
   * Terminates the monitoring process.
   *
   * @since 2.0
   */
  @Override
  void stop() {
    if (readerSpi != null) {
      readerSpi.stopWaitForCardRemoval();
    } else {
      readerProcessingSpi.stopWaitForCardRemovalDuringProcessing();
    }
  }
}
