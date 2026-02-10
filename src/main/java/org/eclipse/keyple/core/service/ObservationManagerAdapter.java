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
 * Manager of event observations for plugins and readers.
 *
 * @param <T> The type of the observers ({@link
 *     org.eclipse.keyple.core.service.spi.PluginObserverSpi} or {@link
 *     org.eclipse.keypop.reader.spi.CardReaderObserverSpi}).
 * @param <S> The type of the exception handler to use during the observation process ({@link
 *     org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi} or {@link
 *     org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi}).
 * @since 2.0.0
 */
final class ObservationManagerAdapter<T, S> {

  private static final Logger logger = LoggerFactory.getLogger(ObservationManagerAdapter.class);

  private final String ownerComponent;
  private final Set<T> observers;
  private final Object monitor;

  private S exceptionHandler;

  /**
   * Constructor.
   *
   * @param pluginName The name of the associated plugin (used for log only).
   * @param readerName The name of the associated reader (used for log only) (optional).
   * @since 2.0.0
   */
  ObservationManagerAdapter(String pluginName, String readerName) {
    if (readerName == null) {
      this.ownerComponent = String.format("[plugin=%s]", pluginName);
    } else {
      this.ownerComponent = String.format("[reader=%s]", readerName);
    }
    this.observers = new LinkedHashSet<>(1);
    this.monitor = new Object();
  }

  /**
   * Adds the provided observer if it is not already present.
   *
   * @param observer The observer to add.
   * @throws IllegalArgumentException If the provided observer is null.
   * @throws IllegalStateException If no observation exception handler has been set.
   * @since 2.0.0
   */
  void addObserver(T observer) {
    logger.info(
        "{} Adding observer [className={}]",
        ownerComponent,
        observer != null ? observer.getClass().getSimpleName() : null);
    Assert.getInstance().notNull(observer, "observer");
    if (exceptionHandler == null) {
      throw new IllegalStateException("No exception handler defined");
    }
    synchronized (monitor) {
      observers.add(observer);
    }
  }

  /**
   * Removes the provided observer if it is present.
   *
   * @param observer The observer to remove.
   * @throws IllegalArgumentException If the provided observer is null.
   * @since 2.0.0
   */
  void removeObserver(T observer) {
    logger.info(
        "{} Removing observer [className={}]",
        ownerComponent,
        observer != null ? observer.getClass().getSimpleName() : null);
    synchronized (monitor) {
      observers.remove(observer);
    }
  }

  /**
   * Removes all observers.
   *
   * @since 2.0.0
   */
  void clearObservers() {
    logger.info("{} Removing all observers", ownerComponent);
    synchronized (monitor) {
      observers.clear();
    }
  }

  /**
   * Gets the number of observers.
   *
   * @return The number of observers.
   * @since 2.0.0
   */
  int countObservers() {
    return observers.size();
  }

  /**
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
   * Gets a copy of the set of all observers.
   *
   * @return A not null copy.
   * @since 2.0.0
   */
  Set<T> getObservers() {
    synchronized (monitor) {
      return new LinkedHashSet<>(observers);
    }
  }

  /**
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
