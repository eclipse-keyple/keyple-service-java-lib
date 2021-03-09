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

import java.util.Map;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;

/**
 * Defines the result of a selection process.
 *
 * @since 2.0
 */
public interface CardSelectionResult {

  /**
   * Tells if the current selection process resulted in an active selection.
   *
   * @return true if an active selection is present
   * @since 2.0
   */
  boolean hasActiveSelection();

  /**
   * Gets the index of the active selection if any.
   *
   * @return A positive int.
   * @throws IllegalStateException if there is no active selection
   * @since 2.0
   */
  int getActiveSelectionIndex();

  /**
   * Get the matching status of a selection case for which the index is provided.
   *
   * <p>Checks for the presence of an entry in the SmartCard Map for the given index.
   *
   * @param selectionIndex The selection index.
   * @return true if the selection has matched
   * @since 2.0
   */
  boolean hasSelectionMatched(int selectionIndex);

  /**
   * Gets all the {@link SmartCard} corresponding to all selection cases in a map where the key is
   * the selection index.
   *
   * @return A map.
   * @since 2.0
   */
  Map<Integer, SmartCard> getSmartCards();

  /**
   * Gets the {@link SmartCard} for the specified index.
   *
   * <p>Returns null if no {@link SmartCard} was found.
   *
   * @param selectionIndex the selection index
   * @return the {@link SmartCard} or null
   * @since 2.0
   */
  SmartCard getSmartCard(int selectionIndex);

  /**
   * Get the active matching card. I.e. the card that has been selected. <br>
   * The hasActiveSelection method should be invoked before.
   *
   * @return The currently active matching card.
   * @throws IllegalStateException if no active matching card is found
   * @since 2.0
   */
  SmartCard getActiveSmartCard();
}
