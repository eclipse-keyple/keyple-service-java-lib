/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.calypsonet.terminal.card.spi.ApduRequestSpi;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionSpi;
import org.calypsonet.terminal.card.spi.CardSelectorSpi;
import org.calypsonet.terminal.card.spi.SmartCardSpi;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * Contains internal DTOs used for serialization and deserialization processes.
 *
 * @since 2.1.1
 */
final class InternalDto {

  private InternalDto() {}

  /**
   * Local implementation of {@link CardSelection} and {@link CardSelectionSpi}.
   *
   * @since 2.1.1
   */
  static final class CardSelectionAdapter implements CardSelection, CardSelectionSpi {

    private CardSelectionRequest cardSelectionRequest;

    /**
     * Default constructor.
     *
     * @since 2.1.1
     */
    CardSelectionAdapter() {}

    /**
     * Builds a new instance using the provided source object.
     *
     * @param src The source.
     * @since 2.1.1
     */
    CardSelectionAdapter(CardSelectionSpi src) {
      this.cardSelectionRequest = new CardSelectionRequest(src.getCardSelectionRequest());
    }

    @Override
    public CardSelectionRequestSpi getCardSelectionRequest() {
      return cardSelectionRequest;
    }

    @Override
    public SmartCardSpi parse(CardSelectionResponseApi cardSelectionResponseApi) {
      throw new UnsupportedOperationException("Method not supported for internal DTO.");
    }

    @Override
    public String toString() {
      return "CARD_SELECTION = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Local implementation of {@link CardSelectionRequestSpi}.
   *
   * @since 2.1.1
   */
  static final class CardSelectionRequest implements CardSelectionRequestSpi {

    private CardSelector cardSelector;
    private CardRequest cardRequest;

    /**
     * Default constructor.
     *
     * @since 2.1.1
     */
    CardSelectionRequest() {}

    /**
     * Builds a new instance using the provided source object.
     *
     * @param src The source.
     * @since 2.1.1
     */
    CardSelectionRequest(CardSelectionRequestSpi src) {
      this.cardSelector = new CardSelector(src.getCardSelector());
      if (src.getCardRequest() != null) {
        this.cardRequest = new CardRequest(src.getCardRequest());
      }
    }

    @Override
    public CardSelectorSpi getCardSelector() {
      return cardSelector;
    }

    @Override
    public CardRequestSpi getCardRequest() {
      return cardRequest;
    }

    @Override
    public String toString() {
      return "CARD_SELECTION_REQUEST = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Local implementation of {@link CardSelectorSpi}.
   *
   * @since 2.1.1
   */
  static final class CardSelector implements CardSelectorSpi {

    private String cardProtocol;
    private String powerOnDataRegex;
    private byte[] aid;
    private FileOccurrence fileOccurrence;
    private FileControlInformation fileControlInformation;
    private Set<Integer> successfulSelectionStatusWords;

    /**
     * Default constructor.
     *
     * @since 2.1.1
     */
    CardSelector() {}

    /**
     * Builds a new instance using the provided source object.
     *
     * @param src The source.
     * @since 2.1.1
     */
    CardSelector(CardSelectorSpi src) {
      this.cardProtocol = src.getCardProtocol();
      this.powerOnDataRegex = src.getPowerOnDataRegex();
      this.aid = src.getAid();
      this.fileOccurrence = src.getFileOccurrence();
      this.fileControlInformation = src.getFileControlInformation();
      this.successfulSelectionStatusWords =
          new HashSet<Integer>(src.getSuccessfulSelectionStatusWords());
    }

    @Override
    public String getCardProtocol() {
      return cardProtocol;
    }

    @Override
    public String getPowerOnDataRegex() {
      return powerOnDataRegex;
    }

    @Override
    public byte[] getAid() {
      return aid;
    }

    @Override
    public FileOccurrence getFileOccurrence() {
      return fileOccurrence;
    }

    @Override
    public FileControlInformation getFileControlInformation() {
      return fileControlInformation;
    }

    @Override
    public Set<Integer> getSuccessfulSelectionStatusWords() {
      return successfulSelectionStatusWords;
    }

    @Override
    public String toString() {
      return "CARD_SELECTOR = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Local implementation of {@link CardRequestSpi}.
   *
   * @since 2.1.1
   */
  static final class CardRequest implements CardRequestSpi {

    private List<ApduRequest> apduRequests;
    private boolean stopOnUnsuccessfulStatusWord;

    /**
     * Default constructor.
     *
     * @since 2.1.1
     */
    CardRequest() {}

    /**
     * Builds a new instance using the provided source object.
     *
     * @param src The source.
     * @since 2.1.1
     */
    CardRequest(CardRequestSpi src) {
      this.apduRequests = new ArrayList<ApduRequest>(src.getApduRequests().size());
      for (ApduRequestSpi apduRequestSpi : src.getApduRequests()) {
        apduRequests.add(new ApduRequest(apduRequestSpi));
      }
      this.stopOnUnsuccessfulStatusWord = src.stopOnUnsuccessfulStatusWord();
    }

    @Override
    public List<ApduRequestSpi> getApduRequests() {
      return new ArrayList<ApduRequestSpi>(apduRequests);
    }

    @Override
    public boolean stopOnUnsuccessfulStatusWord() {
      return stopOnUnsuccessfulStatusWord;
    }

    @Override
    public String toString() {
      return "CARD_REQUEST = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Local implementation of {@link ApduRequestSpi}.
   *
   * @since 2.1.1
   */
  static class ApduRequest implements ApduRequestSpi {

    private byte[] apdu;
    private Set<Integer> successfulStatusWords;
    private String info;

    /**
     * Default constructor.
     *
     * @since 2.1.1
     */
    ApduRequest() {}

    /**
     * Builds a new instance using the provided source object.
     *
     * @param src The source.
     * @since 2.1.1
     */
    ApduRequest(ApduRequestSpi src) {
      this.apdu = src.getApdu().clone();
      this.successfulStatusWords = new HashSet<Integer>(src.getSuccessfulStatusWords());
      this.info = src.getInfo();
    }

    @Override
    public byte[] getApdu() {
      return apdu;
    }

    @Override
    public Set<Integer> getSuccessfulStatusWords() {
      return successfulStatusWords;
    }

    @Override
    public String getInfo() {
      return info;
    }

    @Override
    public String toString() {
      return "APDU_REQUEST = " + JsonUtil.toJson(this);
    }
  }
}
