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

import org.eclipse.keypop.reader.selection.CommonIsoCardSelector;

/**
 * Internal interface defining the package private getters of the {@link
 * org.eclipse.keypop.reader.selection.IsoCardSelector}.
 *
 * @since 3.0.0
 */
interface InternalIsoCardSelector extends InternalCardSelector {
  /**
   * Gets the ISO7816-4 Application Identifier (AID).
   *
   * @return Null if no AID has been set.
   * @since 3.0.0
   */
  byte[] getAid();

  /**
   * Gets the {@link CommonIsoCardSelector.FileOccurrence} parameter defining the navigation within
   * the card applications list.
   *
   * @return A not null reference.
   * @since 3.0.0
   */
  CommonIsoCardSelector.FileOccurrence getFileOccurrence();

  /**
   * Gets the {@link CommonIsoCardSelector.FileControlInformation} parameter defining the output
   * type of the select command.
   *
   * @return A not null reference.
   * @since 3.0.0
   */
  CommonIsoCardSelector.FileControlInformation getFileControlInformation();
}
