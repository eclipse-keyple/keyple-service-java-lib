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

import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;

/**
 * This POJO contains a generic {@link SmartCard} and its associated {@link Reader}.
 *
 * @since 2.0
 */
public class CardResource<T extends SmartCard> {
  private final Reader reader;
  private final T smartCard;

  /**
   * Constructor
   *
   * @param reader the {@link Reader} with which the card is communicating
   * @param smartCard the {@link SmartCard} information structure
   * @since 2.0
   */
  public CardResource(Reader reader, T smartCard) {
    this.reader = reader;
    this.smartCard = smartCard;
  }

  /**
   * Gets the reader
   *
   * @return the current {@link Reader} for this card
   * @since 2.0
   */
  public Reader getReader() {
    return reader;
  }

  /**
   * Gets the card
   *
   * @return the {@link SmartCard}
   * @since 2.0
   */
  public T getSmartCard() {
    return smartCard;
  }
}
