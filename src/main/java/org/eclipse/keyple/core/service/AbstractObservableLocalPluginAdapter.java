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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for all observable local plugin adapters.
 *
 * @since 2.0.0
 */
abstract class AbstractObservableLocalPluginAdapter extends LocalPluginAdapter
    implements ObservablePlugin {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractObservableLocalPluginAdapter.class);

  private final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      observationManager;

  /**
   * Constructor.
   *
   * @param pluginSpi The associated plugin SPI.
   * @since 2.0.0
   */
  AbstractObservableLocalPluginAdapter(PluginSpi pluginSpi) {
    super(pluginSpi);
    this.observationManager = new ObservationManagerAdapter<>(getName(), null);
  }

  /**
   * Gets the associated observation manager.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      getObservationManager() {
    return observationManager;
  }

  /**
   * Notifies all registered observers with the provided {@link PluginEventAdapter}.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The plugin event.
   * @since 2.0.0
   */
  final void notifyObservers(final PluginEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Plugin [{}] notifies event [{}] to {} observer(s)",
          getName(),
          event.getType().name(),
          countObservers());
    }

    for (PluginObserverSpi observer : observationManager.getObservers()) {
      notifyObserver(observer, event);
    }
  }

  /**
   * Notifies a single observer of an event.
   *
   * @param observer The observer to notify.
   * @param event The event.
   */
  private void notifyObserver(PluginObserverSpi observer, PluginEvent event) {
    try {
      observer.onPluginEvent(event);
    } catch (Exception e) {
      try {
        observationManager.getObservationExceptionHandler().onPluginObservationError(getName(), e);
      } catch (Exception e2) {
        logger.error("Event notification error: {}", e2.getMessage(), e2);
        logger.error("Original cause: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  final void unregister() {
    Set<String> unregisteredReaderNames = new HashSet<>(this.getReaderNames());
    notifyObservers(
        new PluginEventAdapter(
            this.getName(), unregisteredReaderNames, PluginEvent.Type.UNAVAILABLE));
    clearObservers();
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {
    checkStatus();
    observationManager.addObserver(observer);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {
    Assert.getInstance().notNull(observer, "observer");
    if (observationManager.getObservers().contains(observer)) {
      observationManager.removeObserver(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void clearObservers() {
    observationManager.clearObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final int countObservers() {
    return observationManager.countObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void setPluginObservationExceptionHandler(
      PluginObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }
}
