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
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all observable plugin adapters.
 *
 * @param <P> The type of plugin.
 * @since 2.0
 */
abstract class AbstractObservablePluginAdapter<P> extends PluginAdapter<P>
    implements ObservablePlugin {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractObservablePluginAdapter.class);

  private List<PluginObserverSpi> observers;
  /*
   * this object will be used to synchronize the access to the observers list in order to be
   * thread safe
   */
  private final Object monitor = new Object();
  private ExecutorService eventNotificationExecutorService;
  private PluginObservationExceptionHandlerSpi exceptionHandler;

  /**
   * (package-private)<br>
   * Common constructor for all plugin adapters.
   *
   * @param observablePluginSpi The plugin SPI.
   * @since 2.0
   */
  AbstractObservablePluginAdapter(P observablePluginSpi) {
    super(observablePluginSpi);
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
  final void notifyObservers(PluginEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Notifying a plugin event to {} observers. EVENTNAME = {} ",
          this.getName(),
          countObservers(),
          event.getEventType().name());
    }
    List<PluginObserverSpi> observersCopy;

    synchronized (monitor) {
      if (observers == null) {
        return;
      }
      observersCopy = new ArrayList<PluginObserverSpi>(observers);
    }

    // TODO add the asynchronous notification with the executor service if set
    for (PluginObserverSpi observer : observersCopy) {
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
      if (observers == null) {
        observers = new ArrayList<PluginObserverSpi>(1);
      }
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
      if (observers != null) {
        observers.remove(observer);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public synchronized void clearObservers() {
    if (observers != null) {
      this.observers.clear();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final synchronized int countObservers() {
    return observers == null ? 0 : observers.size();
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
