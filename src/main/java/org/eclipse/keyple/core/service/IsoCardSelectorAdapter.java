/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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

import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.reader.selection.IsoCardSelector;

/**
 * Implementation of public {@link IsoCardSelector} API.
 *
 * @since 3.0.0
 */
final class IsoCardSelectorAdapter implements IsoCardSelector, InternalIsoCardSelector {
  private String logicalProtocolName;
  private String powerOnDataRegex;
  private byte[] aid;
  private FileOccurrence fileOccurrence = FileOccurrence.FIRST; // default value: FIRST
  private FileControlInformation fileControlInformation =
      FileControlInformation.FCI; // default value: FCI

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public String getLogicalProtocolName() {
    return logicalProtocolName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public String getPowerOnDataRegex() {
    return powerOnDataRegex;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public byte[] getAid() {
    return aid;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public FileOccurrence getFileOccurrence() {
    return fileOccurrence;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public FileControlInformation getFileControlInformation() {
    return fileControlInformation;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector filterByDfName(byte[] aid) {
    this.aid = aid;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector filterByDfName(String aid) {
    this.aid = HexUtil.toByteArray(aid);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector setFileOccurrence(FileOccurrence fileOccurrence) {
    this.fileOccurrence = fileOccurrence;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector setFileControlInformation(FileControlInformation fileControlInformation) {
    this.fileControlInformation = fileControlInformation;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector filterByCardProtocol(String logicalProtocolName) {
    this.logicalProtocolName = logicalProtocolName;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector filterByPowerOnData(String powerOnDataRegex) {
    this.powerOnDataRegex = powerOnDataRegex;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public String toString() {
    return "IsoCardSelectorAdapter{"
        + "logicalProtocolName='"
        + logicalProtocolName
        + '\''
        + ", powerOnDataRegex='"
        + powerOnDataRegex
        + '\''
        + ", aid='"
        + HexUtil.toHex(aid)
        + '\''
        + ", fileOccurrence="
        + fileOccurrence
        + ", fileControlInformation="
        + fileControlInformation
        + '}';
  }
}
