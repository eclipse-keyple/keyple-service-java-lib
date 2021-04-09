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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
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

  private final List<PluginObserverSpi> observers;
  private final Object monitor = new Object();

  private ExecutorService eventNotificationExecutorService;
  private PluginObservationExceptionHandlerSpi exceptionHandler;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param pluginSpi The associated plugin SPI.
   * @since 2.0
   */
  AbstractObservableLocalPluginAdapter(PluginSpi pluginSpi) {
    super(pluginSpi);
    this.observers = new ArrayList<PluginObserverSpi>(1);
  }

  /**
   * (package-private)<br>
   * Gets the exception handler used to notify the application of exceptions raised during the
   * observation process.
   *
   * @return null if no exception has been set.
   * @since 2.0
   */
  final PluginObservationExceptionHandlerSpi getObservationExceptionHandler() {
    return exceptionHandler;
  }

  /**
   * (package-private)<br>
   * Push a {@link PluginEvent} of the observable plugin to its registered observers.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The plugin event.
   * @since 2.0
   */
  final void notifyObservers(final PluginEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Notifying a plugin event to {} observers. EVENTNAME = {} ",
          this.getName(),
          countObservers(),
          event.getEventType().name());
    }
    List<PluginObserverSpi> observersCopy;

    synchronized (monitor) {
      observersCopy = new ArrayList<PluginObserverSpi>(observers);
    }

    if (eventNotificationExecutorService == null) {
      // synchronous notification
      for (PluginObserverSpi observer : observersCopy) {
        notifyObserver(observer, event);
      }
    } else {
      // asynchronous notification
      for (final PluginObserverSpi observer : observersCopy) {
        eventNotificationExecutorService.execute(
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
   * Notify a single observer of an event.
   *
   * @param observer The observer to notify.
   * @param event The event.
   */
  private void notifyObserver(PluginObserverSpi observer, PluginEvent event) {
    try {
      observer.onPluginEvent(event);
    } catch (Exception e) {
      try {
        exceptionHandler.onPluginObservationError(getName(), e);
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
    super.unregister();
    notifyObservers(
        new PluginEvent(
            this.getName(), this.getReadersNames(), PluginEvent.EventType.UNREGISTERED));
    clearObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {

    Assert.getInstance().notNull(observer, "observer");

    if (logger.isTraceEnabled()) {
      logger.trace(
          "Adding '{}' as an observer of '{}'.", observer.getClass().getSimpleName(), getName());
    }

    if (getObservationExceptionHandler() == null) {
      throw new IllegalStateException("No exception handler defined.");
    }

    synchronized (monitor) {
      observers.add(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {

    Assert.getInstance().notNull(observer, "observer");

    if (logger.isTraceEnabled()) {
      logger.trace("[{}] Deleting a plugin observer", getName());
    }
    synchronized (monitor) {
      observers.remove(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {
    synchronized (monitor) {
      observers.clear();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final int countObservers() {
    return observers.size();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void setEventNotificationExecutorService(
      ExecutorService eventNotificationExecutorService) {
    this.eventNotificationExecutorService = eventNotificationExecutorService;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void setPluginObservationExceptionHandler(
      PluginObservationExceptionHandlerSpi exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }
}
