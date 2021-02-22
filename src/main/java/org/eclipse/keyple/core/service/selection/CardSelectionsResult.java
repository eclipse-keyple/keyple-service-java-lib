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
package org.eclipse.keyple.core.service.selection;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;

/**
 * Contains the result of a selection process.
 *
 * <p>Embeds a map of {@link SmartCard}. At most one of these matching card is active.<br>
 * Provides a set of methods to retrieve the active selection (getActiveSmartCard) or a particular
 * matching card specified by its index.
 *
 * @since 2.0
 */
public final class CardSelectionsResult {
  private Integer activeSelectionIndex = null;
  private final Map<Integer, SmartCard> smartCardMap = new HashMap<Integer, SmartCard>();

  /**
   * (package-private)<br>
   * Constructor
   */
  CardSelectionsResult() {}

  /**
   * (package-private)<br>
   * Append a {@link SmartCard} to the internal list
   *
   * @param selectionIndex the index of the selection that resulted in the matching card
   * @param smartCard the matching card to add
   * @param isSelected true if the currently added matching card is selected (its logical channel is
   *     open) TODO check if isSelected should ne renamed to isActive
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
   * Tells if the current selection process resulted in an active selection.
   *
   * @return true if an active selection is present
   * @since 2.0
   */
  public boolean hasActiveSelection() {
    return activeSelectionIndex != null;
  }
  /**
   * Gets the index of the active selection
   *
   * @return the index as an int
   * @throws IllegalStateException if there is no active selection
   * @since 2.0
   */
  public int getActiveSelectionIndex() {
    if (hasActiveSelection()) {
      return activeSelectionIndex;
    }
    throw new IllegalStateException("No active Matching card is available");
  }

  /**
   * Get the matching status of a selection case for which the index is provided. <br>
   * Checks for the presence of an entry in the SmartCard Map for the given index
   *
   * @param selectionIndex the selection index
   * @return true if the selection has matched
   * @since 2.0
   */
  public boolean hasSelectionMatched(int selectionIndex) {
    return smartCardMap.containsKey(selectionIndex);
  }

  /**
   * Gets all the {@link SmartCard} corresponding to all selection cases in a map where the key is
   * the selection index.
   *
   * @return A map
   * @since 2.0
   */
  public Map<Integer, SmartCard> getSmartCards() {
    return smartCardMap;
  }

  /**
   * Gets the {@link SmartCard} for the specified index.
   *
   * <p>Returns null if no {@link SmartCard} was found.
   *
   * @param selectionIndex the selection index
   * @return the {@link SmartCard} or null
   * @since 2.0
   */
  public SmartCard getSmartCard(int selectionIndex) {
    return smartCardMap.get(selectionIndex);
  }

  /**
   * Get the active matching card. I.e. the card that has been selected. <br>
   * The hasActiveSelection method should be called before.
   *
   * @return the currently active matching card
   * @throws IllegalStateException if no active matching card is found
   * @since 2.0
   */
  public SmartCard getActiveSmartCard() {
    SmartCard smartCard = smartCardMap.get(activeSelectionIndex);
    if (smartCard == null) {
      throw new IllegalStateException("No active Matching card is available");
    }
    return smartCard;
  }
}
