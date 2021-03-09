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

import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;

/**
 * Reader able to observe the insertion/removal of cards.
 *
 * @since 2.0
 */
public interface ObservableReader extends Reader {

  /**
   * Indicates the desired notification conditions when processing a card selection.
   *
   * @since 2.0
   */
  enum NotificationMode {

    /**
     * All cards presented to readers are notified regardless of the result of the selection.
     *
     * @since 2.0
     */
    ALWAYS,
    /**
     * Only cards that have been successfully selected will be notified. The others will be ignored
     * and the application will not be aware of them.
     *
     * @since 2.0
     */
    MATCHED_ONLY
  }

  /**
   * Indicates the action to be taken after processing a card.
   *
   * @since 2.0
   */
  enum PollingMode {

    /**
     * Continue waiting for the insertion of a next card.
     *
     * @since 2.0
     */
    REPEATING,
    /**
     * Stop and wait for a restart signal.
     *
     * @since 2.0
     */
    SINGLESHOT
  }

  /**
   * Register a new observer to be notified when a reader event occurs.
   *
   * <p>The provided observer must implement the {@link ReaderObserverSpi} interface to be able to
   * receive the events produced by this reader (card insertion, removal, etc.)
   *
   * @param observer An observer object implementing the required interface (should be not null).
   * @since 2.0
   */
  void addObserver(final ReaderObserverSpi observer);

  /**
   * Unregister a reader observer.
   *
   * <p>The observer will no longer receive any of the events produced by this reader.
   *
   * @param observer The observer object to be removed (should be not null).
   * @since 2.0
   */
  void removeObserver(final ReaderObserverSpi observer);

  /**
   * Unregister all observers at once
   *
   * @since 2.0
   */
  void clearObservers();

  /**
   * Provides the current number of registered observers
   *
   * @return an int
   * @since 2.0
   */
  int countObservers();

  /**
   * Starts the card detection. Once activated, the application can be notified of the arrival of a
   * card.
   *
   * <p>The {@link PollingMode} indicates the action to be followed after processing the card.
   *
   * @param pollingMode The polling mode policy.
   * @since 2.0
   */
  void startCardDetection(PollingMode pollingMode);

  /**
   * Stops the card detection.
   *
   * @since 2.0
   */
  void stopCardDetection();

  /**
   * Terminates the card processing.
   *
   * <p>This method notifies the observation process that the processing of the card has been
   * completed in order to ensure that the card monitoring cycle runs properly.<br>
   * It is <b>mandatory</b> to invoke it when the physical communication channel with the card could
   * not be closed. <br>
   * This method will do nothing if the channel has already been closed.<br>
   * The channel closing is nominally managed during the last transmission with the card. However,
   * there are cases where exchanges with the card are interrupted by an exception, in which case it
   * is necessary to explicitly close the channel using this method.
   *
   * @since 2.0
   */
  void finalizeCardProcessing();

  /**
   * Configures the reader to use a custom thread pool for events notification.
   *
   * <p>The custom pool should be flexible enough to handle many concurrent tasks as each {@link
   * ReaderEvent} are executed asynchronously.
   *
   * <p>The use of this method is optional and depends on the needs of the application.<br>
   * When used, the event notification will always be done asynchronously. Otherwise, the
   * notification can be synchronous (local plugin) or asynchronous (remote plugin) depending on the
   * type of reader.
   *
   * @param eventNotificationExecutorService The executor service provided by the application.
   * @since 2.0
   */
  void setEventNotificationExecutorService(ExecutorService eventNotificationExecutorService);

  /**
   * Sets the exception handler.
   *
   * <p>The invocation of this method is <b>mandatory</b> when the reader has to be observed.
   *
   * <p>In case of a fatal error during the observation, the handler will receive a notification.
   *
   * @param exceptionHandler The exception handler implemented by the application.
   * @since 2.0
   */
  void setReaderObservationExceptionHandler(ReaderObservationExceptionHandlerSpi exceptionHandler);
}
