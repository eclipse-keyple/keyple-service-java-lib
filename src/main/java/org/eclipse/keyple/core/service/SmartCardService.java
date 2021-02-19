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
import org.eclipse.keyple.core.commons.KeypleCardExtension;
import org.eclipse.keyple.core.commons.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.commons.KeyplePluginExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SmartCardService. This singleton is the entry point of the card Proxy Service, its
 * instance has to be called by a ticketing application in order to establish a link with a card’s
 * application.
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
   */
  public Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory) {

    if (pluginFactory == null) {
      throw new IllegalArgumentException("Factory must not be null");
    }

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
   * Check weither a plugin is already registered to the platform or not
   *
   * @param pluginName name of the plugin to be checked
   * @return true if a plugin with matching name has been registered
   */
  public synchronized boolean isRegistered(String pluginName) {
    synchronized (MONITOR) {
      return plugins.containsKey(pluginName);
    }
  }

  /**
   * Gets the plugins.
   *
   * @return the plugin names and plugin instances map of interfaced reader’s plugins.
   */
  public synchronized Map<String, Plugin> getPlugins() {
    return plugins;
  }

  /**
   * Gets the plugin whose name is provided as an argument.
   *
   * @param name the plugin name
   * @return the plugin
   * @throws KeyplePluginNotFoundException if the wanted plugin is not found
   */
  public synchronized Plugin getPlugin(String name) {
    synchronized (MONITOR) {
      Plugin plugin = plugins.get(name);
      if (plugin == null) {
        throw new KeyplePluginNotFoundException(name);
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
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    // TODO complete
  }

  /**
   * TODO complete
   *
   * @param distributedLocalServiceExtensionFactory
   * @return
   */
  public DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory) {
    // TODO complete
    return null;
  }

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName
   */
  public void unregisterDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName
   */
  public void getDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }
}
