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

import java.util.Arrays;
import org.calypsonet.terminal.card.ApduResponseApi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * This POJO contains a set of data related to an ISO-7816 APDU response.
 *
 * @since 2.0
 */
public final class ApduResponseAdapter implements ApduResponseApi {

  private final byte[] bytes;
  private final int statusWord;

  /**
   * (package-private)<br>
   * Builds an APDU response from an array of bytes from the card, computes the status word.
   *
   * @param bytes A byte array
   * @since 2.0
   */
  ApduResponseAdapter(byte[] bytes) {
    this.bytes = bytes;
    statusWord =
        ((bytes[bytes.length - 2] & 0x000000FF) << 8) + (bytes[bytes.length - 1] & 0x000000FF);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int getStatusWord() {
    return statusWord;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public byte[] getBytes() {
    return this.bytes;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public byte[] getDataOut() {
    return Arrays.copyOfRange(this.bytes, 0, this.bytes.length - 2);
  }

  /**
   * Converts the APDU response into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0
   */
  @Override
  public String toString() {
    return "APDU_RESPONSE = " + JsonUtil.toJson(this);
  }
}
