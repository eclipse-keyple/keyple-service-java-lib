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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.ApduRequestSpi;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.reader.selection.spi.CardSelector;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.AutonomousSelectionReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.ApduUtil;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Local reader adapter.
 *
 * @since 2.0
 */
class LocalReaderAdapter extends AbstractReaderAdapter {

  private static final Logger logger = LoggerFactory.getLogger(LocalReaderAdapter.class);

  /** predefined ISO values */
  private static final byte[] APDU_GET_RESPONSE = {
    (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00
  };

  private static final byte[] APDU_GET_DATA = {
    (byte) 0x00, (byte) 0xCA, (byte) 0x00, (byte) 0x6F, (byte) 0x00
  };
  private static final int DEFAULT_SUCCESSFUL_CODE = 0x9000;

  private final ReaderSpi readerSpi;
  private long before;
  private boolean logicalChannelIsOpen;
  private boolean useDefaultProtocol;
  private String currentProtocol;
  private final Map<String, String> protocolAssociations;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param readerSpi The reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0
   */
  LocalReaderAdapter(ReaderSpi readerSpi, String pluginName) {
    super(readerSpi.getName(), readerSpi, pluginName);
    this.readerSpi = readerSpi;
    protocolAssociations = new LinkedHashMap<String, String>();
  }

  /**
   * (package-private)<br>
   * Gets {@link ReaderSpi} associated to this reader.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public ReaderSpi getReaderSpi() {
    return readerSpi;
  }

  /**
   * (package-private)<br>
   * Gets the logical channel's opening state.
   *
   * @return True if the channel is open, false if not.
   * @since 2.0
   */
  final boolean isLogicalChannelOpen() {
    return logicalChannelIsOpen;
  }

  /**
   * (package-private)<br>
   * Close both logical and physical channels
   *
   * <p>This method doesn't raise any exception.
   *
   * @since 2.0
   */
  final void closeLogicalAndPhysicalChannelsSilently() {

    closeLogicalChannel();
    // Closes the physical channel and resets the current protocol info.
    currentProtocol = null;
    useDefaultProtocol = false;
    try {
      readerSpi.closePhysicalChannel();
    } catch (ReaderIOException e) {
      logger.error(
          "[{}] Exception occurred in releaseSeChannel. Message: {}",
          this.getName(),
          e.getMessage(),
          e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Invoke {@link ReaderSpi#unregister()} on the associated SPI.
   *
   * @since 2.0
   */
  @Override
  void unregister() {
    super.unregister();
    readerSpi.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final List<CardSelectionResponseApi> processCardSelectionRequests(
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException,
          UnexpectedStatusWordException {

    checkStatus();

    List<CardSelectionResponseApi> cardSelectionResponses =
        new ArrayList<CardSelectionResponseApi>();

    /* Open the physical channel if needed, determine the current protocol */
    if (!readerSpi.isPhysicalChannelOpen()) {
      try {
        openPhysicalChannelAndSetProtocol();
      } catch (ReaderIOException e) {
        throw new ReaderBrokenCommunicationException(
            null, "Reader communication failure while opening physical channel", e);
      } catch (CardIOException e) {
        throw new CardBrokenCommunicationException(
            null, "Card communication failure while opening physical channel", e);
      }
    }

    /* loop over all CardRequest provided in the list */
    for (CardSelectionRequestSpi cardSelectionRequest : cardSelectionRequests) {
      /* process the CardRequest and append the CardResponse list */
      CardSelectionResponseApi cardSelectionResponse =
          processCardSelectionRequest(cardSelectionRequest);
      cardSelectionResponses.add(cardSelectionResponse);
      if (multiSelectionProcessing == MultiSelectionProcessing.PROCESS_ALL) {
        /* multi CardRequest case: just close the logical channel and go on with the next selection. */
        closeLogicalChannel();
      } else {
        if (logicalChannelIsOpen) {
          /* the logical channel being open, we stop here */
          break; // exit for loop
        }
      }
    }

    /* close the channel if requested */
    if (channelControl == ChannelControl.CLOSE_AFTER) {
      releaseChannel();
    }

    return cardSelectionResponses;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final CardResponseApi processCardRequest(
      CardRequestSpi cardRequest, ChannelControl channelControl)
      throws CardBrokenCommunicationException, ReaderBrokenCommunicationException,
          UnexpectedStatusWordException {

    checkStatus();

    CardResponseApi cardResponse;

    /* process the CardRequest and keep the CardResponse */
    cardResponse = processCardRequest(cardRequest);

    /* close the channel if requested */
    if (channelControl == ChannelControl.CLOSE_AFTER) {
      releaseChannel();
    }

    return cardResponse;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final boolean isContactless() {
    return readerSpi.isContactless();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public boolean isCardPresent() {
    checkStatus();
    try {
      return readerSpi.checkCardPresence();
    } catch (ReaderIOException e) {
      throw new KeypleReaderCommunicationException(
          "An exception occurred while checking the card presence.", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void activateProtocol(String readerProtocol, String applicationProtocol) {
    checkStatus();
    Assert.getInstance()
        .notEmpty(readerProtocol, "readerProtocol")
        .notEmpty(applicationProtocol, "applicationProtocol");

    if (!readerSpi.isProtocolSupported(readerProtocol)) {
      throw new KeypleReaderProtocolNotSupportedException(readerProtocol);
    }

    readerSpi.activateProtocol(readerProtocol);

    protocolAssociations.put(readerProtocol, applicationProtocol);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public final void deactivateProtocol(String readerProtocol) {
    checkStatus();
    Assert.getInstance().notEmpty(readerProtocol, "readerProtocol");

    protocolAssociations.remove(readerProtocol);

    if (!readerSpi.isProtocolSupported(readerProtocol)) {
      throw new KeypleReaderProtocolNotSupportedException(readerProtocol);
    }

    readerSpi.deactivateProtocol(readerProtocol);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void releaseChannel() throws ReaderBrokenCommunicationException {
    checkStatus();
    try {
      readerSpi.closePhysicalChannel();
    } catch (ReaderIOException e) {
      throw new ReaderBrokenCommunicationException(
          null, "Failed to release the physical channel", e);
    }
  }

  /**
   * (private)<br>
   * Opens the physical channel, determines and keep the current protocol.
   *
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private void openPhysicalChannelAndSetProtocol() throws ReaderIOException, CardIOException {
    readerSpi.openPhysicalChannel();
    computeCurrentProtocol();
  }

  /**
   * (private)<br>
   * Transmits a {@link CardRequestSpi} and returns a {@link CardResponseApi}.
   *
   * @param cardRequest The card request to transmit.
   * @return A not null reference.
   * @throws ReaderBrokenCommunicationException If the communication with the reader has failed.
   * @throws CardBrokenCommunicationException If the communication with the card has failed.
   * @throws UnexpectedStatusWordException If status word verification is enabled in the card
   *     request and the card returned an unexpected code.
   */
  private CardResponseApi processCardRequest(CardRequestSpi cardRequest)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException,
          UnexpectedStatusWordException {

    List<ApduResponseApi> apduResponses = new ArrayList<ApduResponseApi>();

    /* Proceeds with the APDU requests present in the CardRequest */
    for (ApduRequestSpi apduRequest : cardRequest.getApduRequests()) {
      try {
        ApduResponseApi apduResponse = processApduRequest(apduRequest);
        apduResponses.add(apduResponse);
        if (cardRequest.stopOnUnsuccessfulStatusWord()
            && !apduRequest.getSuccessfulStatusWords().contains(apduResponse.getStatusWord())) {
          throw new UnexpectedStatusWordException(
              new CardResponseAdapter(
                  apduResponses,
                  false,
                  cardRequest.getApduRequests().size() == apduResponses.size()),
              "Unexpected status word.");
        }
      } catch (ReaderIOException e) {
        /*
         * The process has been interrupted. We close the logical channel and launch a
         * KeypleReaderException with the Apdu responses collected so far.
         */
        closeLogicalAndPhysicalChannelsSilently();

        throw new ReaderBrokenCommunicationException(
            new CardResponseAdapter(apduResponses, false, false),
            "Reader communication failure while transmitting a card request.",
            e);
      } catch (CardIOException e) {
        /*
         * The process has been interrupted. We close the logical channel and launch a
         * KeypleReaderException with the Apdu responses collected so far.
         */
        closeLogicalAndPhysicalChannelsSilently();

        throw new CardBrokenCommunicationException(
            new CardResponseAdapter(apduResponses, false, false),
            "Card communication failure while transmitting a card request.",
            e);
      }
    }

    return new CardResponseAdapter(apduResponses, logicalChannelIsOpen, true);
  }

  /**
   * (private)<br>
   * Transmits an {@link ApduRequestSpi} and receives the {@link ApduResponseApi}.
   *
   * <p>The time measurement is carried out and logged with the detailed information of the
   * exchanges (TRACE level).
   *
   * @param apduRequest The APDU request to transmit.
   * @return A not null reference.
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private ApduResponseApi processApduRequest(ApduRequestSpi apduRequest)
      throws CardIOException, ReaderIOException {

    ApduResponseApi apduResponse;
    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] processApduRequest => {}, elapsed {} ms.",
          this.getName(),
          apduRequest,
          elapsed10ms / 10.0);
    }

    apduResponse = new ApduResponseAdapter(readerSpi.transmitApdu(apduRequest.getBytes()));

    if (ApduUtil.isCase4(apduRequest.getBytes())
        && apduResponse.getDataOut().length == 0
        && apduResponse.getStatusWord() == DEFAULT_SUCCESSFUL_CODE) {
      // do the get response command
      apduResponse = case4HackGetResponse();
    }

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] processApduRequest => {}, elapsed {} ms.",
          this.getName(),
          apduResponse,
          elapsed10ms / 10.0);
    }

    return apduResponse;
  }

  /**
   * (private)<br>
   * Process dedicated to some cards not following the ISO standard for case 4 management.
   *
   * <p>Execute an explicit get response command in order to get the outgoing data from specific
   * cards answering 9000 with no data although the command has outgoing data.
   *
   * @return ApduResponse the response to the get response command
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private ApduResponseApi case4HackGetResponse() throws ReaderIOException, CardIOException {

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      // TODO review log format
      logger.debug(
          "[{}] case4HackGetResponse => ApduRequest: NAME = \"Internal Get Response\", RAWDATA = {}, elapsed = {}",
          this.getName(),
          ByteArrayUtil.toHex(APDU_GET_RESPONSE),
          elapsed10ms / 10.0);
    }

    byte[] getResponseHackResponseBytes = readerSpi.transmitApdu(APDU_GET_RESPONSE);

    ApduResponseApi getResponseHackResponse = new ApduResponseAdapter(getResponseHackResponseBytes);

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      // TODO review log format
      logger.debug(
          "[{}] case4HackGetResponse => Internal {}, elapsed {} ms.",
          this.getName(),
          getResponseHackResponseBytes,
          elapsed10ms / 10.0);
    }

    return getResponseHackResponse;
  }

  /**
   * (private)<br>
   * Attempts to select the card and executes the optional requests if any.
   *
   * @param cardSelectionRequest The {@link CardSelectionRequestSpi} to be processed.
   * @return A not null reference.
   * @throws ReaderBrokenCommunicationException If the communication with the reader has failed.
   * @throws CardBrokenCommunicationException If the communication with the card has failed.
   * @throws UnexpectedStatusWordException If status word verification is enabled in the card
   *     request and the card returned an unexpected code.
   */
  private CardSelectionResponseApi processCardSelectionRequest(
      CardSelectionRequestSpi cardSelectionRequest)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException,
          UnexpectedStatusWordException {

    SelectionStatusApi selectionStatus;
    try {
      selectionStatus = processSelection((CardSelector) cardSelectionRequest.getCardSelector());
    } catch (ReaderIOException e) {
      throw new ReaderBrokenCommunicationException(
          new CardResponseAdapter(new ArrayList<ApduResponseApi>(), false, false),
          e.getMessage(),
          e);
    } catch (CardIOException e) {
      throw new CardBrokenCommunicationException(
          new CardResponseAdapter(new ArrayList<ApduResponseApi>(), false, false),
          e.getMessage(),
          e);
    }
    if (!selectionStatus.hasMatched()) {
      // the selection failed, return an empty response having the selection status
      return new CardSelectionResponseAdapter(
          selectionStatus, new CardResponseAdapter(new ArrayList<ApduResponseApi>(), false, false));
    }

    logicalChannelIsOpen = true;

    CardResponseApi cardResponse;

    if (cardSelectionRequest.getCardRequest() != null) {
      cardResponse = processCardRequest(cardSelectionRequest.getCardRequest());
    } else {
      cardResponse = null;
    }

    return new CardSelectionResponseAdapter(selectionStatus, cardResponse);
  }

  /**
   * (private)<br>
   * Select the card according to the {@link CardSelectorAdapter}.
   *
   * <p>The selection status is returned.<br>
   * 3 levels of filtering/selection are applied successively if they are enabled: protocol, power
   * on data and AID.<br>
   * As soon as one of these operations fails, the method returns with a failed selection status.
   *
   * <p>Conversely, the selection is considered successful if none of the filters have rejected the
   * card, even if none of the filters are active.
   *
   * @param cardSelector A not null {@link CardSelectorAdapter}.
   * @return A not null {@link SelectionStatusApi}.
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private SelectionStatusApi processSelection(CardSelector cardSelector)
      throws CardIOException, ReaderIOException {

    byte[] powerOnData;
    ApduResponseApi fciResponse;
    boolean hasMatched = true;

    if (cardSelector.getCardProtocol() != null && useDefaultProtocol) {
      throw new IllegalStateException(
          "Protocol " + cardSelector.getCardProtocol() + " not associated to a reader protocol.");
    }

    // check protocol if enabled
    if (cardSelector.getCardProtocol() == null
        || cardSelector.getCardProtocol().equals(currentProtocol)) {
      // protocol check succeeded, check power-on data if enabled
      powerOnData = readerSpi.getPowerOnData();
      if (checkPowerOnData(powerOnData, cardSelector)) {
        // no power-on data filter or power-on data check succeeded, select by AID if enabled.
        if (cardSelector.getAid() != null) {
          fciResponse = selectByAid(cardSelector);
          hasMatched =
              cardSelector
                  .getSuccessfulSelectionStatusWords()
                  .contains(fciResponse.getStatusWord());
        } else {
          fciResponse = null;
        }
      } else {
        // check failed
        hasMatched = false;
        fciResponse = null;
      }
    } else {
      // protocol failed
      powerOnData = null;
      fciResponse = null;
      hasMatched = false;
    }
    return new SelectionStatusAdapter(powerOnData, fciResponse, hasMatched);
  }

  /**
   * (private)<br>
   * Checks the provided power-on data with the PowerOnDataFilter.
   *
   * <p>Returns true if the power-on data is accepted by the filter.
   *
   * @param powerOnData A byte array.
   * @param cardSelector The card selector.
   * @return True or false.
   * @throws IllegalStateException if no power-on data is available and the PowerOnDataFilter is
   *     set.
   * @see #processSelection(CardSelector)
   */
  private boolean checkPowerOnData(byte[] powerOnData, CardSelector cardSelector) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "[{}] openLogicalChannel => PowerOnData = {}",
          this.getName(),
          ByteArrayUtil.toHex(powerOnData));
    }

    // check the power-on data
    if (!cardSelector.powerOnDataMatches(powerOnData)) {
      if (logger.isInfoEnabled()) {
        logger.info(
            "[{}] openLogicalChannel => Power-on data didn't match. PowerOnData = {}, regex filter = {}",
            this.getName(),
            ByteArrayUtil.toHex(powerOnData),
            cardSelector.getPowerOnDataRegex());
      }
      // the power-on data have been rejected
      return false;
    } else {
      // the power-on data have been accepted
      return true;
    }
  }

  /**
   * (private)<br>
   * Selects the card with the provided AID and gets the FCI response in return.
   *
   * <p>If the initial FCI response does not contain any data, a special Get Data command is issued
   * to retrieve the FCI structure (OMAPI case).
   *
   * @param cardSelector The card selector.
   * @return An not null {@link ApduResponseApi} containing the FCI.
   * @see #processSelection(CardSelector)
   */
  private ApduResponseApi selectByAid(CardSelector cardSelector)
      throws CardIOException, ReaderIOException {

    ApduResponseApi fciResponse;

    if (readerSpi instanceof AutonomousSelectionReaderSpi) {
      byte[] aid = cardSelector.getAid();
      byte isoControlMask =
          (byte)
              (cardSelector.getFileOccurrence().getIsoBitMask()
                  | cardSelector.getFileControlInformation().getIsoBitMask());
      byte[] selectionDataBytes =
          ((AutonomousSelectionReaderSpi) readerSpi).openChannelForAid(aid, isoControlMask);
      fciResponse = new ApduResponseAdapter(selectionDataBytes);
    } else {
      fciResponse = processExplicitAidSelection(cardSelector);
    }

    if (fciResponse.getStatusWord() == DEFAULT_SUCCESSFUL_CODE
        && fciResponse.getDataOut().length == 0) {
      /*
       * The selection didn't provide data (e.g. OMAPI), we get the FCI using a Get Data
       * command.
       *
       * The AID selector is provided to handle successful status word in the Get Data
       * command.
       */
      fciResponse = recoverSelectionFciData();
    }
    return fciResponse;
  }

  /**
   * (private)<br>
   * This method is dedicated to the case where no FCI data is available in return for the select
   * command.
   *
   * <p>A specific APDU is sent to the card to retrieve the FCI data and returns it in an {@link
   * ApduResponseApi}.<br>
   * The provided AidSelector is used to check the response's status words.
   *
   * @return A {@link ApduResponseApi} containing the FCI.
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private ApduResponseApi recoverSelectionFciData() throws CardIOException, ReaderIOException {

    ApduRequestAdapter apduRequest = new ApduRequestAdapter(APDU_GET_DATA);

    if (logger.isDebugEnabled()) {
      apduRequest.setName("Internal Get Data");
    }

    return processApduRequest(apduRequest);
  }

  /**
   * (private)<br>
   * Sends the select application command to the card and returns the requested data according to
   * AidSelector attributes (ISO7816-4 selection data) into an {@link ApduResponseApi}.
   *
   * @param cardSelector The card selector.
   * @return A not null {@link ApduResponseApi}.
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private ApduResponseApi processExplicitAidSelection(CardSelector cardSelector)
      throws CardIOException, ReaderIOException {

    final byte[] aid = cardSelector.getAid();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "[{}] openLogicalChannel => Select Application with AID = {}",
          this.getName(),
          ByteArrayUtil.toHex(aid));
    }
    /*
     * build a get response command the actual length expected by the card in the get response
     * command is handled in transmitApdu
     */
    byte[] selectApplicationCommand = new byte[6 + aid.length];
    selectApplicationCommand[0] = (byte) 0x00; // CLA
    selectApplicationCommand[1] = (byte) 0xA4; // INS
    selectApplicationCommand[2] = (byte) 0x04; // P1: select by name
    // P2: b0,b1 define the File occurrence, b2,b3 define the File control information
    // we use the bitmask defined in the respective enums
    selectApplicationCommand[3] =
        (byte)
            (cardSelector.getFileOccurrence().getIsoBitMask()
                | cardSelector.getFileControlInformation().getIsoBitMask());
    selectApplicationCommand[4] = (byte) (aid.length); // Lc
    System.arraycopy(aid, 0, selectApplicationCommand, 5, aid.length); // data
    selectApplicationCommand[5 + aid.length] = (byte) 0x00; // Le

    ApduRequestAdapter apduRequest = new ApduRequestAdapter(selectApplicationCommand);

    if (logger.isDebugEnabled()) {
      apduRequest.setName("Internal Select Application");
    }

    return processApduRequest(apduRequest);
  }

  /**
   * (private)<br>
   * Close the logical channel.
   */
  private void closeLogicalChannel() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] closeLogicalChannel => Closing of the logical channel.", this.getName());
    }
    if (readerSpi instanceof AutonomousSelectionReaderSpi) {
      /* AutonomousSelectionReader have an explicit method for closing channels */
      ((AutonomousSelectionReaderSpi) readerSpi).closeLogicalChannel();
    }
    logicalChannelIsOpen = false;
  }

  /**
   * (private)<br>
   * Determines the current protocol used by the card.
   *
   * <p>The Map {@link #protocolAssociations} containing the protocol names (reader and application)
   * is iterated and the reader protocol (key of the Map) is checked with the reader.<br>
   *
   * <p>If the Map is not empty:
   * <li>The boolean {@link #useDefaultProtocol} is set to false.
   * <li>If the test provided by the reader SPI is positive (the protocol presented is the one used
   *     by the current card) then the field {@link #currentProtocol} is set with the name of the
   *     protocol known to the application.
   * <li>If none of the protocols present in the Map matches then the {@link #currentProtocol} is
   *     set to null.
   * </ul>
   *
   * <p>If the Map is empty, no other check is done, the String field {@link #currentProtocol} is
   * set to null and the boolean field {@link #useDefaultProtocol} is set to true.
   */
  private void computeCurrentProtocol() {

    /* Determine the current protocol */
    currentProtocol = null;
    if (protocolAssociations.size() == 0) {
      useDefaultProtocol = true;
    } else {
      useDefaultProtocol = false;
      for (Map.Entry<String, String> entry : protocolAssociations.entrySet()) {
        if (readerSpi.isCurrentProtocol(entry.getKey())) {
          currentProtocol = entry.getValue();
        }
      }
    }
  }
}
