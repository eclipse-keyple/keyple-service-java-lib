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

import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.JsonProperty;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.distributed.remote.ObservableRemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of a remote {@link ObservablePlugin}.
 *
 * @since 2.0
 */
final class ObservableRemotePluginAdapter extends RemotePluginAdapter
    implements ObservablePlugin, ObservableRemotePluginApi {

  private static final Logger logger = LoggerFactory.getLogger(ObservableRemotePluginAdapter.class);

  private final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      observationManager;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param remotePluginSpi The associated SPI.
   * @since 2.0
   */
  ObservableRemotePluginAdapter(RemotePluginSpi remotePluginSpi) {
    super(remotePluginSpi);
    this.observationManager =
        new ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>(
            getName(), null);
  }

  /**
   * (package-private)<br>
   * Notifies asynchronously all registered observers with the provided {@link PluginEvent}.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The plugin event.
   * @since 2.0
   */
  void notifyObservers(final PluginEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is notifying the plugin event '{}' to {} observers.",
          getName(),
          event.getEventType().name(),
          countObservers());
    }

    Set<PluginObserverSpi> observers = observationManager.getObservers();

    for (final PluginObserverSpi observer : observers) {
      observationManager
          .getEventNotificationExecutorService()
          .execute(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    observer.onPluginEvent(event);
                  } catch (Exception e) {
                    try {
                      observationManager
                          .getObservationExceptionHandler()
                          .onPluginObservationError(getName(), e);
                    } catch (Exception e2) {
                      logger.error("Exception during notification", e2);
                      logger.error("Original cause", e);
                    }
                  }
                }
              });
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final void unregister() {
    Set<String> unregisteredReadersNames = new HashSet<String>(this.getReaderNames());
    super.unregister();
    notifyObservers(
        new PluginEvent(
            this.getName(), unregisteredReadersNames, PluginEvent.EventType.UNREGISTERED));
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
    if (observationManager.countObservers() == 1) {
      if (logger.isDebugEnabled()) {
        logger.debug("Start monitoring the plugin '{}'.", getName());
      }
      getRemotePluginSpi().startPluginsObservation();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {
    observationManager.removeObserver(observer);
    if (observationManager.countObservers() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Stop the plugin monitoring.");
      }
      getRemotePluginSpi().stopPluginsObservation();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {
    observationManager.clearObservers();
    if (logger.isDebugEnabled()) {
      logger.debug("Stop the plugin monitoring.");
    }
    getRemotePluginSpi().stopPluginsObservation();
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

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void registerMasterReader(RemoteReaderSpi masterReaderSpi) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is registering the master reader '{}'.",
          getName(),
          masterReaderSpi != null ? masterReaderSpi.getName() : null);
    }
    Assert.getInstance().notNull(masterReaderSpi, "masterReaderSpi");

    // Create the reader.
    RemoteReaderAdapter remoteReaderAdapter;
    if (masterReaderSpi.isObservable()) {
      remoteReaderAdapter = new ObservableRemoteReaderAdapter(masterReaderSpi, null, getName());
    } else {
      remoteReaderAdapter = new RemoteReaderAdapter(masterReaderSpi, getName());
    }

    // Register the reader.
    getReadersMap().put(remoteReaderAdapter.getName(), remoteReaderAdapter);
    remoteReaderAdapter.register();

    // Notify observers for a plugin event.
    notifyObservers(
        new PluginEvent(
            getName(), remoteReaderAdapter.getName(), PluginEvent.EventType.READER_CONNECTED));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void registerSlaveReader(
      RemoteReaderSpi slaveReaderSpi, String masterReaderName, String readerEventJsonData) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is registering the slave reader '{}' of the master reader '{}' causing by the following reader event : {}",
          getName(),
          slaveReaderSpi != null ? slaveReaderSpi.getName() : null,
          masterReaderName,
          readerEventJsonData);
    }
    Assert.getInstance()
        .notNull(slaveReaderSpi, "slaveReaderSpi")
        .isTrue(slaveReaderSpi.isObservable(), "isObservable")
        .notEmpty(masterReaderName, "masterReaderName")
        .notEmpty(readerEventJsonData, "readerEventJsonData");

    // Get the master reader.
    Reader masterReader = getReader(masterReaderName);
    if (!(masterReader instanceof ObservableRemoteReaderAdapter)) {
      throw new IllegalStateException(
          "The master reader is not found, not registered or not remote observable.");
    }

    // Create the reader.
    RemoteReaderAdapter remoteReaderAdapter =
        new ObservableRemoteReaderAdapter(
            slaveReaderSpi, (ObservableRemoteReaderAdapter) masterReader, getName());

    // Register the reader.
    getReadersMap().put(remoteReaderAdapter.getName(), remoteReaderAdapter);
    remoteReaderAdapter.register();

    // Notify observers for a reader event.
    onReaderEvent(readerEventJsonData);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void unregisterReader(String readerName) {

    if (logger.isDebugEnabled()) {
      logger.debug("The plugin '{}' is unregistering the reader '{}'.", getName(), readerName);
    }
    Assert.getInstance().notEmpty(readerName, "readerName");

    Reader reader = getReader(readerName);

    if (reader instanceof ObservableRemoteReaderAdapter) {

      ObservableRemoteReaderAdapter masterReader =
          ((ObservableRemoteReaderAdapter) reader).getMasterReader();

      if (masterReader != null) {
        // Slave
        unregisterReader(reader);
        // Master
        if (masterReader.countObservers() == 0) {
          unregisterReader(masterReader);
        }
      } else {
        // Master
        if (countObservers() == 0) {
          unregisterReader(reader);
        }
      }

    } else if (reader instanceof RemoteReaderAdapter) {
      unregisterReader(reader);

    } else {
      throw new IllegalArgumentException("The reader is not found, not registered or not remote.");
    }
  }

  /**
   * (private)<br>
   * Unregisters the provided reader.
   *
   * @param reader The reader to unregister.
   */
  private void unregisterReader(Reader reader) {
    getReadersMap().remove(reader.getName());
    ((RemoteReaderAdapter) reader).unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onPluginEvent(String jsonData) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is receiving the following plugin event : {}", getName(), jsonData);
    }
    Assert.getInstance().notEmpty(jsonData, "jsonData");

    // Extract the event.
    PluginEvent pluginEvent;
    try {
      JsonObject json = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      pluginEvent =
          JsonUtil.getParser()
              .fromJson(
                  json.get(JsonProperty.PLUGIN_EVENT.name()).getAsString(), PluginEvent.class);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format("The JSON data of the plugin event is malformed : %s", e.getMessage()), e);
    }

    // Notify the observers for a plugin event.
    notifyObservers(pluginEvent);
  }
}
