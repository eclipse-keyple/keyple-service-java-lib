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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all observable local plugin adapters.
 *
 * @since 2.0
 */
abstract class AbstractObservableLocalPluginAdapter extends LocalPluginAdapter
    implements ObservablePlugin {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractObservableLocalPluginAdapter.class);

  private final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      observationManager;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param pluginSpi The associated plugin SPI.
   * @since 2.0
   */
  AbstractObservableLocalPluginAdapter(PluginSpi pluginSpi) {
    super(pluginSpi);
    this.observationManager =
        new ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>(
            getName(), null);
  }

  /**
   * (package-private)<br>
   * Gets the associated observation manager.
   *
   * @return A not null reference.
   * @since 2.0
   */
  final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      getObservationManager() {
    return observationManager;
  }

  /**
   * (package-private)<br>
   * Notifies all registered observers with the provided {@link PluginEvent}.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The plugin event.
   * @since 2.0
   */
  final void notifyObservers(final PluginEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is notifying the plugin event '{}' to {} observers.",
          getName(),
          event.getEventType().name(),
          countObservers());
    }

    Set<PluginObserverSpi> observers = observationManager.getObservers();

    if (observationManager.getEventNotificationExecutorService() == null) {
      // synchronous notification
      for (PluginObserverSpi observer : observers) {
        notifyObserver(observer, event);
      }
    } else {
      // asynchronous notification
      for (final PluginObserverSpi observer : observers) {
        observationManager
            .getEventNotificationExecutorService()
            .execute(
                new Runnable() {
                  @Override
                  public void run() {
                    notifyObserver(observer, event);
                  }
                });
      }
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
        logger.error("Exception during notification", e2);
        logger.error("Original cause", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final void unregister() {
    Set<String> unregisteredReaderNames = new HashSet<String>(this.getReaderNames());
    super.unregister();
    notifyObservers(
        new PluginEvent(
            this.getName(), unregisteredReaderNames, PluginEvent.EventType.UNREGISTERED));
    clearObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {
    checkStatus();
    observationManager.addObserver(observer);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {
    observationManager.removeObserver(observer);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {
    observationManager.clearObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final int countObservers() {
    return observationManager.countObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void setEventNotificationExecutorService(
      ExecutorService eventNotificationExecutorService) {
    checkStatus();
    observationManager.setEventNotificationExecutorService(eventNotificationExecutorService);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void setPluginObservationExceptionHandler(
      PluginObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }
}
