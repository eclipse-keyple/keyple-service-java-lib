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

import java.util.HashMap;
import java.util.Map;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;

/**
 * (package-private)<br>
 * Implementation of {@link CardSelectionResult}.
 *
 * @since 2.0
 */
final class CardSelectionResultAdapter implements CardSelectionResult {

  private int activeSelectionIndex = -1;
  private final Map<Integer, SmartCard> smartCardMap = new HashMap<Integer, SmartCard>();

  /**
   * (package-private)<br>
   * Constructor
   *
   * @since 2.0
   */
  CardSelectionResultAdapter() {}

  /**
   * (package-private)<br>
   * Append a {@link SmartCard} to the internal list
   *
   * @param selectionIndex the index of the selection that resulted in the matching card
   * @param smartCard the matching card to add
   * @param isSelected true if the currently added matching card is selected (its logical channel is
   *     open)
   * @since 2.0
   */
  void addSmartCard(int selectionIndex, SmartCard smartCard, boolean isSelected) {
    if (smartCard != null) smartCardMap.put(selectionIndex, smartCard);
    // if the current selection is active, we keep its index
    if (isSelected) {
      activeSelectionIndex = selectionIndex;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Map<Integer, SmartCard> getSmartCards() {
    return smartCardMap;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public SmartCard getActiveSmartCard() {
    SmartCard smartCard = smartCardMap.get(activeSelectionIndex);
    if (smartCard == null) {
      throw new IllegalStateException("No active matching card is available");
    }
    return smartCard;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int getActiveSelectionIndex() {
    return activeSelectionIndex;
  }
}
