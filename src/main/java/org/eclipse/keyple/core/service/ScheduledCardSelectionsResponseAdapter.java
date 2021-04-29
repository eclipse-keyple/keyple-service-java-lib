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
import org.eclipse.keyple.core.card.CardSelectionResponse;
import org.eclipse.keyple.core.service.selection.ScheduledCardSelectionsResponse;

/**
 * (package-private)<br>
 * POJO containing the card selection responses received during the card selection process.
 *
 * @since 2.0
 */
class ScheduledCardSelectionsResponseAdapter implements ScheduledCardSelectionsResponse {

  private final List<CardSelectionResponse> cardSelectionResponses;

  /**
   * (package-private)<br>
   * Constructor
   *
   * @param cardSelectionResponses The card selection responses.
   * @since 2.0
   */
  ScheduledCardSelectionsResponseAdapter(List<CardSelectionResponse> cardSelectionResponses) {
    this.cardSelectionResponses = cardSelectionResponses;
  }

  /**
   * (package-private)<br>
   * Gets the card responses.
   *
   * @return A list of {@link CardSelectionResponse}.
   * @since 2.0
   */
  List<CardSelectionResponse> getCardSelectionResponses() {
    return cardSelectionResponses;
  }
}
