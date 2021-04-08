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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all Readers.
 *
 * @since 2.0
 */
abstract class AbstractReaderAdapter implements Reader, ProxyReader {

  private static final Logger logger = LoggerFactory.getLogger(AbstractReaderAdapter.class);

  private final String readerName;
  private final String pluginName;
  private boolean isRegistered;
  private long before;

  /**
   * (package-private) <br>
   *
   * @param readerName The name of the reader.
   * @param pluginName The name of the plugin.
   * @since 2.0
   */
  AbstractReaderAdapter(String readerName, String pluginName) {
    this.readerName = readerName;
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
   * @throws UnexpectedStatusCodeException If status code verification is enabled in the card
   *     request and the card returned an unexpected code.
   * @since 2.0
   */
  final List<KeypleCardSelectionResponse> transmitCardSelectionRequests(
      List<CardSelectionRequest> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException,
          UnexpectedStatusCodeException {
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
    } catch (ReaderCommunicationException e) {
      if (logger.isErrorEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] Reader IO failure while transmitting card selection request. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw e;
    } catch (CardCommunicationException e) {
      if (logger.isErrorEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] Card IO failure while transmitting card selection request. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw e;
    } catch (UnexpectedStatusCodeException e) {
      if (logger.isErrorEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] Unexpected status code reiceived. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw e;
    }

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
   * Change the reader status to registered
   *
   * @since 2.0
   */
  final void register() {
    isRegistered = true;
  }

  /**
   * (package-private)<br>
   * Change the reader status to unregistered
   *
   * <p>This method may be overridden in order to meet specific needs in certain implementations of
   * readers.
   *
   * @throws IllegalStateException is thrown when plugin is already unregistered.
   * @since 2.0
   */
  void unregister() {
    checkStatus();
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
    } catch (ReaderCommunicationException ex) {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] Reader IO failure while transmitting card selection request. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw ex;
    } catch (CardCommunicationException ex) {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] Card IO failure while transmitting card selection request. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw ex;
    } catch (UnexpectedStatusCodeException e) {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] An unexpected status code was received while transmitting card selection request. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      throw e;
    }

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] receive => {}, elapsed {} ms.", this.getName(), cardResponse, elapsed10ms / 10.0);
    }

    return cardResponse;
  }
}
