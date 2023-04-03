/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
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

import org.calypsonet.terminal.card.ApduResponseApi;
import org.calypsonet.terminal.card.CardResponseApi;
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * This POJO contains the data from a card obtained in response to a card selection request.
 *
 * <p>These data are the selection status and the responses, if any, to the additional APDUs sent to
 * the card ({@link CardResponseApi}).
 *
 * @see org.calypsonet.terminal.card.spi.CardSelectionRequestSpi
 * @since 2.0.0
 */
final class CardSelectionResponseAdapter implements CardSelectionResponseApi {

  private final boolean hasMatched;
  private final String powerOnData;
  private final ApduResponseAdapter selectApplicationResponse;
  private final CardResponseAdapter cardResponse;

  /**
   * Builds a card selection response including the selection status and a {@link CardResponseApi}
   * (list of {@link org.calypsonet.terminal.card.ApduResponseApi}).
   *
   * @param powerOnData The card power-on data, null if the power-on data is not available.
   * @param selectApplicationResponse The response to the Select Application command, null if no
   *     Select Application command was performed...
   * @param hasMatched True if the card inserted matches the selection filters.
   * @param cardResponse null if no card response is available.
   * @since 2.0.0
   */
  CardSelectionResponseAdapter(
      String powerOnData,
      ApduResponseAdapter selectApplicationResponse,
      boolean hasMatched,
      CardResponseAdapter cardResponse) {
    this.powerOnData = powerOnData;
    this.selectApplicationResponse = selectApplicationResponse;
    this.hasMatched = hasMatched;
    this.cardResponse = cardResponse;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getPowerOnData() {
    // RL-ATR-IDENTIFY.1
    return powerOnData;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public ApduResponseApi getSelectApplicationResponse() {
    return selectApplicationResponse;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public boolean hasMatched() {
    return hasMatched;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardResponseApi getCardResponse() {
    return cardResponse;
  }

  /**
   * Converts the card selection response into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "CARD_SELECTION_RESPONSE = " + JsonUtil.toJson(this);
  }
}
