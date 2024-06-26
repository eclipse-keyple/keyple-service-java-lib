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

import static org.eclipse.keyple.core.service.DistributedUtilAdapter.*;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.distributed.remote.ObservableRemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemoteReaderSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a remote {@link ObservablePlugin}.
 *
 * @since 2.0.0
 */
final class ObservableRemotePluginAdapter extends RemotePluginAdapter
    implements ObservablePlugin, ObservableRemotePluginApi {

  private static final Logger logger = LoggerFactory.getLogger(ObservableRemotePluginAdapter.class);

  private final ObservableRemotePluginSpi observableRemotePluginSpi;
  private final ObservationManagerAdapter<PluginObserverSpi, PluginObservationExceptionHandlerSpi>
      observationManager;
  private final ExecutorService eventNotificationExecutorService;

  /**
   * Constructor.
   *
   * @param observableRemotePluginSpi The associated SPI.
   * @since 2.0.0
   */
  ObservableRemotePluginAdapter(ObservableRemotePluginSpi observableRemotePluginSpi) {
    super(observableRemotePluginSpi);
    this.observableRemotePluginSpi = observableRemotePluginSpi;
    this.observableRemotePluginSpi.connect((ObservableRemotePluginApi) this);
    observationManager = new ObservationManagerAdapter<>(getName(), null);
    eventNotificationExecutorService =
        observableRemotePluginSpi.getExecutorService() != null
            ? observableRemotePluginSpi.getExecutorService()
            : Executors.newCachedThreadPool();
  }

  /**
   * Notifies asynchronously all registered observers with the provided {@link PluginEvent}.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The plugin event.
   * @since 2.0.0
   */
  private void notifyObservers(final PluginEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Plugin [{}] notifies event [{}] to {} observer(s)",
          getName(),
          event.getType().name(),
          countObservers());
    }

    Set<PluginObserverSpi> observers = observationManager.getObservers();

    for (final PluginObserverSpi observer : observers) {
      eventNotificationExecutorService.execute(
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
                  logger.error("Event notification error: {}", e2.getMessage(), e2);
                  logger.error("Original cause: {}", e.getMessage(), e);
                }
              }
            }
          });
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    Set<String> unregisteredReaderNames = new HashSet<>(getReaderNames());
    notifyObservers(
        new PluginEventAdapter(getName(), unregisteredReaderNames, PluginEvent.Type.UNAVAILABLE));
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
    if (observationManager.countObservers() == 1) {

      logger.info("Start monitoring of plugin [{}]", getName());

      // Start the observation remotely.
      JsonObject input = new JsonObject();
      input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
      input.addProperty(JsonProperty.SERVICE.getKey(), PluginService.START_READER_DETECTION.name());

      // Execute the remote service.
      try {
        executePluginServiceRemotely(input, observableRemotePluginSpi, getName(), logger);

      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throwRuntimeException(e);
      }

      // Notify the SPI.
      observableRemotePluginSpi.onStartObservation();
    }
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
      if (observationManager.countObservers() == 0) {
        stopPluginMonitoring();
      }
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
    stopPluginMonitoring();
  }

  /** Stops the monitoring of the plugin. */
  private void stopPluginMonitoring() {

    logger.info("Stop plugin monitoring");

    // Notify the SPI first.
    observableRemotePluginSpi.onStopObservation();

    // Stop the observation remotely.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    input.addProperty(JsonProperty.SERVICE.getKey(), PluginService.STOP_READER_DETECTION.name());

    // Execute the remote service.
    try {
      executePluginServiceRemotely(input, observableRemotePluginSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
    logger.info("Plugin monitoring stopped");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public int countObservers() {
    return observationManager.countObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void setPluginObservationExceptionHandler(
      PluginObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  public void addRemoteReader(RemoteReaderSpi remoteReaderSpi, int clientCoreApiLevel) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Plugin [{}] registers reader [{}]",
          getName(),
          remoteReaderSpi != null ? remoteReaderSpi.getName() : null);
    }
    Assert.getInstance().notNull(remoteReaderSpi, "remoteReaderSpi");

    // Create the reader.
    RemoteReaderAdapter remoteReaderAdapter;
    if (remoteReaderSpi instanceof ObservableRemoteReaderSpi) {
      remoteReaderAdapter =
          new ObservableRemoteReaderAdapter(
              (ObservableRemoteReaderSpi) remoteReaderSpi, getName(), clientCoreApiLevel);
    } else {
      remoteReaderAdapter =
          new RemoteReaderAdapter(remoteReaderSpi, getName(), null, clientCoreApiLevel);
    }

    // Register the reader.
    getReadersMap().put(remoteReaderAdapter.getName(), remoteReaderAdapter);
    remoteReaderAdapter.register();

    // Notify observers for a plugin event.
    notifyObservers(
        new PluginEventAdapter(
            getName(), remoteReaderAdapter.getName(), PluginEvent.Type.READER_CONNECTED));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void removeRemoteReader(String remoteReaderName) {

    if (logger.isDebugEnabled()) {
      logger.debug("Plugin [{}] unregisters reader [{}]", getName(), remoteReaderName);
    }
    Assert.getInstance().notEmpty(remoteReaderName, "remoteReaderName");

    CardReader reader = getReader(remoteReaderName);

    if (reader instanceof RemoteReaderAdapter) {
      getReadersMap().remove(reader.getName());
      ((RemoteReaderAdapter) reader).unregister();
    } else {
      throw new IllegalArgumentException("Reader is not found, not registered or not remote");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onPluginEvent(String jsonData) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug("Plugin [{}] receives plugin event: {}", getName(), jsonData);
    }
    Assert.getInstance().notEmpty(jsonData, "jsonData");

    // Extract the event.
    PluginEvent pluginEvent;
    try {
      JsonObject json = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      pluginEvent =
          JsonUtil.getParser()
              .fromJson(
                  json.getAsJsonObject(JsonProperty.PLUGIN_EVENT.getKey()).toString(),
                  PluginEventAdapter.class);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format("JSON data of the plugin event is malformed: %s", e.getMessage()), e);
    }

    // Notify the observers for a plugin event.
    notifyObservers(pluginEvent);
  }
}
