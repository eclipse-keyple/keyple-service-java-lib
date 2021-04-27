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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.card.spi.CardResourceProfileExtensionSpi;
import org.eclipse.keyple.core.common.KeypleCardResourceProfileExtension;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of {@link CardResourceServiceConfigurator}.
 *
 * @since 2.0
 */
final class CardResourceServiceConfiguratorAdapter
    implements CardResourceServiceConfigurator,
        CardResourceServiceConfigurator.PluginStep,
        CardResourceServiceConfigurator.PluginMonitoringStep,
        CardResourceServiceConfigurator.PoolPluginStep,
        CardResourceServiceConfigurator.AllocationModeStep,
        CardResourceServiceConfigurator.AllocationStrategyStep,
        CardResourceServiceConfigurator.UsageTimeoutStep,
        CardResourceServiceConfigurator.PoolAllocationStrategyStep,
        CardResourceServiceConfigurator.ProfileStep,
        CardResourceServiceConfigurator.ProfileParameterStep,
        CardResourceServiceConfigurator.ConfigurationStep {

  private static final Logger logger =
      LoggerFactory.getLogger(CardResourceServiceConfiguratorAdapter.class);

  private static final int DEFAULT_USAGE_TIMEOUT_MILLIS = 10000;
  private static final int DEFAULT_CYCLE_DURATION_MILLIS = 100;
  private static final int DEFAULT_TIMEOUT_MILLIS = 15000;
  private static final String PLUGIN = "plugin";

  private AllocationStrategy allocationStrategy;
  private PoolAllocationStrategy poolAllocationStrategy;
  private final Set<Plugin> configuredPlugins;
  private final List<ConfiguredRegularPlugin> configuredRegularPlugins;
  private final List<ConfiguredPoolPlugin> configuredPoolPlugins;
  private final List<CardProfile> cardProfiles;
  private boolean isBlockingAllocationMode;
  private int usageTimeoutMillis;
  private int cycleDurationMillis;
  private int timeoutMillis;
  private CardProfile cardProfile;
  private ConfiguredRegularPlugin currentConfiguredRegularPlugin;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @since 2.0
   */
  CardResourceServiceConfiguratorAdapter() {
    allocationStrategy = AllocationStrategy.FIRST;
    poolAllocationStrategy = PoolAllocationStrategy.POOL_FIRST;
    configuredPlugins = new HashSet<Plugin>();
    configuredRegularPlugins = new ArrayList<ConfiguredRegularPlugin>();
    configuredPoolPlugins = new ArrayList<ConfiguredPoolPlugin>();
    cardProfiles = new ArrayList<CardProfile>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep withPluginMonitoring(
      PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi) {
    Assert.getInstance()
        .notNull(pluginObservationExceptionHandlerSpi, "pluginObservationExceptionHandlerSpi");
    currentConfiguredRegularPlugin.setPluginMonitoring(pluginObservationExceptionHandlerSpi);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep withReaderMonitoring(
      ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi) {
    Assert.getInstance()
        .notNull(readerObservationExceptionHandlerSpi, "readerObservationExceptionHandlerSpi");
    currentConfiguredRegularPlugin.setReaderMonitoring(readerObservationExceptionHandlerSpi);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep withPluginAndReaderMonitoring(
      PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi,
      ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi) {
    withPluginMonitoring(pluginObservationExceptionHandlerSpi);
    withReaderMonitoring(readerObservationExceptionHandlerSpi);
    return this;
  }

  /**
   * (package-private)<br>
   * The different allocation strategies for regular plugins.
   *
   * @since 2.0
   */
  enum AllocationStrategy {
    FIRST,
    CYCLIC,
    RANDOM
  }

  /**
   * (package-private)<br>
   * The different allocation strategies when a {@link PoolPlugin } is available.
   *
   * @since 2.0
   */
  enum PoolAllocationStrategy {
    POOL_FIRST,
    POOL_LAST
  }

  /**
   * (package-private)<br>
   * This POJO contains a plugin and the parameters that have been associated with it.
   *
   * @since 2.0
   */
  static class ConfiguredRegularPlugin {

    private final Plugin plugin;
    private final ReaderConfiguratorSpi readerConfiguratorSpi;

    private boolean withPluginMonitoring;
    private PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi;

    private boolean withReaderMonitoring;
    private ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi;

    private ConfiguredRegularPlugin(Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi) {
      this.plugin = plugin;
      this.readerConfiguratorSpi = readerConfiguratorSpi;
    }

    /**
     * (private)<br>
     *
     * @param pluginObservationExceptionHandlerSpi The exception handler.
     */
    private void setPluginMonitoring(
        PluginObservationExceptionHandlerSpi pluginObservationExceptionHandlerSpi) {
      this.withPluginMonitoring = true;
      this.pluginObservationExceptionHandlerSpi = pluginObservationExceptionHandlerSpi;
    }

    /**
     * (private)<br>
     *
     * @param readerObservationExceptionHandlerSpi The exception handler.
     */
    private void setReaderMonitoring(
        ReaderObservationExceptionHandlerSpi readerObservationExceptionHandlerSpi) {
      this.withReaderMonitoring = true;
      this.readerObservationExceptionHandlerSpi = readerObservationExceptionHandlerSpi;
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
     * @return A not null {@link ReaderConfiguratorSpi} reference if reader monitoring is requested.
     * @since 2.0
     */
    ReaderConfiguratorSpi getReaderConfiguratorSpi() {
      return readerConfiguratorSpi;
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

  /**
   * (package-private)<br>
   * This POJO contains a pool plugin and the parameters that have been associated with it.
   *
   * @since 2.0
   */
  static class ConfiguredPoolPlugin {
    private final PoolPlugin poolPlugin;

    private ConfiguredPoolPlugin(PoolPlugin poolPlugin) {
      this.poolPlugin = poolPlugin;
    }

    /**
     * (package-private)<br>
     *
     * @return A not null {@link PoolPlugin} reference.
     * @since 2.0
     */
    PoolPlugin getPoolPlugin() {
      return poolPlugin;
    }
  }

  /**
   * (package-private)<br>
   * This POJO contains all the elements defining a CARD profile.
   *
   * @since 2.0
   */
  static class CardProfile {

    private final String name;
    private final CardResourceProfileExtensionSpi cardResourceProfileExtension;

    private List<Plugin> plugins;
    private String readerNameRegex;
    private String readerGroupReference;

    /** (private) */
    private CardProfile(
        String name, KeypleCardResourceProfileExtension cardResourceProfileExtension) {
      this.name = name;
      this.cardResourceProfileExtension =
          (CardResourceProfileExtensionSpi) cardResourceProfileExtension;
    }

    /** (private) */
    private void setPlugins(Plugin... plugins) {
      this.plugins = Arrays.asList(plugins);
    }

    /** (private) */
    private void setReaderNameRegex(String readerNameRegex) {
      this.readerNameRegex = readerNameRegex;
    }

    /** (private) */
    private void setReaderGroupReference(String readerGroupReference) {
      this.readerGroupReference = readerGroupReference;
    }

    /**
     * (package-private)<br>
     *
     * @return A not empty String containing the name of the profile.
     * @since 2.0
     */
    String getName() {
      return name;
    }

    /**
     * (package-private)<br>
     * Gets the list of {@link Plugin} associated to the profile.
     *
     * @return An empty list if no plugins have been configured.
     * @since 2.0
     */
    List<Plugin> getPlugins() {
      if (plugins == null) {
        plugins = Collections.emptyList();
      }
      return plugins;
    }

    /**
     * (package-private)<br>
     * Gets the string containing the regular expression to apply to the reader names associated to
     * the profile.
     *
     * @return null if no regex has been defined.
     * @since 2.0
     */
    String getReaderNameRegex() {
      return readerNameRegex;
    }

    /**
     * (package-private)<br>
     * Gets the string containing the reader group reference associated to the profile.
     *
     * @return null if no reader group reference has been defined.
     * @since 2.0
     */
    String getReaderGroupReference() {
      return readerGroupReference;
    }

    /**
     * (package-private)<br>
     * Gets the {@link CardResourceProfileExtensionSpi} associated to the profile.
     *
     * @return null if no extension has been defined.
     * @since 2.0
     */
    public CardResourceProfileExtensionSpi getCardResourceProfileExtension() {
      return cardResourceProfileExtension;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public AllocationStrategyStep withPlugins() {
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public UsageTimeoutStep usingFirstAllocationStrategy() {
    allocationStrategy = AllocationStrategy.FIRST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public UsageTimeoutStep usingCyclicAllocationStrategy() {
    allocationStrategy = AllocationStrategy.CYCLIC;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public UsageTimeoutStep usingRandomAllocationStrategy() {
    allocationStrategy = AllocationStrategy.RANDOM;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep usingDefaultUsageTimeout() {
    return usingUsageTimeout(DEFAULT_USAGE_TIMEOUT_MILLIS);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep usingUsageTimeout(int usageTimeoutMillis) {
    Assert.getInstance().greaterOrEqual(usageTimeoutMillis, 1, "usageTimeoutMillis");
    this.usageTimeoutMillis = usageTimeoutMillis;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep addPlugin(Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi) {
    Assert.getInstance()
        .notNull(plugin, PLUGIN)
        .notNull(readerConfiguratorSpi, "readerConfiguratorSpi");
    configuredPlugins.add(plugin);
    configuredRegularPlugins.add(new ConfiguredRegularPlugin(plugin, readerConfiguratorSpi));
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginMonitoringStep addPluginWithMonitoring(
      Plugin plugin, ReaderConfiguratorSpi readerConfiguratorSpi) {
    Assert.getInstance()
        .notNull(plugin, PLUGIN)
        .notNull(readerConfiguratorSpi, "readerConfiguratorSpi");
    configuredPlugins.add(plugin);
    currentConfiguredRegularPlugin = new ConfiguredRegularPlugin(plugin, readerConfiguratorSpi);
    configuredRegularPlugins.add(currentConfiguredRegularPlugin);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator addNoMorePlugins() {

    if (configuredRegularPlugins.isEmpty()) {
      throw new IllegalStateException("No plugin has been added.");
    }

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolAllocationStrategyStep withPoolPlugins() {
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolPluginStep usingPoolPluginFirstAllocationStrategy() {
    poolAllocationStrategy = PoolAllocationStrategy.POOL_FIRST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolPluginStep usingPoolPluginLastAllocationStrategy() {
    poolAllocationStrategy = PoolAllocationStrategy.POOL_LAST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolPluginStep addPoolPlugin(PoolPlugin poolPlugin) {

    Assert.getInstance().notNull(poolPlugin, "poolPlugin");

    configuredPlugins.add(poolPlugin);
    configuredPoolPlugins.add(new ConfiguredPoolPlugin(poolPlugin));

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator addNoMorePoolPlugins() {

    if (configuredRegularPlugins.isEmpty()) {
      throw new IllegalStateException("No pool plugin has been added.");
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public AllocationModeStep endPluginsConfiguration() {

    if (configuredRegularPlugins.isEmpty() && configuredPoolPlugins.isEmpty()) {
      throw new IllegalStateException("No plugin has been added.");
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileStep usingBlockingAllocationMode() {
    return usingBlockingAllocationMode(DEFAULT_CYCLE_DURATION_MILLIS, DEFAULT_TIMEOUT_MILLIS);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileStep usingBlockingAllocationMode(int cycleDurationMillis, int timeoutMillis) {
    Assert.getInstance()
        .greaterOrEqual(cycleDurationMillis, 1, "cycleDurationMillis")
        .greaterOrEqual(timeoutMillis, 1, "timeoutMillis");
    this.isBlockingAllocationMode = true;
    this.cycleDurationMillis = cycleDurationMillis;
    this.timeoutMillis = timeoutMillis;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileStep usingNonBlockingAllocationMode() {
    this.isBlockingAllocationMode = false;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileParameterStep addCardResourceProfile(
      String name, KeypleCardResourceProfileExtension cardResourceProfileExtension) {

    Assert.getInstance()
        .notEmpty(name, "name")
        .notNull(cardResourceProfileExtension, "cardResourceProfileExtension");

    for (CardProfile profile : cardProfiles) {
      if (name.equals(profile.getName())) {
        throw new IllegalStateException("Profile already in use: " + name);
      }
    }

    cardProfile = new CardProfile(name, cardResourceProfileExtension);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ConfigurationStep addNoMoreCardResourceProfiles() {

    if (cardProfiles.isEmpty()) {
      throw new IllegalStateException("No card profile has been added.");
    }

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileParameterStep setReaderGroupReference(String readerGroupReference) {

    Assert.getInstance().notEmpty(readerGroupReference, "readerGroupReference");

    if (cardProfile.getReaderGroupReference() != null) {
      throw new IllegalStateException("Card reader group reference has already been set.");
    }

    cardProfile.setReaderGroupReference(readerGroupReference);

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileParameterStep setPlugins(Plugin... plugins) {

    // check if all provided plugins are valid and known as configured regular or pool plugins.
    for (Plugin plugin : plugins) {
      Assert.getInstance().notNull(plugin, PLUGIN);
      if (!configuredPlugins.contains(plugin)) {
        throw new IllegalStateException("Plugin not configured: " + plugin.getName());
      }
    }

    cardProfile.setPlugins(plugins);

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileParameterStep setReaderNameRegex(String readerNameRegex) {

    Assert.getInstance().notEmpty(readerNameRegex, "readerNameRegex");

    if (cardProfile.getReaderNameRegex() != null) {
      throw new IllegalStateException("Reader name regex has already been set.");
    }

    try {
      Pattern.compile(readerNameRegex);
    } catch (PatternSyntaxException exception) {
      throw new IllegalArgumentException("Invalid regular expression: " + readerNameRegex);
    }

    cardProfile.setReaderNameRegex(readerNameRegex);

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileStep addNoMoreParameters() {

    cardProfiles.add(cardProfile);

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void configure() {

    // Remove plugins not used by a least one card profile.
    Set<Plugin> usedPlugins = computeUsedPlugins();

    if (configuredPlugins.size() != usedPlugins.size()) {

      Set<Plugin> unusedPlugins = new HashSet<Plugin>(configuredPlugins);
      unusedPlugins.removeAll(usedPlugins);

      List<ConfiguredRegularPlugin> unusedConfiguredRegularPlugins =
          getConfiguredRegularPlugins(unusedPlugins);

      List<ConfiguredPoolPlugin> unusedConfiguredPoolPlugins =
          getConfiguredPoolPlugins(unusedPlugins);

      configuredPlugins.removeAll(unusedPlugins);
      configuredRegularPlugins.removeAll(unusedConfiguredRegularPlugins);
      configuredPoolPlugins.removeAll(unusedConfiguredPoolPlugins);
    }

    // Apply the configuration.
    CardResourceServiceAdapter.getInstance().configure(this);
  }

  /**
   * (private)<br>
   * Gets all {@link ConfiguredRegularPlugin} associated to a plugin contained in the provided
   * collection.
   *
   * @param plugins The reference collection.
   * @return A not null collection.
   */
  private List<ConfiguredRegularPlugin> getConfiguredRegularPlugins(Set<Plugin> plugins) {
    List<ConfiguredRegularPlugin> results = new ArrayList<ConfiguredRegularPlugin>();
    for (ConfiguredRegularPlugin configuredRegularPlugin : configuredRegularPlugins) {
      if (plugins.contains(configuredRegularPlugin.getPlugin())) {
        logger.warn(
            "The card resource configurator removes the unused regular plugin '{}'.",
            configuredRegularPlugin.getPlugin().getName());
        results.add(configuredRegularPlugin);
      }
    }
    return results;
  }

  /**
   * (private)<br>
   * Gets all {@link ConfiguredPoolPlugin} associated to a pool plugin contained in the provided
   * collection.
   *
   * @param plugins The reference collection.
   * @return A not null collection.
   */
  private List<ConfiguredPoolPlugin> getConfiguredPoolPlugins(Set<Plugin> plugins) {
    List<ConfiguredPoolPlugin> results = new ArrayList<ConfiguredPoolPlugin>();
    for (ConfiguredPoolPlugin configuredPoolPlugin : configuredPoolPlugins) {
      if (plugins.contains(configuredPoolPlugin.getPoolPlugin())) {
        logger.warn(
            "The card resource configurator removes the unused pool plugin '{}'.",
            configuredPoolPlugin.getPoolPlugin().getName());
        results.add(configuredPoolPlugin);
      }
    }
    return results;
  }

  /**
   * (private)<br>
   * Computes the collection of the plugins used by at least one card profile.
   *
   * @return A not null collection.
   */
  private Set<Plugin> computeUsedPlugins() {
    Set<Plugin> usedPlugins = new HashSet<Plugin>();
    for (CardProfile profile : cardProfiles) {
      if (!profile.getPlugins().isEmpty()) {
        usedPlugins.addAll(profile.getPlugins());
      } else {
        return configuredPlugins;
      }
    }
    return usedPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return The selected card resource allocation strategy.
   * @since 2.0
   */
  AllocationStrategy getAllocationStrategy() {
    return allocationStrategy;
  }

  /**
   * (package-private)<br>
   *
   * @return The pool allocation strategy.
   * @since 2.0
   */
  PoolAllocationStrategy getPoolAllocationStrategy() {
    return poolAllocationStrategy;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null list of configured regular plugins.
   * @since 2.0
   */
  List<ConfiguredRegularPlugin> getConfiguredRegularPlugins() {
    return configuredRegularPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null list of configured pool plugins.
   * @since 2.0
   */
  List<ConfiguredPoolPlugin> getConfiguredPoolPlugins() {
    return configuredPoolPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return A not empty list of card profiles.
   * @since 2.0
   */
  List<CardProfile> getCardProfiles() {
    return cardProfiles;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null boolean.
   * @since 2.0
   */
  public boolean isBlockingAllocationMode() {
    return isBlockingAllocationMode;
  }

  /**
   * (package-private)<br>
   *
   * @return A positive int.
   * @since 2.0
   */
  int getUsageTimeoutMillis() {
    return usageTimeoutMillis;
  }

  /**
   * (package-private)<br>
   *
   * @return A positive int.
   * @since 2.0
   */
  int getCycleDurationMillis() {
    return cycleDurationMillis;
  }

  /**
   * (package-private)<br>
   *
   * @return A positive int.
   * @since 2.0
   */
  int getTimeoutMillis() {
    return timeoutMillis;
  }
}
