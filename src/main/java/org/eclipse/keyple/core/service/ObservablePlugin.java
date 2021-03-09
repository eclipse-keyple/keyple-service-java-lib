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

import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;

/**
 * Plugin able to observe the connection/disconnection of {@link Reader}.
 *
 * <p>Allows registered observers to receive a {@link PluginEvent} when a reader is
 * connected/disconnected.
 *
 * @since 2.0
 */
public interface ObservablePlugin extends Plugin {

  /**
   * Registers a new observer to be notified when a plugin event occurs.
   *
   * <p>The provided observer must implement the {@link PluginObserverSpi} interface to be able to
   * receive the events produced by this plugin (reader connection, disconnection).
   *
   * <p>It is possible to add as many observers as necessary. They will be notified of events
   * <b>sequentially</b> in the order in which they are added.
   *
   * <p>If applicable, the observation process shall be started when the first observer is added.
   *
   * @param observer An observer object implementing the required interface (should be not null).
   * @since 2.0
   */
  void addObserver(final PluginObserverSpi observer);

  /**
   * Unregisters a plugin observer.
   *
   * <p>The observer will no longer receive any of the events produced by this plugin.
   *
   * <p>If applicable, the observation process shall be stopped when the last observer is removed.
   *
   * @param observer The observer object to be unregistered (should be not null).
   * @since 2.0
   */
  void removeObserver(final PluginObserverSpi observer);

  /**
   * Unregisters all observers at once.
   *
   * @since 2.0
   */
  void clearObservers();

  /**
   * Provides the current number of registered observers.
   *
   * @return An int.
   * @since 2.0
   */
  int countObservers();

  /**
   * Configures the plugin to use a custom thread pool for events notification.
   *
   * <p>The custom pool should be flexible enough to handle many concurrent tasks as each {@link
   * PluginEvent} are executed asynchronously.
   *
   * <p>The use of this method is optional and depends on the needs of the application.<br>
   * When used, the event notification will always be done asynchronously. Otherwise, the
   * notification can be synchronous (local plugin) or asynchronous (remote plugin) depending on the
   * type of reader.
   *
   * @param eventNotificationExecutorService The provided by the application.
   * @since 2.0
   */
  void setEventNotificationExecutorService(ExecutorService eventNotificationExecutorService);

  /**
   * Sets the exception handler.
   *
   * <p>In case of a fatal error during the observation, the handler will receive a notification.
   *
   * @param exceptionHandler The exception handler implemented by the application.
   * @since 2.0
   */
  void setReaderObservationExceptionHandler(PluginObservationExceptionHandlerSpi exceptionHandler);
}
