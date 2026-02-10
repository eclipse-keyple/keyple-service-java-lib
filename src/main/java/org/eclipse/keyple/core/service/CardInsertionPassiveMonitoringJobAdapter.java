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
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect the card insertion thanks to the method {@link
 * CardInsertionWaiterBlockingSpi#waitForCardInsertion()} or {@link
 * WaitForCardInsertionBlockingSpi#waitForCardInsertion()}.
 *
 * <p>This method is invoked in another thread.
 *
 * <p>The job waits indefinitely for the waitForCardInsertion method to return unless the {@link
 * #stop()} method is invoked. In this case, the job is aborted.
 *
 * <p>When a card is present, an internal CARD_INSERTED event is fired.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0.0
 */
final class CardInsertionPassiveMonitoringJobAdapter extends AbstractMonitoringJobAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(CardInsertionPassiveMonitoringJobAdapter.class);

  private static final String JOB_ID = "InsertionPassive";

  private final ObservableReaderSpi readerSpi;

  /**
   * Constructor.
   *
   * @param reader The reader.
   * @since 2.0.0
   */
  CardInsertionPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
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
       * <p>Waits for the presence of a card until no card is present. <br>
       * Triggers a CARD_INSERTED event and exits as soon as a communication with a card is
       * established.
       *
       * <p>Any exceptions are notified to the application using the exception handler.
       */
      @Override
      public void run() {
        try {
          if (logger.isTraceEnabled()) {
            logger.trace(
                "[fsmJob={}, reader={}] Starting monitoring job process",
                JOB_ID,
                getReader().getName());
          }
          if (readerSpi instanceof CardInsertionWaiterBlockingSpi) {
            ((CardInsertionWaiterBlockingSpi) readerSpi).waitForCardInsertion();
          } else if (readerSpi instanceof WaitForCardInsertionBlockingSpi) {
            ((WaitForCardInsertionBlockingSpi) readerSpi).waitForCardInsertion();
          }
          monitoringState.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_INSERTED);
        } catch (ReaderIOException e) {
          // just warn as it can be a disconnection of the reader.
          logger.warn(
              "[fsmJob={}, reader={}] Failed to process card insertion event [reason={}]",
              JOB_ID,
              getReader().getName(),
              e.getMessage());
        } catch (TaskCanceledException e) {
          logger.warn(
              "[fsmJob={}, reader={}] Monitoring job process cancelled [reason={}]",
              JOB_ID,
              getReader().getName(),
              e.getMessage());
        } catch (RuntimeException e) {
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
      logger.trace(
          "[fsmJob={}, reader={}] Stopping monitoring job process", JOB_ID, getReader().getName());
    }
    if (readerSpi instanceof CardInsertionWaiterBlockingSpi) {
      ((CardInsertionWaiterBlockingSpi) readerSpi).stopWaitForCardInsertion();
    } else if (readerSpi instanceof WaitForCardInsertionBlockingSpi) {
      ((WaitForCardInsertionBlockingSpi) readerSpi).stopWaitForCardInsertion();
    }
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[fsmJob={}, reader={}] Monitoring job process stopped", JOB_ID, getReader().getName());
    }
  }
}
