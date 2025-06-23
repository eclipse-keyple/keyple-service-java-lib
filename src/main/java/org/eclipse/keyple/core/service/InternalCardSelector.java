/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

/**
 * Internal interface defining the package private getters of the {@link
 * org.eclipse.keypop.reader.selection.CardSelector}.
 *
 * @since 3.0.0
 */
interface InternalCardSelector {
  /**
   * Gets the logical card protocol name.
   *
   * @return Null if no card protocol has been set.
   * @since 3.0.0
   */
  String getLogicalProtocolName();

  /**
   * Gets the regular expression to be applied to the card's power-on data.
   *
   * @return Null if no power-on data regex has been set.
   * @since 3.0.0
   */
  String getPowerOnDataRegex();
}
