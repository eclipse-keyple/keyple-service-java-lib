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

import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;

/**
 * Configurator of the card resource service.
 *
 * <p>The configuration consists in a sequence of steps including:
 *
 * <ul>
 *   <li>Assignment of plugins to be used with or without automatic refresh.
 *   <li>Selection of strategies and parameters of card allocation.
 *   <li>Creation of card profiles.
 * </ul>
 *
 * @since 2.0
 */
public interface CardResourceServiceConfigurator {

  /**
   * Configures the card resource service with one or more {@link Plugin} or {@link
   * ObservablePlugin}.
   *
   * @param pluginsConfigurator The plugins configurator to use.
   * @return The current configurator instance.
   * @throws IllegalArgumentException If the provided plugins configurator is null.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  CardResourceServiceConfigurator withPlugins(PluginsConfigurator pluginsConfigurator);

  /**
   * Configures the card resource service with one or more {@link PoolPlugin}.
   *
   * @param poolPluginsConfigurator The pool plugins configurator to use.
   * @return The current configurator instance.
   * @throws IllegalArgumentException If the provided pool plugins configurator is null.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  CardResourceServiceConfigurator withPoolPlugins(PoolPluginsConfigurator poolPluginsConfigurator);

  /**
   * Configures the card resource service with one or more card resource profiles.
   *
   * @param cardResourceProfileConfigurators The collection of card resources profiles to use.
   * @return The current configurator instance.
   * @throws IllegalArgumentException If the provided configurators are null.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  CardResourceServiceConfigurator withCardResourceProfiles(
      CardResourceProfileConfigurator... cardResourceProfileConfigurators);

  /**
   * Configures the card resource service to use a blocking allocation mode with the provided timing
   * parameters used during the allocation process.
   *
   * <p>By default, the card resource service is configured with a <b>non-blocking</b> allocation
   * mode.
   *
   * @param cycleDurationMillis The cycle duration (in milliseconds) is the time between two
   *     attempts to find an available card.
   * @param timeoutMillis The timeout (in milliseconds) is the maximum amount of time the allocation
   *     method will attempt to find an available card.
   * @return The current configurator instance.
   * @throws IllegalArgumentException If one of the provided values is less or equal to 0.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  CardResourceServiceConfigurator withBlockingAllocationMode(
      int cycleDurationMillis, int timeoutMillis);

  /**
   * Finalizes the configuration of the card resource service.
   *
   * <p>If the service is already started, the new configuration is applied immediately.<br>
   * Any previous configuration will be overwritten.
   *
   * <p>If some global configured plugins are not used by any card resource profile, then they are
   * automatically removed from the configuration.
   *
   * @throws IllegalStateException
   *     <ul>
   *       <li>If no "plugin" or "pool plugin" is configured.
   *       <li>If no card resource profile is configured.
   *       <li>If some card resource profiles are configured with the same profile name.
   *       <li>If some card resource profiles specify plugins which are not configured in the global
   *           list.
   *     </ul>
   *
   * @since 2.0
   */
  void configure();
}
