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

import org.eclipse.keyple.core.commons.KeypleDefaultSelectionsRequest;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;

/**
 * Provides the API to observe cards insertion/removal.
 *
 * <ul>
 *   <li>Observers management
 *   <li>Starting/stopping card detection
 *   <li>Managing the default selection
 *   <li>Definition of polling and notification modes
 * </ul>
 *
 * @since 2.0
 */
public interface ObservableReader extends Reader {

  /**
   * Indicates the expected behavior when processing a default selection.
   *
   * @since 2.0
   */
  enum NotificationMode {

    /**
     * All cards presented to readers are notified regardless of the result of the default
     * selection.
     *
     * @since 2.0
     */
    ALWAYS,
    /**
     * Only cards that have been successfully selected (logical channel open) will be notified. The
     * others will be ignored and the application will not be aware of them.
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
   * Register a new reader observer to be notified when a reader event occurs.
   *
   * <p>The provided observer will receive all the events produced by this reader (card insertion,
   * removal, etc.)
   *
   * <p>It is possible to add as many observers as necessary. They will be notified of events
   * <b>sequentially</b> in the order in which they are added.
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
   * <p>The {@link PollingMode} indicates the action to be followed after processing the card: if
   * {@link PollingMode#REPEATING}, the card detection is restarted, if {@link
   * PollingMode#SINGLESHOT}, the card detection is stopped until a new call to startCardDetection
   * is made
   *
   * @param pollingMode The polling mode to use (should be not null).
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
   * Defines the default selection request to be processed when a card is inserted.
   *
   * <p>Depending on the card and the notificationMode parameter, a {@link
   * ReaderEvent.EventType#CARD_INSERTED EventType#CARD_INSERTED}, {@link
   * ReaderEvent.EventType#CARD_MATCHED EventType#CARD_MATCHED} or no event at all will be notified
   * to the application observers.
   *
   * @param defaultSelectionsRequest The default selection request to be operated (should be not
   *     null).
   * @param notificationMode The notification mode to use (should be not null).
   * @since 2.0
   */
  void setDefaultSelectionRequest(
      KeypleDefaultSelectionsRequest defaultSelectionsRequest, NotificationMode notificationMode);

  /**
   * Defines the default selection request and starts the card detection using the provided polling
   * mode.
   *
   * <p>The notification mode indicates whether a {@link ReaderEvent.EventType#CARD_INSERTED} event
   * should be notified even if the selection has failed ({@link NotificationMode#ALWAYS}) or
   * whether the card insertion should be ignored in this case ({@link
   * NotificationMode#MATCHED_ONLY}).
   *
   * <p>The polling mode indicates the action to be followed after processing the card: if {@link
   * PollingMode#REPEATING}, the card detection is restarted, if {@link PollingMode#SINGLESHOT}, the
   * card detection is stopped until a new call to * startCardDetection is made.
   *
   * @param defaultSelectionsRequest The default selection request to be operated.
   * @param notificationMode The notification mode to use (should be not null).
   * @param pollingMode The polling mode to use (should be not null).
   * @since 2.0
   */
  void setDefaultSelectionRequest(
      KeypleDefaultSelectionsRequest defaultSelectionsRequest,
      NotificationMode notificationMode,
      PollingMode pollingMode);

  /**
   * Terminates the processing of the card, in particular after an interruption by exception<br>
   * Do nothing if the channel is already closed.<br>
   * Channel closing is nominally managed the last transmission with the card. However, there are
   * cases where exchanges with the card are interrupted by an exception, in which case it is
   * necessary to explicitly close the channel using this method.
   *
   * @since 2.0
   */
  void finalizeCardProcessing();
}
