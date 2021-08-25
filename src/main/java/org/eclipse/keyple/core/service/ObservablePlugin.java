/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;

/**
 * Plugin able to observe the connection/disconnection of {@link Reader}.
 *
 * <p>Allows registered observers to receive a {@link PluginEvent} when a reader is
 * connected/disconnected.
 *
 * @since 2.0.0
 */
public interface ObservablePlugin extends Plugin {

  /**
   * Registers a new observer to be notified when a plugin event occurs.
   *
   * <p>The provided observer must implement the {@link PluginObserverSpi} interface to be able to
   * receive the events produced by this plugin (reader connection, disconnection).
   *
   * <p>If applicable, the observation process shall be started when the first observer is added.
   *
   * @param observer An observer object implementing the required interface (should be not null).
   * @since 2.0.0
   * @throws IllegalArgumentException if observer is null.
   * @throws IllegalStateException if no exception handler is defined.
   */
  void addObserver(final PluginObserverSpi observer);

  /**
   * Unregisters a plugin observer.
   *
   * <p>The observer will no longer receive any of the events produced by the plugin.
   *
   * <p>If applicable, the observation process shall be stopped when the last observer is removed.
   *
   * @param observer The observer object to be unregistered (should be not null).
   * @since 2.0.0
   * @throws IllegalArgumentException if observer is null.
   */
  void removeObserver(final PluginObserverSpi observer);

  /**
   * Unregisters all observers at once.
   *
   * @since 2.0.0
   */
  void clearObservers();

  /**
   * Provides the current number of registered observers.
   *
   * @return An int.
   * @since 2.0.0
   */
  int countObservers();

  /**
   * Sets the exception handler.
   *
   * <p>The invocation of this method is <b>mandatory</b> when the plugin has to be observed.
   *
   * <p>In case of a fatal error during the observation, the handler will receive a notification.
   *
   * @param exceptionHandler The exception handler implemented by the application.
   * @since 2.0.0
   */
  void setPluginObservationExceptionHandler(PluginObservationExceptionHandlerSpi exceptionHandler);
}
