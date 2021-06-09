/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
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

import org.calypsonet.terminal.card.ApduResponseApi;

/**
 * (package-private)<br>
 * This POJO contains the card selection status.
 *
 * @since 2.0
 */
class SelectionStatus {
  private final String powerOnData;
  private final ApduResponseApi selectApplicationResponse;
  private final boolean hasMatched;

  /**
   * Constructor.
   *
   * <p>Note: power-on data and select application response are optional but cannot both be null at
   * the same time when the selection has matched.
   *
   * @param powerOnData A String containing the power-on data (optional).
   * @param selectApplicationResponse The response to the select application command (optional).
   * @param hasMatched A boolean.
   * @since 2.0
   */
  SelectionStatus(
      String powerOnData, ApduResponseApi selectApplicationResponse, boolean hasMatched) {
    this.powerOnData = powerOnData;
    this.selectApplicationResponse = selectApplicationResponse;
    this.hasMatched = hasMatched;
  }

  /**
   * Gets the card's power-on data.
   *
   * @since 2.0
   */
  public String getPowerOnData() {
    return powerOnData;
  }

  /**
   * Gets the response to the select application command.
   *
   * @since 2.0
   */
  public ApduResponseApi getSelectApplicationResponse() {
    return selectApplicationResponse;
  }

  /**
   * Indicates if the selection has matched the selector filters.
   *
   * @since 2.0
   */
  public boolean hasMatched() {
    return hasMatched;
  }
}
