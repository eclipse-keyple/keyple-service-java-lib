/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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

import org.eclipse.keyple.core.service.selection.CardSelectionService;
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing;

/**
 * Factory of {@link CardSelectionService}
 *
 * @since 2.0
 */
public class CardSelectionServiceFactory {

  /**
   * Retrieves an instance of a {@link CardSelectionService} that stops as soon as a card matches.
   *
   * @return A not null reference
   * @since 2.0
   */
  CardSelectionService getService() {
    return null;
  }

  /**
   * Retrieves an instance of a {@link CardSelectionService} with the possibility to define whether
   * it stops or not after a card match.
   *
   * @param multiSelectionProcessing The multi selection processing policy.
   * @return A not null reference
   * @since 2.0
   */
  CardSelectionService getService(MultiSelectionProcessing multiSelectionProcessing) {
    return null;
  }
}
