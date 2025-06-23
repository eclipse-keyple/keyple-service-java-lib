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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

/**
 * Implementation of {@link CardSelectionResult}.
 *
 * @since 2.0.0
 */
final class CardSelectionResultAdapter implements CardSelectionResult {

  private int activeSelectionIndex = -1;
  private final Map<Integer, SmartCard> smartCardMap = new HashMap<>();

  /**
   * Constructor
   *
   * @since 2.0.0
   */
  CardSelectionResultAdapter() {}

  /**
   * Append a {@link SmartCard} to the internal list
   *
   * @param selectionIndex The index of the selection that resulted in the smart card.
   * @param smartCard The smart card.
   * @since 2.0.0
   */
  void addSmartCard(int selectionIndex, SmartCard smartCard) {
    smartCardMap.put(selectionIndex, smartCard);
    // keep the current selection index
    activeSelectionIndex = selectionIndex;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Map<Integer, SmartCard> getSmartCards() {
    return smartCardMap;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SmartCard getActiveSmartCard() {
    return smartCardMap.get(activeSelectionIndex);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public int getActiveSelectionIndex() {
    return activeSelectionIndex;
  }
}
