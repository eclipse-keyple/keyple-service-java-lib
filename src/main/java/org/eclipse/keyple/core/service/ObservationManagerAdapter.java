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

import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Manager of event observations for plugins and readers.
 *
 * @param <T> The type of the observers ({@link
 *     org.eclipse.keyple.core.service.spi.PluginObserverSpi} or {@link
 *     org.calypsonet.terminal.reader.spi.CardReaderObserverSpi}).
 * @param <S> The type of the exception handler to use during the observation process ({@link
 *     org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi} or {@link
 *     org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi}).
 * @since 2.0.0
 */
final class ObservationManagerAdapter<T, S> {

  private static final Logger logger = LoggerFactory.getLogger(ObservationManagerAdapter.class);

  private final String ownerComponent;
  private final Set<T> observers;
  private final Object monitor;

  private S exceptionHandler;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param pluginName The name of the associated plugin (used for log only).
   * @param readerName The name of the associated reader (used for log only) (optional).
   * @since 2.0.0
   */
  ObservationManagerAdapter(String pluginName, String readerName) {
    if (readerName == null) {
      this.ownerComponent = String.format("Plugin '%s'", pluginName);
    } else {
      this.ownerComponent = String.format("Reader '%s' of plugin '%s'", readerName, pluginName);
    }
    this.observers = new LinkedHashSet<T>(1);
    this.monitor = new Object();
  }

  /**
   * (package-private)<br>
   * Adds the provided observer if it is not already present.
   *
   * @param observer The observer to add.
   * @throws IllegalArgumentException If the provided observer is null.
   * @throws IllegalStateException If no observation exception handler has been set.
   * @since 2.0.0
   */
  void addObserver(T observer) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "{} is adding the observer '{}'.",
          ownerComponent,
          observer != null ? observer.getClass().getSimpleName() : null);
    }
    Assert.getInstance().notNull(observer, "observer");
    if (exceptionHandler == null) {
      throw new IllegalStateException("No exception handler defined.");
    }
    synchronized (monitor) {
      observers.add(observer);
    }
  }

  /**
   * (package-private)<br>
   * Removes the provided observer if it is present.
   *
   * @param observer The observer to remove.
   * @throws IllegalArgumentException If the provided observer is null.
   * @since 2.0.0
   */
  void removeObserver(T observer) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "{} is removing the observer '{}'.",
          ownerComponent,
          observer != null ? observer.getClass().getSimpleName() : null);
    }
    synchronized (monitor) {
      observers.remove(observer);
    }
  }

  /**
   * (package-private)<br>
   * Removes all observers.
   *
   * @since 2.0.0
   */
  void clearObservers() {
    if (logger.isDebugEnabled()) {
      logger.debug("{} is removing all observers.", ownerComponent);
    }
    synchronized (monitor) {
      observers.clear();
    }
  }

  /**
   * (package-private)<br>
   * Gets the number of observers.
   *
   * @return The number of observers.
   * @since 2.0.0
   */
  int countObservers() {
    return observers.size();
  }

  /**
   * (package-private)<br>
   * Sets the observation exception handler.
   *
   * @param exceptionHandler the observation exception handler.
   * @throws IllegalArgumentException If the provided handler is null.
   * @since 2.0.0
   */
  void setObservationExceptionHandler(S exceptionHandler) {
    Assert.getInstance().notNull(exceptionHandler, "exceptionHandler");
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * (package-private)<br>
   * Gets a copy of the set of all observers.
   *
   * @return A not null copy.
   * @since 2.0.0
   */
  Set<T> getObservers() {
    synchronized (monitor) {
      return new LinkedHashSet<T>(observers);
    }
  }

  /**
   * (package-private)<br>
   * Gets the exception handler used to notify the application of exceptions raised during the
   * observation process.
   *
   * @return Null if no exception handler has been set.
   * @since 2.0.0
   */
  S getObservationExceptionHandler() {
    return exceptionHandler;
  }
}
