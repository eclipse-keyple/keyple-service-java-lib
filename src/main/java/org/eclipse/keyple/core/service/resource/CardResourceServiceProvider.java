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
package org.eclipse.keyple.core.service.resource;

/**
 * Provider of the {@link CardResourceService}.
 *
 * @since 2.0
 */
public final class CardResourceServiceProvider {

  /** Private constructor */
  private CardResourceServiceProvider() {}

  /**
   * Gets the unique instance of {@link CardResourceService}.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public static CardResourceService getService() {
    return CardResourceServiceAdapter.getInstance();
  }
}
