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

import org.eclipse.keyple.core.common.KeypleCardResourceProfileExtension;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;

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
  AllocationModeStep endPluginsConfiguration();

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
     * @param plugin The plugin to add.
     * @param readerConfiguratorSpi The reader configurator to use when a reader is connected and
     *     accepted by at leas one card resource profile.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the provided plugin or reader configurator is null.
     * @since 2.0
     */
    PluginStep addPlugin(Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi);

    /**
     * Adds a {@link Plugin} or {@link ObservablePlugin} to the default list of all card profiles
     * with background auto monitoring of reader connections/disconnections and/or card
     * insertions/removals.
     *
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * <p>The plugin or readers must be observable for the monitoring operations to have an effect.
     *
     * @param plugin The plugin to add.
     * @param readerConfiguratorSpi The reader configurator to use when a reader is connected and
     *     accepted by at leas one card resource profile.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the provided plugin or reader configurator is null.
     * @since 2.0
     */
    PluginMonitoringStep addPluginWithMonitoring(
        Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi);

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
   * Step to configure the monitoring settings of a plugin.
   *
   * @since 2.0
   */
  interface PluginMonitoringStep {

    /**
     * Configures the service to observe the plugin to automatically detect reader
     * connections/disconnections.
     *
     * <p>The plugin must be observable for the monitoring operations to have an effect.
     *
     * @param pluginObservationExceptionHandlerSpi The exception handler to use when an exception
     *     occurs during the asynchronous observation process.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the provided exception handler is null.
     * @since 2.0
     */
    PluginStep withPluginMonitoring(
        PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi);

    /**
     * Configures the service to observe the reader to automatically detect card insertions/removals
     *
     * <p>The reader must be observable for the monitoring operations to have an effect.
     *
     * @param readerObservationExceptionHandlerSpi The exception handler to use when an exception
     *     occurs during the asynchronous observation process.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the provided exception handler is null.
     * @since 2.0
     */
    PluginStep withReaderMonitoring(
        ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi);

    /**
     * Configures the service to observe the plugin to automatically detect reader
     * connections/disconnections and the reader to automatically detect card insertions/removals.
     *
     * <p>The plugin and the reader must be observable for the monitoring operations to have an
     * effect.
     *
     * @param pluginObservationExceptionHandlerSpi The exception handler to use when an exception
     *     occurs during the asynchronous plugin observation process.
     * @param readerObservationExceptionHandlerSpi The exception handler to use when an exception
     *     occurs during the asynchronous reader observation process.
     * @return Next configuration step.
     * @throws IllegalArgumentException If one of the provided exception handler is null.
     * @since 2.0
     */
    PluginStep withPluginAndReaderMonitoring(
        PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi,
        ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi);
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
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * @param poolPlugin The pool plugin to add.
     * @throws IllegalArgumentException If the provided pool plugin is null.
     * @return Next configuration step.
     * @since 2.0
     */
    PoolPluginStep addPoolPlugin(PoolPlugin poolPlugin);

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
   * Step to configure the card resource service allocation mode.
   *
   * @since 2.0
   */
  interface AllocationModeStep {

    /**
     * Configures the card resource service to use a blocking allocation mode with the default
     * timing parameters used during the allocation process.
     *
     * <p>The default cycle duration is set to 100 milliseconds.<br>
     * The default timeout duration is set to 15 seconds.
     *
     * @return Next configuration step.
     * @see #usingBlockingAllocationMode(int, int)
     * @since 2.0
     */
    ProfileStep usingBlockingAllocationMode();

    /**
     * Configures the card resource service to use a blocking allocation mode with the provided
     * timing parameters used during the allocation process.
     *
     * <p>The cycle duration is the time between two attempts to find an available card.
     *
     * <p>The timeout is the maximum amount of time the allocation method will attempt to find an
     * available card.
     *
     * @param cycleDurationMillis A positive int.
     * @param timeoutMillis A positive int.
     * @return Next configuration step.
     * @throws IllegalArgumentException If one of the provided values is less or equal to 0.
     * @since 2.0
     */
    ProfileStep usingBlockingAllocationMode(int cycleDurationMillis, int timeoutMillis);

    /**
     * Configures the card resource service to use a non blocking allocation mode.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    ProfileStep usingNonBlockingAllocationMode();
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
    UsageTimeoutStep usingFirstAllocationStrategy();

    /**
     * Configures the card resource service to provide available cards on a cyclical basis to avoid
     * always providing the same card.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    UsageTimeoutStep usingCyclicAllocationStrategy();

    /**
     * Configures the card resource service to provide available cards randomly to avoid always
     * providing the same card.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    UsageTimeoutStep usingRandomAllocationStrategy();
  }

  /**
   * Step to configure the max usage duration of a card resource associated to reader of a "regular"
   * plugin before it will be automatically released.
   *
   * @since 2.0
   */
  interface UsageTimeoutStep {

    /**
     * Uses the default card resource max usage duration of 10 seconds.
     *
     * @return Next configuration step.
     * @since 2.0
     */
    PluginStep usingDefaultUsageTimeout();

    /**
     * Uses the provided card resource max usage duration.
     *
     * @param usageTimeoutMillis The max usage duration of a card resource (in milliseconds).
     * @return Next configuration step.
     * @throws IllegalArgumentException if the provided value is less or equal to 0.
     * @since 2.0
     */
    PluginStep usingUsageTimeout(int usageTimeoutMillis);
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
     * Creates a card resource profile with the provided name and a card resource profile extension
     * to handle specific card operations to be performed at allocation time.
     *
     * @param name The card resource profile name.
     * @param cardResourceProfileExtension The associated specific extension able to select a card.
     * @return Next configuration step.
     * @throws IllegalArgumentException If the name or the card profile extension is null or empty.
     * @throws IllegalStateException If the name is already in use.
     * @since 2.0
     */
    ProfileParameterStep addCardResourceProfile(
        String name, KeypleCardResourceProfileExtension cardResourceProfileExtension);

    /**
     * Terminates the creation of card profiles.
     *
     * @return Next configuration step.
     * @throws IllegalStateException If no card profile has been added.
     * @since 2.0
     */
    ConfigurationStep addNoMoreCardResourceProfiles();
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
     * allocation process, but the pool plugins allocation strategy is defined by {@link
     * PoolAllocationStrategyStep}.
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
