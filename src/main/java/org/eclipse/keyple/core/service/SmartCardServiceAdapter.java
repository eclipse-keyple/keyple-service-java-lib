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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SmartCardService}.
 *
 * @since 2.0
 */
public final class SmartCardServiceAdapter implements SmartCardService {

  /** Field logger */
  private static final Logger logger = LoggerFactory.getLogger(SmartCardServiceAdapter.class);

  /** singleton instance of SmartCardServiceAdapter */
  private static final SmartCardServiceAdapter uniqueInstance = new SmartCardServiceAdapter();

  /** the list of readers’ plugins interfaced with the card Proxy Service */
  private final Map<String, Plugin> plugins = new ConcurrentHashMap<String, Plugin>();

  /** Field MONITOR, this is the object we will be synchronizing on ("the monitor") */
  private final Object MONITOR = new Object();

  /** Private constructor. */
  private SmartCardServiceAdapter() {}

  /**
   * (package-private)<br>
   * Gets the single instance of SmartCardServiceAdapter.
   *
   * @return single instance of SmartCardServiceAdapter
   * @since 2.0
   */
  static SmartCardServiceAdapter getInstance() {
    return uniqueInstance;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory) {

    Assert.getInstance().notNull(pluginFactory, "pluginFactory");

    //    synchronized (MONITOR) {
    //      if(pluginFactory instanceof PluginFactorySpi)
    //      {
    //        PluginSpi pluginSpi = ((PluginFactorySpi) pluginFactory).getPlugin();
    //        if(pluginSpi instanceof ObservablePluginSpi) {
    //          Plugin plugin = new Observable(pluginSpi, );
    //        }
    //      }
    //      final String pluginName = pluginFactory.;
    //      if (this.plugins.containsKey(pluginName)) {
    //        throw new IllegalStateException(
    //            "Plugin has already been registered to the platform : " + pluginName);
    //      } else {
    //        Plugin pluginInstance = pluginFactory.getPlugin();
    //        if (pluginInstance instanceof AbstractPlugin) {
    //          logger.info("Registering a new Plugin to the platform : {}", pluginName);
    //          ((AbstractPlugin) pluginInstance).register();
    //        } else {
    //          logger.info("No registration needed for pool plugin : {}", pluginName);
    //        }
    //        this.plugins.put(pluginName, pluginInstance);
    //        return pluginInstance;
    //      }
    //    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void unregisterPlugin(String pluginName) {
    /*
    synchronized (MONITOR) {
      final Plugin removedPlugin = plugins.remove(pluginName);
      if (removedPlugin != null) {
        if (removedPlugin instanceof AbstractPlugin) {
          ((AbstractPlugin) removedPlugin).unregister();
          logger.info("Unregistering a plugin from the platform : {}", removedPlugin.getName());
        } else {
          logger.info("Unregistration not needed for pool plugin : {}", pluginName);
        }
      } else {
        throw new IllegalStateException(
            String.format("This plugin, %s, is not registered", pluginName));
      }
    }
     */
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public synchronized boolean isPluginRegistered(String pluginName) {
    synchronized (MONITOR) {
      return plugins.containsKey(pluginName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public synchronized Map<String, Plugin> getPlugins() {
    return plugins;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public synchronized Plugin getPlugin(String pluginName) {
    synchronized (MONITOR) {
      Plugin plugin = plugins.get(pluginName);
      if (plugin == null) {
        throw new KeyplePluginNotFoundException(pluginName);
      }
      return plugin;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    // TODO complete
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory) {
    // TODO complete
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void unregisterDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void getDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }
}
