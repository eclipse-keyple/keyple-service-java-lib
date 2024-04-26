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
import java.util.List;
import java.util.Set;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.spi.ApduRequestSpi;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.CommonIsoCardSelector;

/**
 * Contains internal legacy DTOs used for serialization and deserialization processes.
 *
 * <p>They are compliant with Core JSON API level 1 and 0.
 *
 * @since 3.0.0
 */
final class InternalLegacyDto {

  private InternalLegacyDto() {}

  static List<LegacyCardSelectionRequest> mapToLegacyCardSelectionRequests(
      List<CardSelector<?>> cardSelectors, List<CardSelectionRequestSpi> cardSelectionRequests) {
    List<LegacyCardSelectionRequest> result = new ArrayList<>(cardSelectors.size());
    for (int i = 0; i < cardSelectors.size(); i++) {
      result.add(
          mapToLegacyCardSelectionRequest(cardSelectors.get(i), cardSelectionRequests.get(i)));
    }
    return result;
  }

  static LegacyCardSelectionRequest mapToLegacyCardSelectionRequest(
      CardSelector<?> cardSelector, CardSelectionRequestSpi cardSelectionRequestSpi) {
    LegacyCardSelectionRequest result = new LegacyCardSelectionRequest();
    result.cardRequest =
        cardSelectionRequestSpi.getCardRequest() != null
            ? mapToLegacyCardRequest(cardSelectionRequestSpi.getCardRequest())
            : null;
    result.cardSelector = mapToLegacyCardSelector(cardSelector, cardSelectionRequestSpi);
    return result;
  }

  static LegacyCardSelector mapToLegacyCardSelector(
      CardSelector<?> cardSelector, CardSelectionRequestSpi cardSelectionRequestSpi) {
    LegacyCardSelector result = new LegacyCardSelector();
    InternalCardSelector basicCardSelector = (InternalCardSelector) cardSelector;
    result.cardProtocol = basicCardSelector.getLogicalProtocolName();
    result.powerOnDataRegex = basicCardSelector.getPowerOnDataRegex();
    if (cardSelector instanceof InternalIsoCardSelector) {
      InternalIsoCardSelector isoCardSelector = (InternalIsoCardSelector) cardSelector;
      result.aid = isoCardSelector.getAid();
      result.fileOccurrence = isoCardSelector.getFileOccurrence();
      result.fileControlInformation = isoCardSelector.getFileControlInformation();
    } else {
      result.aid = null;
      result.fileOccurrence = CommonIsoCardSelector.FileOccurrence.FIRST;
      result.fileControlInformation = CommonIsoCardSelector.FileControlInformation.FCI;
    }
    result.successfulSelectionStatusWords =
        cardSelectionRequestSpi.getSuccessfulSelectionStatusWords();
    return result;
  }

  static LegacyCardRequest mapToLegacyCardRequest(CardRequestSpi cardRequest) {
    LegacyCardRequest result = new LegacyCardRequest();
    result.apduRequests = mapToLegacyApduRequests(cardRequest.getApduRequests());
    result.stopOnUnsuccessfulStatusWord = cardRequest.stopOnUnsuccessfulStatusWord();
    return result;
  }

  static List<LegacyApduRequest> mapToLegacyApduRequests(List<ApduRequestSpi> apduRequests) {
    List<LegacyApduRequest> result = new ArrayList<>(apduRequests.size());
    for (ApduRequestSpi apduRequestSpi : apduRequests) {
      result.add(mapToLegacyApduRequest(apduRequestSpi));
    }
    return result;
  }

  static LegacyApduRequest mapToLegacyApduRequest(ApduRequestSpi apduRequestSpi) {
    LegacyApduRequest result = new LegacyApduRequest();
    result.apdu = apduRequestSpi.getApdu();
    result.info = apduRequestSpi.getInfo();
    result.successfulStatusWords = apduRequestSpi.getSuccessfulStatusWords();
    return result;
  }

  /**
   * @since 2.1.1
   */
  static final class LegacyCardSelectionRequest {
    LegacyCardSelector cardSelector;
    LegacyCardRequest cardRequest;

    @Override
    public String toString() {
      return "CARD_SELECTION_REQUEST = " + JsonUtil.toJson(this);
    }
  }

  /**
   * @since 2.1.1
   */
  static final class LegacyCardSelector {
    String cardProtocol;
    String powerOnDataRegex;
    byte[] aid;
    CommonIsoCardSelector.FileOccurrence fileOccurrence;
    CommonIsoCardSelector.FileControlInformation fileControlInformation;
    Set<Integer> successfulSelectionStatusWords;

    @Override
    public String toString() {
      return "CARD_SELECTOR = " + JsonUtil.toJson(this);
    }
  }

  /**
   * @since 2.1.1
   */
  static final class LegacyCardRequest {
    List<LegacyApduRequest> apduRequests;
    boolean stopOnUnsuccessfulStatusWord;

    @Override
    public String toString() {
      return "CARD_REQUEST = " + JsonUtil.toJson(this);
    }
  }

  /**
   * @since 2.1.1
   */
  static class LegacyApduRequest {
    byte[] apdu;
    Set<Integer> successfulStatusWords;
    String info;

    @Override
    public String toString() {
      return "APDU_REQUEST = " + JsonUtil.toJson(this);
    }
  }
}
