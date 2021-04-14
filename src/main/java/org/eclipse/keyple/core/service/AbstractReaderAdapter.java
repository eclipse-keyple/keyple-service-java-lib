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

import java.util.List;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all readers.
 *
 * @since 2.0
 */
abstract class AbstractReaderAdapter implements Reader, ProxyReader {

  private static final Logger logger = LoggerFactory.getLogger(AbstractReaderAdapter.class);

  private final String readerName;
  private final Object readerExtension;
  private final String pluginName;

  private boolean isRegistered;
  private long before;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param readerName The name of the reader.
   * @param readerExtension The associated reader extension SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0
   */
  AbstractReaderAdapter(String readerName, Object readerExtension, String pluginName) {
    this.readerName = readerName;
    this.readerExtension = readerExtension;
    this.pluginName = pluginName;
  }

  /**
   * (package-private) <br>
   * Gets the plugin name.
   *
   * @return A not empty String.
   * @since 2.0
   */
  final String getPluginName() {
    return pluginName;
  }

  /**
   * (package-private)<br>
   * Performs a selection scenario following a card detection.
   *
   * <p>Each scenario selection case consists in checking if the card matches the profile defined in
   * the {@link CardSelectionRequest} and possibly sending the optional commands provided.<br>
   * The cases are processed sequentially in the order in which they are found in the list.<br>
   * Processing continues or stops after the first successful selection according to the policy
   * defined by {@link MultiSelectionProcessing}.<br>
   * At the end of the treatment of each case, the channel is left open or is closed according to
   * the channel control policy.
   *
   * @param cardSelectionRequests A list of selection cases composed of one or more {@link
   *     CardSelectionRequest}.
   * @param multiSelectionProcessing The multi selection policy.
   * @param channelControl The channel control policy.
   * @return An empty list if no response was received.
   * @throws ReaderCommunicationException if the communication with the reader has failed.
   * @throws CardCommunicationException if the communication with the card has failed.
   * @since 2.0
   */
  final List<KeypleCardSelectionResponse> transmitCardSelectionRequests(
      List<CardSelectionRequest> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException {

    checkStatus();

    List<KeypleCardSelectionResponse> cardSelectionResponses = null;

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => {}, elapsed {} ms.",
          this.getName(),
          cardSelectionRequests,
          elapsed10ms / 10.0);
    }

    try {
      cardSelectionResponses =
          processCardSelectionRequests(
              cardSelectionRequests, multiSelectionProcessing, channelControl);
    } catch (UnexpectedStatusCodeException e) {
      throw new CardCommunicationException(
          e.getCardResponse(), "An unexpected status code was received.", e);
    } finally {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] received => {}, elapsed {} ms.",
            this.getName(),
            cardSelectionResponses,
            elapsed10ms / 10.0);
      }
    }

    return cardSelectionResponses;
  }

  /**
   * (package-private)<br>
   * Check if the reader status is "registered".
   *
   * @throws IllegalStateException is thrown when reader is not or no longer registered.
   * @since 2.0
   */
  final void checkStatus() {
    if (!isRegistered)
      throw new IllegalStateException(
          String.format("This reader, %s, is not registered", getName()));
  }

  /**
   * (package-private)<br>
   * Changes the reader status to registered.
   *
   * @since 2.0
   */
  final void register() {
    isRegistered = true;
  }

  /**
   * (package-private)<br>
   * Changes the reader status to unregistered if is not already unregistered.
   *
   * <p>This method may be overridden in order to meet specific needs in certain implementations of
   * readers.
   *
   * @since 2.0
   */
  void unregister() {
    isRegistered = false;
  }

  /**
   * (package-private)<br>
   * Abstract method performing the actual card selection process.
   *
   * @param cardSelectionRequests A list of selection cases composed of one or more {@link
   *     CardSelectionRequest}.
   * @param multiSelectionProcessing The multi selection policy.
   * @param channelControl The channel control policy.
   * @return An empty list if no response was received.
   * @throws ReaderCommunicationException if the communication with the reader has failed.
   * @throws CardCommunicationException if the communication with the card has failed.
   * @throws UnexpectedStatusCodeException If status code verification is enabled in the card
   *     request and the card returned an unexpected code.
   * @since 2.0
   */
  abstract List<KeypleCardSelectionResponse> processCardSelectionRequests(
      List<CardSelectionRequest> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException,
          UnexpectedStatusCodeException;

  /**
   * (package-private)<br>
   * Abstract method performing the actual transmission of the card request.
   *
   * @param cardRequest The card request.
   * @param channelControl The channel control policy to apply.
   * @return A not null reference.
   * @throws ReaderCommunicationException if the communication with the reader has failed.
   * @throws CardCommunicationException if the communication with the card has failed.
   * @throws UnexpectedStatusCodeException If status code verification is enabled in the card
   *     request and the card returned an unexpected code.
   * @since 2.0
   */
  abstract CardResponse processCardRequest(CardRequest cardRequest, ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException,
          UnexpectedStatusCodeException;

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final String getName() {
    return readerName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final <T extends KeypleReaderExtension> T getExtension(Class<T> readerExtensionType) {
    checkStatus();
    return (T) readerExtension;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResponse transmitCardRequest(CardRequest cardRequest, ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException,
          UnexpectedStatusCodeException {
    checkStatus();

    CardResponse cardResponse;

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
