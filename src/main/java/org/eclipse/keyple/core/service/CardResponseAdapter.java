/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
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
import org.calypsonet.terminal.card.ApduResponseApi;
import org.calypsonet.terminal.card.CardResponseApi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * This POJO contains an ordered list of the responses received following a card request and
 * indicators related to the status of the channel and the completion of the card request.
 *
 * @see org.calypsonet.terminal.card.spi.CardRequestSpi
 * @since 2.0.0
 */
final class CardResponseAdapter implements CardResponseApi {

  private final List<ApduResponseAdapter> apduResponses;
  private final boolean isLogicalChannelOpen;

  /**
   * Builds a card response from all {@link ApduResponseApi} received from the card and booleans
   * indicating if the logical channel is still open.
   *
   * @param apduResponses A not null list.
   * @param isLogicalChannelOpen true if the logical channel is open, false if not.
   * @since 2.0.0
   */
  CardResponseAdapter(List<ApduResponseAdapter> apduResponses, boolean isLogicalChannelOpen) {

    this.apduResponses = apduResponses;
    this.isLogicalChannelOpen = isLogicalChannelOpen;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public List<ApduResponseApi> getApduResponses() {
    return new ArrayList<ApduResponseApi>(apduResponses);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public boolean isLogicalChannelOpen() {
    return isLogicalChannelOpen;
  }

  /**
   * Converts the card response into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "CARD_RESPONSE = " + JsonUtil.toJson(this);
  }
}
