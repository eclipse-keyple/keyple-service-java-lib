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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.ApduRequestSpi;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectorSpi;
import org.calypsonet.terminal.reader.ReaderCommunicationException;
import org.calypsonet.terminal.reader.ReaderProtocolNotSupportedException;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.AutonomousSelectionReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.ApduUtil;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Local reader adapter.
 *
 * <ul>
 *   <li>RL-CMD-USED.1
 *   <li>RL-CLA-ACCEPTED.1
 *   <li>RL-PERF-TIME.1
 * </ul>
 *
 * @since 2.0.0
 */
class LocalReaderAdapter extends AbstractReaderAdapter {

  private static final Logger logger = LoggerFactory.getLogger(LocalReaderAdapter.class);

  /** predefined ISO values */
  private static final int SW_9000 = 0x9000;

  private static final int SW_6100 = 0x6100;
  private static final int SW_6C00 = 0x6C00;

  private static final int SW1_MASK = 0xFF00;
  private static final int SW2_MASK = 0x00FF;

  private final ReaderSpi readerSpi;
  private long before;
  private boolean isLogicalChannelOpen;
  private boolean useDefaultProtocol;
  private String currentProtocol;
  private final Map<String, String> protocolAssociations;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param readerSpi The reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0.0
   */
  LocalReaderAdapter(ReaderSpi readerSpi, String pluginName) {
    super(readerSpi.getName(), (KeypleReaderExtension) readerSpi, pluginName);
    this.readerSpi = readerSpi;
    protocolAssociations = new LinkedHashMap<String, String>();
  }

  /**
   * (package-private)<br>
   * Gets {@link ReaderSpi} associated to this reader.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public ReaderSpi getReaderSpi() {
    return readerSpi;
  }

  /**
   * (package-private)<br>
   * Gets the logical channel's opening state.
   *
   * @return True if the channel is open, false if not.
   * @since 2.0.0
   */
  final boolean isLogicalChannelOpen() {
    return isLogicalChannelOpen;
  }

  /**
   * (package-private)<br>
   * Close both logical and physical channels
   *
   * <p>This method doesn't raise any exception.
   *
   * @since 2.0.0
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
   * <p>Invoke {@link ReaderSpi#onUnregister()} on the associated SPI.
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    try {
      readerSpi.onUnregister();
    } catch (Exception e) {
      logger.error("Error during the unregistration of the extension of reader '{}'", getName(), e);
    }
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
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
        readerSpi.openPhysicalChannel();
        computeCurrentProtocol();
      } catch (ReaderIOException e) {
        throw new ReaderBrokenCommunicationException(
            null, false, "Reader communication failure while opening physical channel", e);
      } catch (CardIOException e) {
        throw new CardBrokenCommunicationException(
            null, false, "Card communication failure while opening physical channel", e);
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
        if (isLogicalChannelOpen) {
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
   * @since 2.0.0
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
   * @since 2.0.0
   */
  @Override
  public final boolean isContactless() {
    return readerSpi.isContactless();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public boolean isCardPresent() {
    // RL-DET-PCRQ.1
    // RL-DET-PCAPDU.1
    checkStatus();
    try {
      return readerSpi.checkCardPresence();
    } catch (ReaderIOException e) {
      throw new ReaderCommunicationException(
          "An exception occurred while checking the card presence.", e);
    }
  }

  /**
   * (package-private)<br>
   * Activates a protocol (for configurable reader only).
   *
   * @param readerProtocol The reader protocol.
   * @param applicationProtocol The corresponding application protocol to associate.
   * @since 2.0.0
   */
  final void activateReaderProtocol(String readerProtocol, String applicationProtocol) {
    // RL-CL-PROTOCOL.1
    checkStatus();
    Assert.getInstance()
        .notEmpty(readerProtocol, "readerProtocol")
        .notEmpty(applicationProtocol, "applicationProtocol");
    if (!((ConfigurableReaderSpi) readerSpi).isProtocolSupported(readerProtocol)) {
      throw new ReaderProtocolNotSupportedException(readerProtocol);
    }
    ((ConfigurableReaderSpi) readerSpi).activateProtocol(readerProtocol);
    protocolAssociations.put(readerProtocol, applicationProtocol);
  }

  /**
   * (package-private)<br>
   * Deactivates a protocol (for configurable reader only).
   *
   * @param readerProtocol The reader protocol.
   * @since 2.0.0
   */
  final void deactivateReaderProtocol(String readerProtocol) {
    // RL-CL-PROTOCOL.1
    checkStatus();
    Assert.getInstance().notEmpty(readerProtocol, "readerProtocol");
    protocolAssociations.remove(readerProtocol);
    if (!((ConfigurableReaderSpi) readerSpi).isProtocolSupported(readerProtocol)) {
      throw new ReaderProtocolNotSupportedException(readerProtocol);
    }
    ((ConfigurableReaderSpi) readerSpi).deactivateProtocol(readerProtocol);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void releaseChannel() throws ReaderBrokenCommunicationException {
    checkStatus();
    try {
      readerSpi.closePhysicalChannel();
    } catch (ReaderIOException e) {
      throw new ReaderBrokenCommunicationException(
          null, false, "Failed to release the physical channel", e);
    }
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
  private CardResponseAdapter processCardRequest(CardRequestSpi cardRequest)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException,
          UnexpectedStatusWordException {

    List<ApduResponseAdapter> apduResponses = new ArrayList<ApduResponseAdapter>();

    /* Proceeds with the APDU requests present in the CardRequest */
    for (ApduRequestSpi apduRequest : cardRequest.getApduRequests()) {
      try {
        ApduResponseAdapter apduResponse = processApduRequest(apduRequest);
        apduResponses.add(apduResponse);
        if (cardRequest.stopOnUnsuccessfulStatusWord()
            && !apduRequest.getSuccessfulStatusWords().contains(apduResponse.getStatusWord())) {
          throw new UnexpectedStatusWordException(
              new CardResponseAdapter(apduResponses, false),
              cardRequest.getApduRequests().size() == apduResponses.size(),
              "Unexpected status word.");
        }
      } catch (ReaderIOException e) {
        /*
         * The process has been interrupted. We close the logical channel and launch a
         * KeypleReaderException with the Apdu responses collected so far.
         */
        closeLogicalAndPhysicalChannelsSilently();

        throw new ReaderBrokenCommunicationException(
            new CardResponseAdapter(apduResponses, false),
            false,
            "Reader communication failure while transmitting a card request.",
            e);
      } catch (CardIOException e) {
        /*
         * The process has been interrupted. We close the logical channel and launch a
         * KeypleReaderException with the Apdu responses collected so far.
         */
        closeLogicalAndPhysicalChannelsSilently();

        throw new CardBrokenCommunicationException(
            new CardResponseAdapter(apduResponses, false),
            false,
            "Card communication failure while transmitting a card request.",
            e);
      }
    }

    return new CardResponseAdapter(apduResponses, isLogicalChannelOpen);
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
  private ApduResponseAdapter processApduRequest(ApduRequestSpi apduRequest)
      throws CardIOException, ReaderIOException {

    ApduResponseAdapter apduResponse;
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

    apduResponse = new ApduResponseAdapter(readerSpi.transmitApdu(apduRequest.getApdu()));

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

    if (apduResponse.getDataOut().length == 0) {

      if ((apduResponse.getStatusWord() & SW1_MASK) == SW_6100) {
        // RL-SW-61XX.1
        // Build a GetResponse APDU command with the provided "le"
        byte[] getResponseApdu = {
          (byte) 0x00,
          (byte) 0xC0,
          (byte) 0x00,
          (byte) 0x00,
          (byte) (apduResponse.getStatusWord() & SW2_MASK)
        };
        // Execute APDU
        apduResponse =
            processApduRequest(
                new ApduRequestAdapter(getResponseApdu).setInfo("Internal Get Response"));

      } else if ((apduResponse.getStatusWord() & SW1_MASK) == SW_6C00) {
        // RL-SW-6CXX.1
        // Update the last command with the provided "le"
        apduRequest.getApdu()[apduRequest.getApdu().length - 1] =
            (byte) (apduResponse.getStatusWord() & SW2_MASK);
        // Replay the last command APDU
        apduResponse = processApduRequest(apduRequest);

      } else if (ApduUtil.isCase4(apduRequest.getApdu())
          && apduRequest.getSuccessfulStatusWords().contains(apduResponse.getStatusWord())) {
        // RL-SW-ANALYSIS.1
        // RL-SW-CASE4.1 (SW=6200 not taken into account here)
        // Build a GetResponse APDU command with the original "le"
        byte[] getResponseApdu = {
          (byte) 0x00,
          (byte) 0xC0,
          (byte) 0x00,
          (byte) 0x00,
          apduRequest.getApdu()[apduRequest.getApdu().length - 1]
        };
        // Execute GetResponse APDU
        apduResponse =
            processApduRequest(
                new ApduRequestAdapter(getResponseApdu).setInfo("Internal Get Response"));
      }
    }

    return apduResponse;
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

    isLogicalChannelOpen = false;
    SelectionStatus selectionStatus;
    try {
      selectionStatus = processSelection(cardSelectionRequest.getCardSelector());
    } catch (ReaderIOException e) {
      throw new ReaderBrokenCommunicationException(
          new CardResponseAdapter(new ArrayList<ApduResponseAdapter>(), false),
          false,
          e.getMessage(),
          e);
    } catch (CardIOException e) {
      throw new CardBrokenCommunicationException(
          new CardResponseAdapter(new ArrayList<ApduResponseAdapter>(), false),
          false,
          e.getMessage(),
          e);
    }
    if (!selectionStatus.hasMatched) {
      // the selection failed, return an empty response having the selection status
      return new CardSelectionResponseAdapter(
          selectionStatus.powerOnData,
          selectionStatus.selectApplicationResponse,
          false,
          new CardResponseAdapter(new ArrayList<ApduResponseAdapter>(), false));
    }

    isLogicalChannelOpen = true;

    CardResponseAdapter cardResponse;

    if (cardSelectionRequest.getCardRequest() != null) {
      cardResponse = processCardRequest(cardSelectionRequest.getCardRequest());
    } else {
      cardResponse = null;
    }

    return new CardSelectionResponseAdapter(
        selectionStatus.powerOnData, selectionStatus.selectApplicationResponse, true, cardResponse);
  }

  /**
   * (private)<br>
   * Select the card according to the {@link CardSelectorSpi}.
   *
   * <p>The selection status is returned.<br>
   * 3 levels of filtering/selection are applied successively if they are enabled: protocol, power
   * on data and AID.<br>
   * As soon as one of these operations fails, the method returns with a failed selection status.
   *
   * <p>Conversely, the selection is considered successful if none of the filters have rejected the
   * card, even if none of the filters are active.
   *
   * @param cardSelector A not null {@link CardSelectorSpi}.
   * @return A not null {@link SelectionStatus}.
   * @throws ReaderIOException if the communication with the reader has failed.
   * @throws CardIOException if the communication with the card has failed.
   */
  private SelectionStatus processSelection(CardSelectorSpi cardSelector)
      throws CardIOException, ReaderIOException {

    // RL-CLA-CHAAUTO.1
    String powerOnData;
    ApduResponseAdapter fciResponse;
    boolean hasMatched = true;

    if (cardSelector.getCardProtocol() != null && useDefaultProtocol) {
      throw new IllegalStateException(
          "Protocol " + cardSelector.getCardProtocol() + " not associated to a reader protocol.");
    }

    // check protocol if enabled
    if (cardSelector.getCardProtocol() == null
        || cardSelector.getCardProtocol().equals(currentProtocol)) {
      // protocol check succeeded, check power-on data if enabled
      // RL-ATR-FILTER
      // RL-SEL-USAGE.1
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
    return new SelectionStatus(powerOnData, fciResponse, hasMatched);
  }

  /**
   * (private)<br>
   * Checks the provided power-on data with the PowerOnDataFilter.
   *
   * <p>Returns true if the power-on data is accepted by the filter.
   *
   * @param powerOnData A String containing the power-on data.
   * @param cardSelector The card selector.
   * @return True or false.
   * @throws IllegalStateException if no power-on data is available and the PowerOnDataFilter is
   *     set.
   * @see #processSelection(CardSelectorSpi)
   */
  private boolean checkPowerOnData(String powerOnData, CardSelectorSpi cardSelector) {

    if (logger.isDebugEnabled()) {
      logger.debug("[{}] openLogicalChannel => PowerOnData = {}", this.getName(), powerOnData);
    }
    String powerOnDataRegex = cardSelector.getPowerOnDataRegex();
    // check the power-on data
    if (powerOnData != null && powerOnDataRegex != null && !powerOnData.matches(powerOnDataRegex)) {
      if (logger.isInfoEnabled()) {
        logger.info(
            "[{}] openLogicalChannel => Power-on data didn't match. PowerOnData = {}, regex filter = {}",
            this.getName(),
            powerOnData,
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
   * @param cardSelector The card selector.
   * @return A not null {@link ApduResponseApi} containing the FCI.
   * @see #processSelection(CardSelectorSpi)
   */
  private ApduResponseAdapter selectByAid(CardSelectorSpi cardSelector)
      throws CardIOException, ReaderIOException {

    ApduResponseAdapter fciResponse;

    // RL-SEL-P2LC.1
    // RL-SEL-DFNAME.1
    Assert.getInstance()
        .notNull(cardSelector.getAid(), "aid")
        .isInRange(cardSelector.getAid().length, 0, 16, "aid");

    if (readerSpi instanceof AutonomousSelectionReaderSpi) {
      byte[] aid = cardSelector.getAid();
      byte p2 =
          computeSelectApplicationP2(
              cardSelector.getFileOccurrence(), cardSelector.getFileControlInformation());
      byte[] selectionDataBytes =
          ((AutonomousSelectionReaderSpi) readerSpi).openChannelForAid(aid, p2);
      fciResponse = new ApduResponseAdapter(selectionDataBytes);
    } else {
      fciResponse = processExplicitAidSelection(cardSelector);
    }
    return fciResponse;
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
  private ApduResponseAdapter processExplicitAidSelection(CardSelectorSpi cardSelector)
      throws CardIOException, ReaderIOException {

    final byte[] aid = cardSelector.getAid();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "[{}] openLogicalChannel => Select Application with AID = {}",
          this.getName(),
          HexUtil.toHex(aid));
    }
    /*
     * build a get response command the actual length expected by the card in the get response
     * command is handled in transmitApdu
     */
    // RL-SEL-CLA.1
    // RL-SEL-P2LC.1
    byte[] selectApplicationCommand = new byte[6 + aid.length];
    selectApplicationCommand[0] = (byte) 0x00; // CLA
    selectApplicationCommand[1] = (byte) 0xA4; // INS
    selectApplicationCommand[2] = (byte) 0x04; // P1: select by name
    // P2: b0,b1 define the File occurrence, b2,b3 define the File control information
    // we use the bitmask defined in the respective enums
    selectApplicationCommand[3] =
        computeSelectApplicationP2(
            cardSelector.getFileOccurrence(), cardSelector.getFileControlInformation());
    selectApplicationCommand[4] = (byte) (aid.length); // Lc
    System.arraycopy(aid, 0, selectApplicationCommand, 5, aid.length); // data
    selectApplicationCommand[5 + aid.length] = (byte) 0x00; // Le

    ApduRequestAdapter apduRequest = new ApduRequestAdapter(selectApplicationCommand);

    if (logger.isDebugEnabled()) {
      apduRequest.setInfo("Internal Select Application");
    }

    return processApduRequest(apduRequest);
  }

  /**
   * (private)<br>
   * Computes the P2 parameter of the ISO7816-4 Select Application APDU command from the provided
   * FileOccurrence and FileControlInformation.
   *
   * @param fileOccurrence The file's position relative to the current file.
   * @param fileControlInformation The file control information output.
   * @throws IllegalStateException If one of the provided argument is unexpected.
   */
  private byte computeSelectApplicationP2(
      CardSelectorSpi.FileOccurrence fileOccurrence,
      CardSelectorSpi.FileControlInformation fileControlInformation) {

    byte p2;
    switch (fileOccurrence) {
      case FIRST:
        p2 = (byte) 0x00;
        break;
      case LAST:
        p2 = (byte) 0x01;
        break;
      case NEXT:
        p2 = (byte) 0x02;
        break;
      case PREVIOUS:
        p2 = (byte) 0x03;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + fileOccurrence);
    }

    switch (fileControlInformation) {
      case FCI:
        p2 |= (byte) 0x00;
        break;
      case FCP:
        p2 |= (byte) 0x04;
        break;
      case FMD:
        p2 |= (byte) 0x08;
        break;
      case NO_RESPONSE:
        p2 |= (byte) 0x0C;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + fileControlInformation);
    }

    return p2;
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
    isLogicalChannelOpen = false;
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
    currentProtocol = null;
    if (protocolAssociations.size() == 0) {
      useDefaultProtocol = true;
    } else {
      useDefaultProtocol = false;
      for (Map.Entry<String, String> entry : protocolAssociations.entrySet()) {
        if (((ConfigurableReaderSpi) readerSpi).isCurrentProtocol(entry.getKey())) {
          currentProtocol = entry.getValue();
        }
      }
    }
  }

  /**
   * (private)<br>
   * This POJO contains the card selection status.
   */
  private static class SelectionStatus {

    private final String powerOnData;
    private final ApduResponseAdapter selectApplicationResponse;
    private final boolean hasMatched;

    /**
     * Constructor.
     *
     * @param powerOnData A String containing the power-on data (optional).
     * @param selectApplicationResponse The response to the select application command (optional).
     * @param hasMatched A boolean.
     */
    SelectionStatus(
        String powerOnData, ApduResponseAdapter selectApplicationResponse, boolean hasMatched) {
      this.powerOnData = powerOnData;
      this.selectApplicationResponse = selectApplicationResponse;
      this.hasMatched = hasMatched;
    }
  }
}
