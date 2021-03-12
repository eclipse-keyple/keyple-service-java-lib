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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all observable plugin adapters.
 *
 * @param <P> The type of plugin.
 */
abstract class AbstractObservablePluginAdapter<P> extends PluginAdapter
    implements ObservablePlugin {
  private final P observablePluginSpi;

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractObservablePluginAdapter.class);

  private List<PluginObserverSpi> observers;
  /*
   * this object will be used to synchronize the access to the observers list in order to be
   * thread safe
   */
  private final Object sync = new Object();
  private ExecutorService eventNotificationExecutorService;
  private PluginObservationExceptionHandlerSpi exceptionHandler;

  AbstractObservablePluginAdapter(P observablePluginSpi) {
    super(observablePluginSpi);
    this.observablePluginSpi = observablePluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {
    if (observer == null) {
      return;
    }

    if (logger.isTraceEnabled()) {
      logger.trace(
          "Adding '{}' as an observer of '{}'.", observer.getClass().getSimpleName(), getName());
    }

    synchronized (sync) {
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
    if (observer == null) {
      return;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] Deleting a plugin observer", getName());
    }
    synchronized (sync) {
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
  public void clearObservers() {
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
  public final int countObservers() {
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
  public void setPluginObservationExceptionHandler(
      PluginObservationExceptionHandlerSpi exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final void unregister() {
    super.unregister();
    // TODO check what reader name should be used.
    notifyObservers(new PluginEvent(this.getName(), "", PluginEvent.EventType.UNREGISTERED));
    clearObservers();
  }

  /**
   * Push a {@link PluginEvent} of the observable plugin to its registered observers.
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

    synchronized (sync) {
      if (observers == null) {
        return;
      }
      observersCopy = new ArrayList<PluginObserverSpi>(observers);
    }

    for (PluginObserverSpi observer : observersCopy) {
      observer.onPluginEvent(event);
    }
  }
}
