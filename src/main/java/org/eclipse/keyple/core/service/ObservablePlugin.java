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

import org.eclipse.keyple.core.service.spi.PluginObserverSpi;

/**
 * Provides the API to observe {@link org.eclipse.keyple.core.service.Reader}'s
 * connection/disconnection.
 *
 * <p>Allows registered observers to receive a {@link PluginEvent} when a reader is
 * connected/disconnected
 *
 * @since 2.0
 */
public interface ObservablePlugin extends Plugin {

  /**
   * Register a new plugin observer to be notified when a plugin event occurs.
   *
   * <p>The provided observer will receive all the events produced by this plugin (reader
   * connection, disconnection).
   *
   * <p>It is possible to add as many observers as necessary. They will be notified of events
   * <b>sequentially</b> in the order in which they are added.
   *
   * @param observer An observer object implementing the required interface (should be not null).
   * @since 2.0
   */
  void addObserver(final PluginObserverSpi observer);

  /**
   * Unregister a plugin observer.
   *
   * <p>The observer will no longer receive any of the events produced by this plugin.
   *
   * @param observer The observer object to be unregistered (should be not null).
   * @since 2.0
   */
  void removeObserver(final PluginObserverSpi observer);

  /**
   * Unregister all observers at once.
   *
   * @since 2.0
   */
  void clearObservers();

  /**
   * Provides the current number of registered observers.
   *
   * @return an int
   * @since 2.0
   */
  int countObservers();
}
