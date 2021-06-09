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
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse;

/**
 * (package-private)<br>
 * POJO containing the card selection responses received during the card selection process.
 *
 * @since 2.0
 */
class ScheduledCardSelectionsResponseAdapter implements ScheduledCardSelectionsResponse {

  private final List<CardSelectionResponseApi> cardSelectionResponses;

  /**
   * (package-private)<br>
   * Constructor
   *
   * @param cardSelectionResponses The card selection responses.
   * @since 2.0
   */
  ScheduledCardSelectionsResponseAdapter(List<CardSelectionResponseApi> cardSelectionResponses) {
    this.cardSelectionResponses = cardSelectionResponses;
  }

  /**
   * (package-private)<br>
   * Gets the card responses.
   *
   * @return A list of {@link CardSelectionResponseApi}.
   * @since 2.0
   */
  List<CardSelectionResponseApi> getCardSelectionResponses() {
    return cardSelectionResponses;
  }
}
