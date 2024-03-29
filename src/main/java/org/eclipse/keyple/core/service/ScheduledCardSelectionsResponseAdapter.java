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

import java.util.List;
import org.eclipse.keypop.card.CardSelectionResponseApi;
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse;

/**
 * POJO containing the card selection responses received during the card selection process.
 *
 * @since 2.0.0
 */
class ScheduledCardSelectionsResponseAdapter implements ScheduledCardSelectionsResponse {

  private final List<CardSelectionResponseApi> cardSelectionResponses;

  /**
   * Constructor
   *
   * @param cardSelectionResponses The card selection responses.
   * @since 2.0.0
   */
  ScheduledCardSelectionsResponseAdapter(List<CardSelectionResponseApi> cardSelectionResponses) {
    this.cardSelectionResponses = cardSelectionResponses;
  }

  /**
   * Gets the card responses.
   *
   * @return A list of {@link CardSelectionResponseApi}.
   * @since 2.0.0
   */
  List<CardSelectionResponseApi> getCardSelectionResponses() {
    return cardSelectionResponses;
  }
}
