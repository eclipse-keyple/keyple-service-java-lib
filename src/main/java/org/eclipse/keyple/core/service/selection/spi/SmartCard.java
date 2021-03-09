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
package org.eclipse.keyple.core.service.selection.spi;

import org.eclipse.keyple.core.common.KeypleSmartCard;

/**
 * Defines a generic smart card that has established a communication with a reader after a selection
 * process.
 *
 * <p>Make available the information that may have been collected by the selection process, i.e. the
 * Answer to Reset (ATR) and the Answer to Selection Command (FCI).<br>
 * Both are optional.
 *
 * <p>Must be implemented and possibly extended by a card extension to meet its specific needs.
 *
 * @since 2.0
 */
public interface SmartCard extends KeypleSmartCard {

  /**
   * Tells if the card provided a FCI.
   *
   * @return true if the card has an FCI
   * @since 2.0
   */
  boolean hasFci();

  /**
   * Gets the FCI.
   *
   * @return A not null reference.
   * @throws IllegalStateException if no FCI is available (see hasFci)
   * @since 2.0
   */
  byte[] getFciBytes();

  /**
   * Tells if the card provided an ATR
   *
   * @return true if the card has an ATR
   * @since 2.0
   */
  boolean hasAtr();

  /**
   * Gets the ATR
   *
   * @return A not null reference.
   * @throws IllegalStateException if no ATR is available (see hasAtr)
   * @since 2.0
   */
  byte[] getAtrBytes();
}
