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
   * <p>The service is restarted if it is already started.
   *
   * @throws IllegalStateException If no configuration was done.
   * @since 2.0
   */
  void start();

  /**
   * Stops the service if it is started.
   *
   * <p>All monitoring processes are stopped, all card resources are released.
   *
   * @since 2.0
   */
  void stop();

  /**
   * Gets the first card resource available for the provided card resource profile name using the
   * configured allocation strategy.
   *
   * <p><u>Note</u> : The returned resource is then no longer available to other users until the
   * {@link #releaseCardResource(CardResource)} method is called or the service restarted.
   *
   * @param cardResourceProfileName The name of the card resource profile.
   * @return Null if no card resource is available.
   * @throws IllegalArgumentException If the profile name is null, empty or not configured.
   * @throws IllegalStateException If the service is not started.
   * @since 2.0
   */
  CardResource getCardResource(String cardResourceProfileName);

  /**
   * Releases the card resource to make it available to other users.
   *
   * @param cardResource The card resource to release.
   * @throws IllegalArgumentException If the provided card resource is null.
   * @since 2.0
   */
  void releaseCardResource(CardResource cardResource);

  /**
   * Removes the card resource.
   *
   * @param cardResource The card resource to remove.
   * @throws IllegalArgumentException If the provided card resource is null.
   * @since 2.0
   */
  void removeCardResource(CardResource cardResource);
}
