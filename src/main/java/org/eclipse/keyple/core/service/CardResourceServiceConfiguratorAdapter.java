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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.card.spi.CardResourceProfileExtensionSpi;
import org.eclipse.keyple.core.common.KeypleCardResourceProfileExtension;
import org.eclipse.keyple.core.util.Assert;

/**
 * (package-private)<br>
 * Implementation of {@link CardResourceServiceConfigurator}.
 *
 * @since 2.0
 */
final class CardResourceServiceConfiguratorAdapter
    implements CardResourceServiceConfigurator,
        CardResourceServiceConfigurator.PluginStep,
        CardResourceServiceConfigurator.PoolPluginStep,
        CardResourceServiceConfigurator.AllocationTimingParameterStep,
        CardResourceServiceConfigurator.AllocationStrategyStep,
        CardResourceServiceConfigurator.PoolAllocationStrategyStep,
        CardResourceServiceConfigurator.ProfileStep,
        CardResourceServiceConfigurator.ProfileParameterStep,
        CardResourceServiceConfigurator.ConfigurationStep {

  private static final int DEFAULT_CYCLE_DURATION_MILLIS = 100;
  private static final int DEFAULT_TIMEOUT_MILLIS = 10000;

  private CardResourceAllocationStrategy cardResourceAllocationStrategy;
  private PoolPluginCardResourceAllocationStrategy poolPluginCardResourceAllocationStrategy;
  private final Set<Plugin> configuredPlugins;
  private final List<ConfiguredRegularPlugin> configuredRegularPlugins;
  private final List<ConfiguredPoolPlugin> configuredPoolPlugins;
  private final List<CardProfile> cardProfiles;
  private int cycleDurationMillis;
  private int timeoutMillis;
  private CardProfile cardProfile;

  /** (private) */
  private CardResourceServiceConfiguratorAdapter() {
    cardResourceAllocationStrategy = CardResourceAllocationStrategy.FIRST;
    poolPluginCardResourceAllocationStrategy = PoolPluginCardResourceAllocationStrategy.POOL_FIRST;
    configuredPlugins = new HashSet<Plugin>();
    configuredRegularPlugins = new ArrayList<ConfiguredRegularPlugin>();
    configuredPoolPlugins = new ArrayList<ConfiguredPoolPlugin>();
    cardProfiles = new ArrayList<CardProfile>();
  }

  /**
   * (package-private)<br>
   * The different allocation strategies for regular plugins.
   *
   * @since 2.0
   */
  enum CardResourceAllocationStrategy {
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
  enum PoolPluginCardResourceAllocationStrategy {
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
    private final boolean withReaderMonitoring;
    private final boolean withCardMonitoring;

    private ConfiguredRegularPlugin(
        Plugin plugin, boolean withReaderMonitoring, boolean withCardMonitoring) {
      this.plugin = plugin;
      this.withReaderMonitoring = withReaderMonitoring;
      this.withCardMonitoring = withCardMonitoring;
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
    boolean isWithReaderMonitoring() {
      return withReaderMonitoring;
    }

    /**
     * (package-private)<br>
     *
     * @return true if the card monitoring is required.
     * @since 2.0
     */
    boolean isWithCardMonitoring() {
      return withCardMonitoring;
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
    private final boolean withCardMonitoring;

    private ConfiguredPoolPlugin(PoolPlugin poolPlugin, boolean withCardMonitoring) {
      this.poolPlugin = poolPlugin;
      this.withCardMonitoring = withCardMonitoring;
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

    /**
     * (package-private)<br>
     *
     * @return true if the card monitoring is required.
     * @since 2.0
     */
    boolean isWithCardMonitoring() {
      return withCardMonitoring;
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
    private List<Plugin> plugins;
    private String readerNameRegex;
    private String readerGroupReference;
    private CardResourceProfileExtensionSpi cardResourceProfileExtension;

    /** (private) */
    private CardProfile(String name) {
      this.name = name;
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

    /** (private) */
    private void setCardResourceProfileExtension(
        KeypleCardResourceProfileExtension cardResourceProfileExtension) {
      this.cardResourceProfileExtension =
          (CardResourceProfileExtensionSpi) cardResourceProfileExtension;
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
        plugins = new ArrayList<Plugin>(0);
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
  public PluginStep usingFirstAllocationStrategy() {
    cardResourceAllocationStrategy = CardResourceAllocationStrategy.FIRST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep usingCyclicAllocationStrategy() {
    cardResourceAllocationStrategy = CardResourceAllocationStrategy.CYCLIC;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep usingRandomAllocationStrategy() {
    cardResourceAllocationStrategy = CardResourceAllocationStrategy.RANDOM;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PluginStep addPlugin(
      Plugin plugin, boolean withReaderMonitoring, boolean withCardMonitoring) {

    Assert.getInstance().notNull(plugin, "plugin");

    configuredPlugins.add(plugin);
    configuredRegularPlugins.add(
        new ConfiguredRegularPlugin(plugin, withReaderMonitoring, withCardMonitoring));
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
    poolPluginCardResourceAllocationStrategy = PoolPluginCardResourceAllocationStrategy.POOL_FIRST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolPluginStep usingPoolPluginLastAllocationStrategy() {
    poolPluginCardResourceAllocationStrategy = PoolPluginCardResourceAllocationStrategy.POOL_LAST;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public PoolPluginStep addPoolPlugin(PoolPlugin poolPlugin, boolean withCardMonitoring) {

    Assert.getInstance().notNull(poolPlugin, "poolPlugin");

    configuredPlugins.add(poolPlugin);
    configuredPoolPlugins.add(new ConfiguredPoolPlugin(poolPlugin, withCardMonitoring));

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
  public AllocationTimingParameterStep endPluginsConfiguration() {

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
  public ProfileStep usingDefaultAllocationTimingParameters() {
    return usingAllocationTimingParameters(DEFAULT_CYCLE_DURATION_MILLIS, DEFAULT_TIMEOUT_MILLIS);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileStep usingAllocationTimingParameters(int cycleDurationMillis, int timeoutMillis) {
    Assert.getInstance()
        .greaterOrEqual(0, cycleDurationMillis, "cycleDurationMillis")
        .greaterOrEqual(0, timeoutMillis, "timeoutMillis");
    this.cycleDurationMillis = cycleDurationMillis;
    this.timeoutMillis = timeoutMillis;
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ProfileParameterStep addCardProfile(String name) {

    Assert.getInstance().notEmpty(name, "name");

    for (CardProfile profile : cardProfiles) {
      if (name.equals(profile.getName())) {
        throw new IllegalStateException("Profile already in use: " + name);
      }
    }

    cardProfile = new CardProfile(name);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ConfigurationStep addNoMoreCardProfiles() {

    if (cardProfiles.isEmpty()) {
      throw new IllegalStateException("No CARD profile has been added.");
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
      throw new IllegalStateException("CARD reader group reference has already been set.");
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
      Assert.getInstance().notNull(plugin, "plugin");
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
  public ProfileParameterStep setCardResourceProfileExtension(
      KeypleCardResourceProfileExtension cardResourceProfileExtension) {
    cardProfile.setCardResourceProfileExtension(cardResourceProfileExtension);
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
    ((CardResourceServiceAdapter) CardResourceServiceProvider.getService()).configure(this);
  }

  /**
   * (package-private)<br>
   *
   * @return The selected card resource allocation strategy.
   * @since 2.0
   */
  CardResourceAllocationStrategy getCardResourceAllocationStrategy() {
    return cardResourceAllocationStrategy;
  }

  /**
   * (package-private)<br>
   *
   * @return The pool allocation strategy.
   * @since 2.0
   */
  PoolPluginCardResourceAllocationStrategy getPoolPluginAllocationStrategy() {
    return poolPluginCardResourceAllocationStrategy;
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
