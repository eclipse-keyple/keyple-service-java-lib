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
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect the card removal thanks to the method {@link
 * org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi#waitForCardAbsentNative()}.
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
class CardRemovalPassiveMonitoringJobAdapter extends AbstractMonitoringJob {

  private static final Logger logger =
      LoggerFactory.getLogger(CardRemovalPassiveMonitoringJobAdapter.class);

  private final WaitForCardRemovalBlockingSpi readerSpi;

  public CardRemovalPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
    this.readerSpi = (WaitForCardRemovalBlockingSpi) reader.getObservableReaderSpi();
  }

  /** (package-private)<br> */
  @Override
  Runnable getMonitoringJob(final AbstractObservableStateAdapter state) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          readerSpi.waitForCardAbsentNative();
          // timeout is already managed within the task
          state.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_REMOVED);
        } catch (RuntimeException e) {
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
        } catch (TaskCanceledException e) {
          e.printStackTrace();
        } catch (ReaderIOException e) {
          e.printStackTrace();
        }
      }
    };
  }

  /** (package-private)<br> */
  @Override
  void stop() {
    readerSpi.stopWaitForCardRemoval();
  }
}
