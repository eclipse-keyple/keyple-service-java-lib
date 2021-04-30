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

import java.util.*;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.util.Assert;

/**
 * Configurator of all plugins to associate to the card resource service.
 *
 * @since 2.0
 */
public final class PluginsConfigurator {

  private final AllocationStrategy allocationStrategy;
  private final int usageTimeoutMillis;
  private final List<Plugin> plugins;
  private final List<ConfiguredPlugin> configuredPlugins;

  private PluginsConfigurator(Builder builder) {
    allocationStrategy = builder.allocationStrategy;
    usageTimeoutMillis = builder.usageTimeoutMillis;
    plugins = builder.plugins;
    configuredPlugins = builder.configuredPlugins;
  }

  /**
   * (package-private)<br>
   * Gets the selected card resource allocation strategy.
   *
   * @return A not null reference.
   * @since 2.0
   */
  AllocationStrategy getAllocationStrategy() {
    return allocationStrategy;
  }

  /**
   * (package-private)<br>
   * Gets the configured usage timeout.
   *
   * @return 0 if no timeout is set.
   * @since 2.0
   */
  int getUsageTimeoutMillis() {
    return usageTimeoutMillis;
  }

  /**
   * (package-private)<br>
   * Gets the list of all configured "regular" plugins.
   *
   * @return A not empty list.
   * @since 2.0
   */
  List<Plugin> getPlugins() {
    return plugins;
  }

  /**
   * (package-private)<br>
   * Gets the list of all configured "regular" plugins with their associated configuration.
   *
   * @return A not empty collection.
   * @since 2.0
   */
  List<ConfiguredPlugin> getConfiguredPlugins() {
    return configuredPlugins;
  }

  /**
   * Gets the configurator's builder to use in order to create a new instance.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder of {@link PluginsConfigurator}.
   *
   * @since 2.0
   */
  public static class Builder {

    private AllocationStrategy allocationStrategy;
    private Integer usageTimeoutMillis;
    private final List<Plugin> plugins;
    private final List<ConfiguredPlugin> configuredPlugins;

    private Builder() {
      plugins = new ArrayList<Plugin>(1);
      configuredPlugins = new ArrayList<ConfiguredPlugin>(1);
    }

    /**
     * Specifies the allocation strategy to perform when a card resource is requested.
     *
     * <p>Default value: {@link AllocationStrategy#FIRST}
     *
     * @param allocationStrategy The {@link AllocationStrategy} to use.
     * @return The current builder instance.
     * @throws IllegalArgumentException If the provided strategy is null.
     * @throws IllegalStateException If the strategy has already been configured.
     * @since 2.0
     */
    public Builder withAllocationStrategy(AllocationStrategy allocationStrategy) {
      Assert.getInstance().notNull(allocationStrategy, "allocationStrategy");
      if (this.allocationStrategy != null) {
        throw new IllegalStateException("Allocation strategy already configured.");
      }
      this.allocationStrategy = allocationStrategy;
      return this;
    }

    /**
     * Specifies the timeout to use after that an allocated card resource can be automatically
     * reallocated by card resource service to a new thread if requested.
     *
     * <p>Default value: infinite
     *
     * @param usageTimeoutMillis The max usage duration of a card resource (in milliseconds).
     * @return The current builder instance.
     * @throws IllegalArgumentException If the provided value is less or equal to 0.
     * @throws IllegalStateException If the timeout has already been configured.
     * @since 2.0
     */
    public Builder withUsageTimeout(int usageTimeoutMillis) {
      Assert.getInstance().greaterOrEqual(usageTimeoutMillis, 1, "usageTimeoutMillis");
      if (this.usageTimeoutMillis != null) {
        throw new IllegalStateException("Usage timeout already configured.");
      }
      this.usageTimeoutMillis = usageTimeoutMillis;
      return this;
    }

    /**
     * Adds a {@link Plugin} or {@link ObservablePlugin} to the default list of all card profiles.
     *
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * @param plugin The plugin to add.
     * @param readerConfiguratorSpi The reader configurator to use when a reader is connected and
     *     accepted by at leas one card resource profile.
     * @return The current builder instance.
     * @throws IllegalArgumentException If the provided plugin or reader configurator is null or if
     *     the plugin is not an instance of Plugin or ObservablePlugin.
     * @throws IllegalStateException If the plugin has already been configured.
     * @since 2.0
     */
    public Builder addPlugin(Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi) {
      return addPluginWithMonitoring(plugin, readerConfiguratorSpi, null, null);
    }

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
     * @param pluginObservationExceptionHandlerSpi If not null, then activates the monitoring of the
     *     plugin and specifies the exception handler to use in case of error occurs during the
     *     asynchronous observation process.
     * @param readerObservationExceptionHandlerSpi If not null, then activates the monitoring of the
     *     readers and specifies the exception handler to use in case of error occurs during the
     *     asynchronous observation process.
     * @return The current builder instance.
     * @throws IllegalArgumentException If the provided plugin or reader configurator is null or if
     *     the plugin is not an instance of Plugin or ObservablePlugin.
     * @throws IllegalStateException If the plugin has already been configured.
     * @since 2.0
     */
    public Builder addPluginWithMonitoring(
        Plugin plugin,
        ReaderConfiguratorSpi readerConfiguratorSpi,
        PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi,
        ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi) {
      Assert.getInstance()
          .notNull(plugin, "plugin")
          .notNull(readerConfiguratorSpi, "readerConfiguratorSpi");
      if (plugin instanceof PoolPlugin) {
        throw new IllegalArgumentException(
            "Plugin must be an instance of Plugin or ObservablePlugin");
      }
      if (plugins.contains(plugin)) {
        throw new IllegalStateException("Plugin already configured.");
      }
      plugins.add(plugin);
      configuredPlugins.add(
          new ConfiguredPlugin(
              plugin,
              readerConfiguratorSpi,
              pluginObservationExceptionHandlerSpi,
              readerObservationExceptionHandlerSpi));
      return this;
    }

    /**
     * Creates a new instance of {@link PluginsConfigurator} using the current configuration.
     *
     * @return A new instance.
     * @throws IllegalStateException If no plugin has been configured.
     * @since 2.0
     */
    public PluginsConfigurator build() {
      if (plugins.isEmpty()) {
        throw new IllegalStateException("No plugin was configured.");
      }
      if (allocationStrategy == null) {
        allocationStrategy = AllocationStrategy.FIRST;
      }
      if (usageTimeoutMillis == null) {
        usageTimeoutMillis = 0; // Infinite
      }
      return new PluginsConfigurator(this);
    }
  }

  /**
   * Enumeration of all card resource service allocation strategies.
   *
   * @since 2.0
   */
  public enum AllocationStrategy {

    /**
     * Configures the card resource service to provide the first available card when a card
     * allocation is made.
     *
     * @since 2.0
     */
    FIRST,

    /**
     * Configures the card resource service to provide available cards on a cyclical basis to avoid
     * always providing the same card.
     *
     * @since 2.0
     */
    CYCLIC,

    /**
     * Configures the card resource service to provide available cards randomly to avoid always
     * providing the same card.
     *
     * @since 2.0
     */
    RANDOM
  }

  /**
   * (package-private)<br>
   * This POJO contains a plugin and the parameters that have been associated with it.
   *
   * @since 2.0
   */
  static class ConfiguredPlugin {

    private final Plugin plugin;
    private final ReaderConfiguratorSpi readerConfiguratorSpi;

    private boolean withPluginMonitoring;
    private PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi;

    private boolean withReaderMonitoring;
    private ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi;

    /**
     * (package-private)<br>
     * Constructor.
     *
     * @param plugin The plugin.
     * @param readerConfiguratorSpi The reader configurator to use.
     * @param pluginObservationExceptionHandlerSpi The plugin exception handler to use.
     * @param readerObservationExceptionHandlerSpi The reader exception handler to use.
     * @since 2.0
     */
    ConfiguredPlugin(
        Plugin plugin,
        ReaderConfiguratorSpi readerConfiguratorSpi,
        PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi,
        ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi) {
      this.plugin = plugin;
      this.readerConfiguratorSpi = readerConfiguratorSpi;
      if (pluginObservationExceptionHandlerSpi != null) {
        this.withPluginMonitoring = true;
        this.pluginObservationExceptionHandlerSpi = pluginObservationExceptionHandlerSpi;
      }
      if (readerObservationExceptionHandlerSpi != null) {
        this.withReaderMonitoring = true;
        this.readerObservationExceptionHandlerSpi = readerObservationExceptionHandlerSpi;
      }
    }

    /**
     * (package-private)<br>
     *
     * @return A not null {@link Plugin} reference.
     * @since 2.0
     */
    Plugin getPlugin() {
      return plugin;
    }

    /**
     * (package-private)<br>
     *
     * @return A not null {@link ReaderConfiguratorSpi} reference if reader monitoring is requested.
     * @since 2.0
     */
    ReaderConfiguratorSpi getReaderConfiguratorSpi() {
      return readerConfiguratorSpi;
    }

    /**
     * (package-private)<br>
     *
     * @return true if the reader monitoring is required.
     * @since 2.0
     */
    boolean isWithPluginMonitoring() {
      return withPluginMonitoring;
    }

    /**
     * (package-private)<br>
     *
     * @return A not null {@link PluginObservationExceptionHandlerSpi} reference if reader
     *     monitoring is requested.
     * @since 2.0
     */
    PluginObservationExceptionHandlerSpi getPluginObservationExceptionHandlerSpi() {
      return pluginObservationExceptionHandlerSpi;
    }

    /**
     * (package-private)<br>
     *
     * @return true if the card monitoring is required.
     * @since 2.0
     */
    boolean isWithReaderMonitoring() {
      return withReaderMonitoring;
    }

    /**
     * (package-private)<br>
     *
     * @return A not null {@link ReaderObservationExceptionHandlerSpi} reference if card monitoring
     *     is requested.
     * @since 2.0
     */
    ReaderObservationExceptionHandlerSpi getReaderObservationExceptionHandlerSpi() {
      return readerObservationExceptionHandlerSpi;
    }
  }
}
