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
 * Card Resource Management Service.
 *
 * <p>Provides the means to define and manage an arbitrary number of {@link CardResource} that can
 * be accessed later by the application using the profile names it has chosen and to which the
 * CardResources will be associated.
 *
 * <p>The creation of the {@link CardResource} can be static or dynamic, with various allocation
 * strategy options depending on the parameters specified at configuration time (see {@link
 * CardResourceServiceConfigurator}).
 *
 * <p>The concept of dynamic creation of {@link CardResource} comes in two forms:
 *
 * <ul>
 *   <li>with a dynamic allocation of readers from a {@link PoolPlugin},
 *   <li>with the internally managed observation mechanisms of {@link ObservablePlugin} and {@link
 *       ObservableReader}.
 * </ul>
 *
 * @since 2.0
 */
public interface CardResourceService {

  /**
   * Gets the configuration builder to setup the service.
   *
   * @return A not null reference.
   * @since 2.0
   */
  CardResourceServiceConfigurator getConfigurator();

  /**
   * Starts the service using the current configuration, initializes the list of card resources,
   * activates the required monitoring, if any.
   *
   * @throws IllegalStateException If no configuration was done.
   * @since 2.0
   */
  void start();

  /**
   * Stops the service.
   *
   * <p>All monitoring processes are stopped, all card resources are released.
   *
   * @since 2.0
   */
  void stop();

  /**
   * Gets the current card resources available for the provided card resource profile name.
   *
   * @param cardResourceProfileName The name of the card resource profile.
   * @return An empty list if no card resource is available.
   * @throws IllegalStateException If the service is not started.
   * @since 2.0
   */
  List<CardResource> getCardResources(String cardResourceProfileName);
}
