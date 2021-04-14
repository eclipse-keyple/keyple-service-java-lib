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

import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Detect the card insertion thanks to the method {@link
 * WaitForCardInsertionBlockingSpi#waitForCardPresent()}.
 *
 * <p>This method is invoked in another thread.
 *
 * <p>The job waits indefinitely for the waitForCardPresent method to return unless the {@link
 * #stop()} method is invoked. In this case, the job is aborted.
 *
 * <p>When a card is present, an internal CARD_INSERTED event is fired.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0
 */
final class CardInsertionPassiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardInsertionPassiveMonitoringJobAdapter.class);

  private final WaitForCardInsertionBlockingSpi readerSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param reader The reader.
   * @since 2.0
   */
  CardInsertionPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
    this.readerSpi = (WaitForCardInsertionBlockingSpi) reader.getObservableReaderSpi();
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
       * <p>Waits for the presence of a card until no card is present. <br>
       * Triggers a CARD_INSERTED event and exits as soon as a communication with a card is
       * established.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
       */
      @Override
      public void run() {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              readerSpi.waitForCardInsertion();
              monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_INSERTED);
              break;
            } catch (ReaderIOException e) {
              // TODO check this
              // just warn as it can be a disconnection of the reader.
              logger.warn(
                  "[{}] waitForCardPresent => Error while processing card insertion event",
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
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] stopWaitForCard on reader", getReader().getName());
    }
    readerSpi.stopWaitForCardInsertion();
  }
}
