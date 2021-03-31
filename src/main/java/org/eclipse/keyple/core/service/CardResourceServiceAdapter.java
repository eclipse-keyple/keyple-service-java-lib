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

import java.util.List;

/**
 * (package-private)<br>
 * Implementation of {@link SmartCardService}.
 *
 * @since 2.0
 */
final class CardResourceServiceAdapter implements CardResourceService {

  /** singleton instance of CardResourceServiceAdapter */
  private static final CardResourceServiceAdapter uniqueInstance = new CardResourceServiceAdapter();

  /**
   * (package-private)<br>
   * Gets the single instance of CardResourceServiceAdapter.
   *
   * @return The instance of CardResourceServiceAdapter.
   * @since 2.0
   */
  static CardResourceServiceAdapter getInstance() {
    return uniqueInstance;
  }

  /**
   * (package-private)<br>
   *
   * @since 2.0
   */
  void configure(CardResourceServiceConfiguratorAdapter configurator) {}

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator getConfigurator() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void start() {}

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void stop() {}

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public List<CardResource> getCardResources(String cardResourceProfileName) {
    return null;
  }
}
