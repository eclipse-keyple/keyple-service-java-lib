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

import org.eclipse.keyple.core.service.selection.spi.SmartCard;
import org.eclipse.keyple.core.util.Assert;

/**
 * This POJO contains a {@link SmartCard} and its associated {@link Reader}.
 *
 * @since 2.0
 */
public class CardResource {
  private final Reader reader;
  private final SmartCard smartCard;

  /**
   * Creates an instance of {@link CardResource}.
   *
   * @param reader The {@link Reader}.
   * @param smartCard The {@link SmartCard}.
   * @since 2.0
   */
  public CardResource(Reader reader, SmartCard smartCard) {

    Assert.getInstance().notNull(reader, "reader").notNull(smartCard, "smartCard");

    this.reader = reader;
    this.smartCard = smartCard;
  }

  /**
   * Gets the reader
   *
   * @return A not null reference.
   * @since 2.0
   */
  public Reader getReader() {
    return reader;
  }

  /**
   * Gets the {@link SmartCard}.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public SmartCard getSmartCard() {
    return smartCard;
  }
}
