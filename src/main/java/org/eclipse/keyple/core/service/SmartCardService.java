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
 * Main entry for applications using Keyple.
 *
 * @since 2.0
 */
public final class SmartCardService {

  /** Field logger */
  private static final Logger logger = LoggerFactory.getLogger(SmartCardService.class);

  /** singleton instance of SmartCardService */
  private static final SmartCardService uniqueInstance = new SmartCardService();

  /** the list of readers’ plugins interfaced with the card Proxy Service */
  private final Map<String, Plugin> plugins = new ConcurrentHashMap<String, Plugin>();

  /** Field MONITOR, this is the object we will be synchronizing on ("the monitor") */
  private final Object MONITOR = new Object();

  /** Instantiates a new SmartCardService. */
  private SmartCardService() {}

  /**
   * Gets the single instance of SmartCardService.
   *
   * @return single instance of SmartCardService
   * @since 2.0
   */
  public static SmartCardService getInstance() {
    return uniqueInstance;
  }

  /**
   * Register a new plugin to be available in the platform if not registered yet
   *
   * @param pluginFactory plugin factory to instantiate plugin to be added
   * @throws KeyplePluginInstantiationException if instantiation failed
   * @return Plugin : registered reader plugin
   * @throws IllegalStateException if the plugin has already been registered.
   * @since 2.0
   */
  public Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory) {

    Assert.getInstance().notNull(pluginFactory, "pluginFactory");

    /*
    synchronized (MONITOR) {
      final String pluginName = pluginFactory.getPluginName();
      if (this.plugins.containsKey(pluginName)) {
        throw new IllegalStateException(
            "Plugin has already been registered to the platform : " + pluginName);
      } else {
        Plugin pluginInstance = pluginFactory.getPlugin();
        if (pluginInstance instanceof AbstractPlugin) {
          logger.info("Registering a new Plugin to the platform : {}", pluginName);
          ((AbstractPlugin) pluginInstance).register();
        } else {
          logger.info("No registration needed for pool plugin : {}", pluginName);
        }
        this.plugins.put(pluginName, pluginInstance);
        return pluginInstance;
      }
    }
     */
    return null;
  }

  /**
   * Unregister plugin from platform
   *
   * @param pluginName plugin name
   * @throws IllegalStateException if the plugin or his reader(s) are already unregistered
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
   * Check whether a plugin is already registered to the platform or not
   *
   * @param pluginName name of the plugin to be checked
   * @return true if a plugin with matching name has been registered
   * @since 2.0
   */
  public synchronized boolean isPluginRegistered(String pluginName) {
    synchronized (MONITOR) {
      return plugins.containsKey(pluginName);
    }
  }

  /**
   * Gets the plugins.
   *
   * @return the plugin names and plugin instances map of interfaced reader’s plugins.
   * @since 2.0
   */
  public synchronized Map<String, Plugin> getPlugins() {
    return plugins;
  }

  /**
   * Gets the plugin whose name is provided as an argument.
   *
   * @param pluginName the plugin name
   * @return the plugin
   * @throws KeyplePluginNotFoundException if the wanted plugin is not found
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
   * Verifies the compatibility with the service of the provided card extension.
   *
   * <p>The verification is based on the comparison of the respective API versions.
   *
   * @param cardExtension A not null {@link KeypleCardExtension} reference object
   * @since 2.0
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    // TODO complete
  }

  /**
   * Registers a new Distributed Local Service to be available in the platform if not registered yet
   *
   * @param distributedLocalServiceExtensionFactory Factory to use to instantiate a Distributed
   *     Local Service extension
   * @return A {@link DistributedLocalService} reference
   * @since 2.0
   */
  public DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory) {
    // TODO complete
    return null;
  }

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName TODO
   * @since 2.0
   */
  public void unregisterDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName TODO
   * @since 2.0
   */
  public void getDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }
}
