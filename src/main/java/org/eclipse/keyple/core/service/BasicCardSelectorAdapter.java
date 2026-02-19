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

import org.eclipse.keypop.reader.selection.BasicCardSelector;

/**
 * Implementation of public {@link BasicCardSelector} API.
 *
 * @since 3.0.0
 */
final class BasicCardSelectorAdapter implements BasicCardSelector, InternalCardSelector {
  private String logicalProtocolName;
  private String powerOnDataRegex;

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
  public BasicCardSelector filterByCardProtocol(String logicalProtocolName) {
    this.logicalProtocolName = logicalProtocolName;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public BasicCardSelector filterByPowerOnData(String powerOnDataRegex) {
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
    return "BasicCardSelectorAdapter{"
        + "logicalProtocolName='"
        + logicalProtocolName
        + '\''
        + ", powerOnDataRegex='"
        + powerOnDataRegex
        + '\''
        + '}';
  }
}
