/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
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
import org.calypsonet.terminal.card.SelectionStatusApi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * This POJO contains the card selection status.
 *
 * @since 2.0
 */
public class SelectionStatusAdapter implements SelectionStatusApi {
  private final byte[] powerOnData;
  private final ApduResponseApi fci;
  private final boolean hasMatched;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * <p>Note: power-on data and FCI are optional but cannot both be null at the same time when the
   * selection has matched.
   *
   * @param powerOnData The power-on data (optional).
   * @param fci The answer to select (optional).
   * @param hasMatched A boolean.
   * @throws IllegalStateException if hasMatched true and both power-on data and fci are null.
   * @since 2.0
   */
  SelectionStatusAdapter(byte[] powerOnData, ApduResponseApi fci, boolean hasMatched) {
    this.powerOnData = powerOnData;
    this.fci = fci;
    this.hasMatched = hasMatched;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public byte[] getPowerOnData() {
    return powerOnData;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public ApduResponseApi getFci() {
    return fci;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public boolean hasMatched() {
    return hasMatched;
  }

  /**
   * Converts the selection status into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0
   */
  @Override
  public String toString() {
    return "SELECTION_STATUS = " + JsonUtil.toJson(this);
  }
}
