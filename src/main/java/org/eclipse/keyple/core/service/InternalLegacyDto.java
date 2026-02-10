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
import org.eclipse.keyple.core.util.HexUtil;
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

  static List<LegacyCardSelectionRequestV0> mapToLegacyCardSelectionRequestsV0(
      List<CardSelector<?>> cardSelectors, List<CardSelectionRequestSpi> cardSelectionRequests) {
    List<LegacyCardSelectionRequestV0> result = new ArrayList<>(cardSelectors.size());
    for (int i = 0; i < cardSelectors.size(); i++) {
      result.add(
          mapToLegacyCardSelectionRequestV0(cardSelectors.get(i), cardSelectionRequests.get(i)));
    }
    return result;
  }

  static List<LegacyCardSelectionRequestV1> mapToLegacyCardSelectionRequestsV1(
      List<CardSelector<?>> cardSelectors, List<CardSelectionRequestSpi> cardSelectionRequests) {
    List<LegacyCardSelectionRequestV1> result = new ArrayList<>(cardSelectors.size());
    for (int i = 0; i < cardSelectors.size(); i++) {
      result.add(
          mapToLegacyCardSelectionRequestV1(cardSelectors.get(i), cardSelectionRequests.get(i)));
    }
    return result;
  }

  static LegacyCardSelectionRequestV0 mapToLegacyCardSelectionRequestV0(
      CardSelector<?> cardSelector, CardSelectionRequestSpi cardSelectionRequestSpi) {
    LegacyCardSelectionRequestV0 result = new LegacyCardSelectionRequestV0();
    result.cardRequest =
        cardSelectionRequestSpi.getCardRequest() != null
            ? mapToLegacyCardRequestV0(cardSelectionRequestSpi.getCardRequest())
            : null;
    result.cardSelector = mapToLegacyCardSelector(cardSelector, cardSelectionRequestSpi);
    return result;
  }

  static LegacyCardSelectionRequestV1 mapToLegacyCardSelectionRequestV1(
      CardSelector<?> cardSelector, CardSelectionRequestSpi cardSelectionRequestSpi) {
    LegacyCardSelectionRequestV1 result = new LegacyCardSelectionRequestV1();
    result.cardRequest =
        cardSelectionRequestSpi.getCardRequest() != null
            ? mapToLegacyCardRequestV1(cardSelectionRequestSpi.getCardRequest())
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

  static LegacyCardRequestV0 mapToLegacyCardRequestV0(CardRequestSpi cardRequest) {
    LegacyCardRequestV0 result = new LegacyCardRequestV0();
    result.apduRequests = mapToLegacyApduRequests(cardRequest.getApduRequests());
    result.isStatusCodesVerificationEnabled = cardRequest.stopOnUnsuccessfulStatusWord();
    return result;
  }

  static LegacyCardRequestV1 mapToLegacyCardRequestV1(CardRequestSpi cardRequest) {
    LegacyCardRequestV1 result = new LegacyCardRequestV1();
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
   * @since 3.3.1
   */
  static final class LegacyCardSelectionRequestV0 {
    LegacyCardSelector cardSelector;
    LegacyCardRequestV0 cardRequest;

    @Override
    public String toString() {
      return "LegacyCardSelectionRequestV0{"
          + "cardSelector="
          + cardSelector
          + ", cardRequest="
          + cardRequest
          + '}';
    }
  }

  /**
   * @since 2.1.1
   */
  static final class LegacyCardSelectionRequestV1 {
    LegacyCardSelector cardSelector;
    LegacyCardRequestV1 cardRequest;

    @Override
    public String toString() {
      return "LegacyCardSelectionRequestV1{"
          + "cardSelector="
          + cardSelector
          + ", cardRequest="
          + cardRequest
          + '}';
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
      return "LegacyCardSelector{"
          + "cardProtocol='"
          + cardProtocol
          + '\''
          + ", powerOnDataRegex='"
          + powerOnDataRegex
          + '\''
          + ", aid='"
          + HexUtil.toHex(aid)
          + '\''
          + ", fileOccurrence="
          + fileOccurrence
          + ", fileControlInformation="
          + fileControlInformation
          + ", successfulSelectionStatusWords="
          + JsonUtil.toJson(successfulSelectionStatusWords)
          + '}';
    }
  }

  /**
   * @since 3.3.1
   */
  static final class LegacyCardRequestV0 {
    List<LegacyApduRequest> apduRequests;
    boolean isStatusCodesVerificationEnabled;

    @Override
    public String toString() {
      return "LegacyCardRequestV0{"
          + "apduRequests="
          + apduRequests
          + ", isStatusCodesVerificationEnabled="
          + isStatusCodesVerificationEnabled
          + '}';
    }
  }

  /**
   * @since 2.1.1
   */
  static final class LegacyCardRequestV1 {
    List<LegacyApduRequest> apduRequests;
    boolean stopOnUnsuccessfulStatusWord;

    @Override
    public String toString() {
      return "LegacyCardRequestV1{"
          + "apduRequests="
          + apduRequests
          + ", stopOnUnsuccessfulStatusWord="
          + stopOnUnsuccessfulStatusWord
          + '}';
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
      return "LegacyApduRequest{"
          + "apdu='"
          + HexUtil.toHex(apdu)
          + '\''
          + ", successfulStatusWords="
          + JsonUtil.toJson(successfulStatusWords)
          + ", info='"
          + info
          + '\''
          + '}';
    }
  }
}
