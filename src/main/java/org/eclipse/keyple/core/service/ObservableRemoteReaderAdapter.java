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
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemoteReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observable Remote reader adapter.
 *
 * @since 2.0.0
 */
final class ObservableRemoteReaderAdapter extends RemoteReaderAdapter
    implements ObservableCardReader {

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
   * @param clientCoreApiLevel The JSON API level of the associated client Core layer.
   * @since 2.0.0
   */
  ObservableRemoteReaderAdapter(
      ObservableRemoteReaderSpi observableRemoteReaderSpi,
      String pluginName,
      int clientCoreApiLevel) {
    super(observableRemoteReaderSpi, pluginName, null, clientCoreApiLevel);
    this.observableRemoteReaderSpi = observableRemoteReaderSpi;
    this.observationManager = new ObservationManagerAdapter<>(pluginName, getName());
    this.eventNotificationExecutorService = Executors.newCachedThreadPool();
  }

  /**
   * Notifies asynchronously all registered observers with the provided {@link CardReaderEvent}.
   *
   * @param event The reader event.
   * @since 2.0.0
   */
  void notifyObservers(final CardReaderEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "[reader={}] Notifying observers [eventType={}, observerCount={}]",
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
                  logger.error(
                      "[reader={}] Failed to notify observer [reason={}]",
                      getName(),
                      e.getMessage(),
                      e);
                  logger.error(
                      "[reader={}] Failed to notify observation exception handler [reason={}]",
                      getName(),
                      e2.getMessage(),
                      e2);
                }
              }
            }
          });
    }

    if (logger.isDebugEnabled()) {
      logger.debug("[reader={}] Observers notified", getName());
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
   * @since 2.0.0
   */
  void scheduleCardSelectionScenario(
      CardSelectionScenarioAdapter cardSelectionScenario, NotificationMode notificationMode) {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), getClientCoreApiLevel());
    input.addProperty(
        JsonProperty.SERVICE.getKey(), ReaderService.SCHEDULE_CARD_SELECTION_SCENARIO.name());

    JsonObject params = new JsonObject();
    params.add(
        JsonProperty.CARD_SELECTION_SCENARIO.getKey(),
        JsonUtil.getParser().toJsonTree(cardSelectionScenario));
    params.addProperty(JsonProperty.NOTIFICATION_MODE.getKey(), notificationMode.name());

    input.add(JsonProperty.PARAMETERS.getKey(), params);

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, observableRemoteReaderSpi, getName(), logger);

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
      logger.warn(
          "[reader={}] Failed to stop card monitoring [reason={}]", getName(), e.getMessage());
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
    logger.info(
        "[reader={}] Starting remote card monitoring [detectionMode={}]", getName(), detectionMode);
    Assert.getInstance().notNull(detectionMode, "detectionMode");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), getClientCoreApiLevel());
    input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.START_CARD_DETECTION.name());

    JsonObject params = new JsonObject();
    params.addProperty(JsonProperty.POLLING_MODE.getKey(), detectionMode.name());

    input.add(JsonProperty.PARAMETERS.getKey(), params);

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, observableRemoteReaderSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }

    // Notify the SPI.
    observableRemoteReaderSpi.onStartObservation();

    logger.info("[reader={}] Remote card monitoring started", getName());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void stopCardDetection() {

    logger.info("[reader={}] Stopping remote card monitoring", getName());

    // Notify the SPI first.
    observableRemoteReaderSpi.onStopObservation();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), getClientCoreApiLevel());
    input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.STOP_CARD_DETECTION.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, observableRemoteReaderSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
    logger.info("[reader={}] Remote card monitoring stopped", getName());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void finalizeCardProcessing() {

    logger.info("[reader={}] Starting remote card removal sequence", getName());

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), getClientCoreApiLevel());
    input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.FINALIZE_CARD_PROCESSING.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, observableRemoteReaderSpi, getName(), logger);

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
