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

import java.util.HashSet;
import java.util.Set;
import org.calypsonet.terminal.card.spi.ApduRequestSpi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * (package-private)<br>
 * Adapter of {@link ApduRequestSpi}
 *
 * @since 2.0.0
 */
class ApduRequestAdapter implements ApduRequestSpi {
  private static final int DEFAULT_SUCCESSFUL_CODE = 0x9000;

  private final byte[] apdu;
  private final Set<Integer> successfulStatusWords;
  private String info;

  /**
   * Builds an APDU request from a raw byte buffer.
   *
   * <p>The default status words list is initialized with the standard successful code 9000h.
   *
   * @param apdu An array of at least 4 bytes.
   * @since 2.0.0
   */
  public ApduRequestAdapter(byte[] apdu) {
    this.apdu = apdu;
    this.successfulStatusWords = new HashSet<Integer>();
    this.successfulStatusWords.add(DEFAULT_SUCCESSFUL_CODE);
  }

  /**
   * Adds a status word to the list of those that should be considered successful for the APDU.
   *
   * <p>Note: initially, the list contains the standard successful status word {@code 9000h}.
   *
   * @param successfulStatusWord A positive int &le; {@code FFFFh}.
   * @return The object instance.
   * @since 2.0.0
   */
  public ApduRequestAdapter addSuccessfulStatusWord(int successfulStatusWord) {
    this.successfulStatusWords.add(successfulStatusWord);
    return this;
  }

  /**
   * Optional information about the APDU request.
   *
   * <p>This string is essentially dedicated to improving the readability of the logs.
   *
   * @param info The request information (free text).
   * @return The object instance.
   * @since 2.0.0
   */
  public ApduRequestAdapter setInfo(final String info) {
    this.info = info;
    return this;
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
  public Set<Integer> getSuccessfulStatusWords() {
    return successfulStatusWords;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getInfo() {
    return info;
  }

  /**
   * Converts the APDU request into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "APDU_REQUEST = " + JsonUtil.toJson(this);
  }
}
