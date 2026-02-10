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

import java.util.Arrays;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.card.ApduResponseApi;

/**
 * This POJO contains a set of data related to an ISO-7816 APDU response.
 *
 * @since 2.0.0
 */
final class ApduResponseAdapter implements ApduResponseApi {

  private final byte[] apdu;
  private final int statusWord;

  /**
   * Builds an APDU response from an array of bytes from the card, computes the status word.
   *
   * @param apdu An array of at least 2 bytes.
   * @since 2.0.0
   */
  ApduResponseAdapter(byte[] apdu) {
    this.apdu = apdu;
    statusWord = ((apdu[apdu.length - 2] & 0x000000FF) << 8) + (apdu[apdu.length - 1] & 0x000000FF);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public byte[] getApdu() {
    return this.apdu;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public byte[] getDataOut() {
    return Arrays.copyOfRange(this.apdu, 0, this.apdu.length - 2);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public int getStatusWord() {
    return statusWord;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return A string that represents the current state of the object.
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "ApduResponseAdapter{"
        + "apdu='"
        + HexUtil.toHex(apdu)
        + '\''
        + ", statusWord='"
        + HexUtil.toHex(statusWord)
        + '\''
        + '}';
  }
}
