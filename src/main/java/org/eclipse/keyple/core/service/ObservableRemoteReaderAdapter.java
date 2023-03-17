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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemoteReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observable Remote reader adapter.
 *
 * @since 2.0.0
 */
final class ObservableRemoteReaderAdapter extends RemoteReaderAdapter implements ObservableReader {

  private static final Logger logger = LoggerFactory.getLogger(ObservableRemoteReaderAdapter.class);

  private final ObservableRemoteReaderSpi observableRemoteReaderSpi;
  private final ObservationManagerAdapter<
          CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi>
      observationManager;
  private final ExecutorService eventNotificationExecutorService;

  /**
   * Constructor.
   *
   * @param observableRemoteReaderSpi The remote reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0.0
   */
  ObservableRemoteReaderAdapter(
      ObservableRemoteReaderSpi observableRemoteReaderSpi, String pluginName) {
    super(observableRemoteReaderSpi, pluginName);
    this.observableRemoteReaderSpi = observableRemoteReaderSpi;
    this.observationManager =
        new ObservationManagerAdapter<
            CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi>(pluginName, getName());
    this.eventNotificationExecutorService = Executors.newCachedThreadPool();
  }

  /**
   * Notifies asynchronously all registered observers with the provided {@link ReaderEvent}.
   *
   * @param event The reader event.
   * @since 2.0.0
   */
  void notifyObservers(final ReaderEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' notifies the reader event '{}' to {} observer(s).",
          getName(),
          event.getType().name(),
          countObservers());
    }

    Set<CardReaderObserverSpi> observersCopy = observationManager.getObservers();

    for (final CardReaderObserverSpi observer : observersCopy) {
      eventNotificationExecutorService.execute(
          new Runnable() {
            @Override
            public void run() {
              try {
                observer.onReaderEvent(event);
              } catch (Exception e) {
                try {
                  observationManager
                      .getObservationExceptionHandler()
                      .onReaderObservationError(getPluginName(), getName(), e);
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
   * If defined, the prepared {@link CardSelectionScenarioAdapter} will be processed as soon as a
   * card is inserted. The result of this request set will be added to the reader event notified to
   * the application.
   *
   * <p>If it is not defined (set to null), a simple card detection will be notified in the end.
   *
   * <p>Depending on the notification policy, the observer will be notified whenever a card is
   * inserted, regardless of the selection status, or only if the current card matches the selection
   * criteria.
   *
   * @param cardSelectionScenario The card selection scenario.
   * @param notificationMode The notification policy.
   * @param detectionMode The polling policy (optional).
   * @since 2.0.0
   */
  void scheduleCardSelectionScenario(
      CardSelectionScenarioAdapter cardSelectionScenario,
      NotificationMode notificationMode,
      DetectionMode detectionMode) {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(
        JsonProperty.SERVICE.name(), ReaderService.SCHEDULE_CARD_SELECTION_SCENARIO.name());

    input.add(
        JsonProperty.CARD_SELECTION_SCENARIO.name(),
        JsonUtil.getParser().toJsonTree(cardSelectionScenario));

    input.addProperty(JsonProperty.NOTIFICATION_MODE.name(), notificationMode.name());

    if (detectionMode != null) {
      input.addProperty(JsonProperty.POLLING_MODE.name(), detectionMode.name());
    }

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(
          input, observableRemoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Notifies all observers of the UNAVAILABLE event.<br>
   * Stops the card detection unconditionally.<br>
   * Shuts down the reader's executor service.
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    try {
      stopCardDetection();
    } catch (Exception e) {
      logger.error("Error during the stop card detection of reader '{}'", getName(), e);
    }
    notifyObservers(
        new ReaderEventAdapter(getPluginName(), getName(), CardReaderEvent.Type.UNAVAILABLE, null));
    clearObservers();
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void addObserver(CardReaderObserverSpi observer) {
    checkStatus();
    observationManager.addObserver(observer);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void removeObserver(CardReaderObserverSpi observer) {
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
  public int countObservers() {
    return observationManager.countObservers();
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
  public void startCardDetection(DetectionMode detectionMode) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' starts the card detection with polling mode '{}'.",
          getName(),
          getPluginName(),
          detectionMode);
    }
    Assert.getInstance().notNull(detectionMode, "detectionMode");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.START_CARD_DETECTION.name());
    input.addProperty(JsonProperty.POLLING_MODE.name(), detectionMode.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(
          input, observableRemoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }

    // Notify the SPI.
    observableRemoteReaderSpi.onStartObservation();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void stopCardDetection() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' stops the card detection.", getName(), getPluginName());
    }

    // Notify the SPI first.
    observableRemoteReaderSpi.onStopObservation();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.STOP_CARD_DETECTION.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(
          input, observableRemoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void finalizeCardProcessing() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' starts the removal sequence of the card.",
          getName(),
          getPluginName());
    }

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.FINALIZE_CARD_PROCESSING.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(
          input, observableRemoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void setReaderObservationExceptionHandler(
      CardReaderObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }
}
