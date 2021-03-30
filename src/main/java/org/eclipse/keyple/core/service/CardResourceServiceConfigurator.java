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

import org.eclipse.keyple.core.common.KeypleCardResourceProfileExtension;

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
   * @return Next configuration step.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  AllocationStrategyStep withPlugins();

  /**
   * Configures the card resource service with one or more {@link PoolPlugin}.
   *
   * @return Next configuration step.
   * @throws IllegalStateException If this step has already been performed.
   * @since 2.0
   */
  PoolAllocationStrategyStep withPoolPlugins();

  /**
   * Terminates the plugins configuration step.
   *
   * @return Next configuration step.
   * @throws IllegalStateException If no plugin has been added.
   * @since 2.0
   */
  AllocationTimingParameterStep endPluginsConfiguration();

  /**
   * Step to add pool plugins to the card resource service.
   *
   * @since 2.0
   */
  interface PluginStep {

    /**
     * Adds a {@link Plugin} or {@link ObservablePlugin} to the default list of all card profiles.
     *
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * <p>The plugin or readers must be observable for the monitoring operations to have an effect.
     *
     * @param plugin The plugin to add.
     * @param withReaderMonitoring true if the plugin must be observed to automatically detect
     *     reader connections/disconnections, false otherwise.
     * @param withCardMonitoring true if the readers must be observed to automatically detect card
     *     insertions/removals, false otherwise.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the provided plugin is null.
     * @since 2.0
     */
    PluginStep addPlugin(Plugin plugin, boolean withReaderMonitoring, boolean withCardMonitoring);

    /**
     * Terminates the addition of plugins.
     *
     * @return Next configuration step.
     * @throws IllegalStateException If no plugin has been added.
     * @since 2.0
     */
    CardResourceServiceConfigurator addNoMorePlugins();
  }

  /**
   * Step to add pool plugins to the card resource service.
   *
   * @since 2.0
   */
  interface PoolPluginStep {

    /**
     * Adds a {@link PoolPlugin} to the default list of all card profiles.
     *
     * <p><u>Note:</u> The order of the plugins is importan because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * @param poolPlugin The pool plugin to add.
     * @param withCardMonitoring true if the readers must be observed to automatically detect card
     *     insertions/removals, false otherwise.
     * @throws IllegalArgumentException If the provided pool plugin is null.
     * @return Next configuration step.
     * @since 2.0
     */
    PoolPluginStep addPoolPlugin(PoolPlugin poolPlugin, boolean withCardMonitoring);

    /**
     * Terminates the addition of pool plugins.
     *
     * @return Next configuration step.
     * @throws IllegalStateException If no pool plugin has been added.
     * @since 2.0
     */
    CardResourceServiceConfigurator addNoMorePoolPlugins();
  }

  /**
   * Step to configure the card resource service with allocation timeouts.
   *
   * @since 2.0
   */
  interface AllocationTimingParameterStep {

    /**
     * Configures the card resource service with the default timing parameters used during the
     * allocation process.
     *
     * @return Next configuration step.
     * @see #usingAllocationTimingParameters(int, int)
     * @since 2.0
     */
    ProfileStep usingDefaultAllocationTimingParameters();

    /**
     * Configures the card resource service with the provided timing parameters used during the
     * allocation process.
     *
     * <p>The cycle duration is the time between two attempts to find an available card.
     *
     * <p>The timeout is the maximum amount of time the allocation method will attempt to find an
     * available card.
     *
     * @param cycleDurationMillis A positive int.
     * @param timeoutMillis A positive int.
     * @return Next configuration step.
     * @since 2.0
     */
    ProfileStep usingAllocationTimingParameters(int cycleDurationMillis, int timeoutMillis);
  }

  /**
   * Step to configure the card resource service allocation strategy.
   *
   * @since 2.0
   */
  interface AllocationStrategyStep {

    /**
     * Configures the card resource service to provide the first available card when a card
     * allocation is made.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PluginStep usingFirstAllocationStrategy();

    /**
     * Configures the card resource service to provide available cards on a cyclical basis to avoid
     * always providing the same card.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PluginStep usingCyclicAllocationStrategy();

    /**
     * Configures the card resource service to provide available cards randomly to avoid always
     * providing the same card.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PluginStep usingRandomAllocationStrategy();
  }

  /**
   * Step to configure the card resource service pool and regular plugins priority strategy.
   *
   * @since 2.0
   */
  interface PoolAllocationStrategyStep {

    /**
     * Configures the card resource service to search for available cards in pool plugins before
     * regular plugins.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PoolPluginStep usingPoolPluginFirstAllocationStrategy();

    /**
     * Configures the card resource service to search for available cards in regular plugins before
     * pool plugins.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PoolPluginStep usingPoolPluginLastAllocationStrategy();
  }

  /**
   * Step to configure the card resource service with card profiles.
   *
   * @since 2.0
   */
  interface ProfileStep {

    /**
     * Creates a card profile with the provided name.
     *
     * @param name The card profile name.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the name is null or empty.
     * @throws IllegalStateException If the name is already in use.
     * @since 2.0
     */
    ProfileParameterStep addCardProfile(String name);

    /**
     * Terminates the creation of card profiles.
     *
     * @return Next configuration step.
     * @throws IllegalStateException If no card profile has been added.
     * @since 2.0
     */
    ConfigurationStep addNoMoreCardProfiles();
  }

  /**
   * Step to configure a card profile with parameters.
   *
   * @since 2.0
   */
  interface ProfileParameterStep {

    /**
     * Restricts the scope of the search during the allocation process to the provided plugins.
     *
     * <p>If this method is not invoked, all configured plugins will be used as search domain during
     * the allocation process.
     *
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process.
     *
     * @param plugins An ordered list of plugins.
     * @return Next configuration step.
     * @throws IllegalArgumentException If one or more plugin are null or empty.
     * @throws IllegalStateException If one or more plugins are not previously configured.
     * @since 2.0
     */
    ProfileParameterStep setPlugins(Plugin... plugins);

    /**
     * Sets a filter targeting all card readers having a name matching the provided regular
     * expression.
     *
     * @param readerNameRegex A regular expression.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the readerNameRegex is null, empty or invalid.
     * @since 2.0
     */
    ProfileParameterStep setReaderNameRegex(String readerNameRegex);

    /**
     * Sets a filter to target all card having the provided specific reader group reference.
     *
     * <p>This parameter only applies to a pool plugin.
     *
     * @param readerGroupReference A reader group reference.
     * @return Next configuration step.
     * @throws IllegalArgumentException If readerGroupReference is null or empty.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    ProfileParameterStep setReaderGroupReference(String readerGroupReference);

    /**
     * Defines a card resource profile extension to handle specific card operations to be performed
     * at allocation time.
     *
     * @param cardResourceProfileExtension A specific extension.
     * @return Next configuration step.
     * @throws IllegalArgumentException If cardResourceProfileExtension is null or invalid.
     * @since 2.0
     */
    ProfileParameterStep setCardResourceProfileExtension(
        KeypleCardResourceProfileExtension cardResourceProfileExtension);

    /**
     * Terminates the addition of parameters.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    ProfileStep addNoMoreParameters();
  }

  /**
   * Last step to configure the card resource service.
   *
   * @since 2.0
   */
  interface ConfigurationStep {

    /**
     * Finalizes the configuration of the card resource service.
     *
     * <p>If the service is already started, the new configuration is applied immediately. <br>
     * Any previous configuration will be overwritten.
     *
     * @since 2.0
     */
    void configure();
  }
}
