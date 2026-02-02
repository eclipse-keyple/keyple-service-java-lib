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
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.ApduResponseApi;

/**
 * This POJO contains a set of data related to an ISO-7816 APDU response.
 *
 * @since 2.0.0
 */
final class ApduResponseAdapter implements ApduResponseApi {

  private final byte[] apdu;
  private final int statusWord;
  private final Integer responseTime;

  /**
   * Builds an APDU response from an array of bytes from the card, computes the status word.
   *
   * @param apdu An array of at least 2 bytes.
   * @since 2.0.0
   */
  ApduResponseAdapter(byte[] apdu) {
    this(apdu, null);
  }

  /**
   * Builds an APDU response from an array of bytes from the card, computes the status word.
   *
   * @param apdu An array of at least 2 bytes.
   * @param responseTime The response time in milliseconds (can be null).
   * @since 3.4.0
   */
  ApduResponseAdapter(byte[] apdu, Integer responseTime) {
    this.apdu = apdu;
    this.statusWord =
        ((apdu[apdu.length - 2] & 0x000000FF) << 8) + (apdu[apdu.length - 1] & 0x000000FF);
    this.responseTime = responseTime;
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
   * {@inheritDoc}
   *
   * @since 3.4.0
   */
  @Override
  public Integer getResponseTime() {
    return responseTime;
  }

  /**
   * Converts the APDU response into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "APDU_RESPONSE = " + JsonUtil.toJson(this);
  }
}
