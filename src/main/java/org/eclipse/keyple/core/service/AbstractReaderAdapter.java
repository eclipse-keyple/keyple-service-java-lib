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

import java.util.List;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for all readers.
 *
 * @since 2.0.0
 */
abstract class AbstractReaderAdapter implements CardReader, ProxyReaderApi {

  private static final Logger logger = LoggerFactory.getLogger(AbstractReaderAdapter.class);

  private final String readerName;
  private final KeypleReaderExtension readerExtension;
  private final String pluginName;

  private boolean isRegistered;
  private long before;

  /**
   * Constructor.
   *
   * @param readerName The name of the reader.
   * @param readerExtension The associated reader extension SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0.0
   */
  AbstractReaderAdapter(
      String readerName, KeypleReaderExtension readerExtension, String pluginName) {
    this.readerName = readerName;
    this.readerExtension = readerExtension;
    this.pluginName = pluginName;
  }

  /**
   * Gets the plugin name.
   *
   * @return A not empty String.
   * @since 2.0.0
   */
  final String getPluginName() {
    return pluginName;
  }

  /**
   * Performs a selection scenario following a card detection.
   *
   * <p>Each scenario selection case consists in checking if the card matches the profile defined in
   * the {@link CardSelectionRequestSpi} and possibly sending the optional commands provided.<br>
   * The cases are processed sequentially in the order in which they are found in the list.<br>
   * Processing continues or stops after the first successful selection according to the policy
   * defined by {@link MultiSelectionProcessing}.<br>
   * At the end of the treatment of each case, the channel is left open or is closed according to
   * the channel control policy.
   *
   * @param cardSelectors A list of {@link CardSelector}.
   * @param cardSelectionRequests A list of selection cases composed of one or more {@link
   *     CardSelectionRequestSpi}.
   * @param multiSelectionProcessing The multi selection policy.
   * @param channelControl The channel control policy.
   * @return An empty list if no response was received.
   * @throws ReaderBrokenCommunicationException if the communication with the reader has failed.
   * @throws CardBrokenCommunicationException if the communication with the card has failed.
   * @since 2.0.0
   */
  final List<CardSelectionResponseApi> transmitCardSelectionRequests(
      List<CardSelector<?>> cardSelectors,
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException {

    checkStatus();

    List<CardSelectionResponseApi> cardSelectionResponses = null;

    if (logger.isTraceEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.trace(
          "[{}] transmit => {}, elapsed {} ms.",
          this.getName(),
          cardSelectionRequests,
          elapsed10ms / 10.0);
    }

    try {
      cardSelectionResponses =
          processCardSelectionRequests(
              cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl);
    } catch (UnexpectedStatusWordException e) {
      throw new CardBrokenCommunicationException(
          e.getCardResponse(), false, "An unexpected status word was received.", e);
    } finally {
      if (logger.isTraceEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.trace(
            "[{}] received => {}, elapsed {} ms.",
            this.getName(),
            cardSelectionResponses,
            elapsed10ms / 10.0);
      }
    }

    return cardSelectionResponses;
  }

  /**
   * Check if the reader status is "registered".
   *
   * @throws IllegalStateException is thrown when reader is not or no longer registered.
   * @since 2.0.0
   */
  final void checkStatus() {
    if (!isRegistered)
      throw new IllegalStateException(
          String.format("This reader, %s, is not registered", getName()));
  }

  /**
   * Changes the reader status to registered.
   *
   * @since 2.0.0
   */
  final void register() {
    isRegistered = true;
  }

  /**
   * Changes the reader status to unregistered if is not already unregistered.
   *
   * <p>This method may be overridden in order to meet specific needs in certain implementations of
   * readers.
   *
   * @since 2.0.0
   */
  void unregister() {
    isRegistered = false;
  }

  /**
   * Abstract method performing the actual card selection process.
   *
   * @param cardSelectors A list of {@link CardSelector}.
   * @param cardSelectionRequests A list of selection cases composed of one or more {@link
   *     CardSelectionRequestSpi}.
   * @param multiSelectionProcessing The multi selection policy.
   * @param channelControl The channel control policy.
   * @return A not empty list containing at most as many responses as there are selection cases.
   * @throws ReaderBrokenCommunicationException if the communication with the reader has failed.
   * @throws CardBrokenCommunicationException if the communication with the card has failed.
   * @throws UnexpectedStatusWordException If status word verification is enabled in the card
   *     request and the card returned an unexpected code.
   * @since 2.0.0
   */
  abstract List<CardSelectionResponseApi> processCardSelectionRequests(
      List<CardSelector<?>> cardSelectors,
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderBrokenCommunicationException,
          CardBrokenCommunicationException,
          UnexpectedStatusWordException;

  /**
   * Abstract method performing the actual transmission of the card request.
   *
   * @param cardRequest The card request.
   * @param channelControl The channel control policy to apply.
   * @return A not null reference.
   * @throws ReaderBrokenCommunicationException if the communication with the reader has failed.
   * @throws CardBrokenCommunicationException if the communication with the card has failed.
   * @throws UnexpectedStatusWordException If status word verification is enabled in the card
   *     request and the card returned an unexpected code.
   * @since 2.0.0
   */
  abstract CardResponseApi processCardRequest(
      CardRequestSpi cardRequest, ChannelControl channelControl)
      throws ReaderBrokenCommunicationException,
          CardBrokenCommunicationException,
          UnexpectedStatusWordException;

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final String getName() {
    return readerName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  public final <T extends KeypleReaderExtension> T getExtension(Class<T> readerExtensionClass) {
    checkStatus();
    return (T) readerExtension;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final CardResponseApi transmitCardRequest(
      CardRequestSpi cardRequest, ChannelControl channelControl)
      throws ReaderBrokenCommunicationException,
          CardBrokenCommunicationException,
          UnexpectedStatusWordException {
    checkStatus();

    Assert.getInstance()
        .notNull(cardRequest, "cardRequest")
        .notNull(channelControl, "channelControl");

    CardResponseApi cardResponse = null;

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => {}, elapsed {} ms.", this.getName(), cardRequest, elapsed10ms / 10.0);
    }

    try {
      cardResponse = processCardRequest(cardRequest, channelControl);
    } finally {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] receive => {}, elapsed {} ms.", this.getName(), cardResponse, elapsed10ms / 10.0);
      }
    }

    return cardResponse;
  }
}
