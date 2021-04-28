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

import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.*;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Observable Remote reader adapter.
 *
 * @since 2.0
 */
final class ObservableRemoteReaderAdapter extends RemoteReaderAdapter implements ObservableReader {

  private static final Logger logger = LoggerFactory.getLogger(ObservableRemoteReaderAdapter.class);

  private final ObservableRemoteReaderAdapter masterReader;
  private final ObservationManagerAdapter<ReaderObserverSpi, ReaderObservationExceptionHandlerSpi>
      observationManager;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param remoteReaderSpi The remote reader SPI.
   * @param masterReader The associated master observable reader. Null if there is no master reader
   *     associated.
   * @param pluginName The name of the plugin.
   * @since 2.0
   */
  ObservableRemoteReaderAdapter(
      RemoteReaderSpi remoteReaderSpi,
      ObservableRemoteReaderAdapter masterReader,
      String pluginName) {
    super(remoteReaderSpi, pluginName);
    this.masterReader = masterReader;
    this.observationManager =
        new ObservationManagerAdapter<ReaderObserverSpi, ReaderObservationExceptionHandlerSpi>(
            pluginName, getName());
    this.observationManager.setEventNotificationExecutorService(Executors.newCachedThreadPool());
  }

  /**
   * (package-private)<br>
   * Gets the associated master observable reader.
   *
   * @return A not null reference.
   * @since 2.0
   */
  ObservableRemoteReaderAdapter getMasterReader() {
    return masterReader;
  }

  /**
   * (package-private)<br>
   * Notifies asynchronously all registered observers with the provided {@link ReaderEvent}.
   *
   * @param event The reader event.
   * @since 2.0
   */
  void notifyObservers(final ReaderEvent event) {

    if (masterReader != null) {
      masterReader.notifyObservers(event);

    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "The reader '{}' is notifying the reader event '{}' to {} observers.",
            getName(),
            event.getEventType().name(),
            countObservers());
      }

      Set<ReaderObserverSpi> observersCopy = observationManager.getObservers();

      for (final ReaderObserverSpi observer : observersCopy) {
        observationManager
            .getEventNotificationExecutorService()
            .execute(
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
  }

  /**
   * (package-private)<br>
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
   * @param pollingMode The polling policy (optional).
   * @since 2.0
   */
  void scheduleCardSelectionScenario(
      CardSelectionScenarioAdapter cardSelectionScenario,
      ObservableReader.NotificationMode notificationMode,
      ObservableReader.PollingMode pollingMode) {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(
        JsonProperty.SERVICE.name(), ReaderService.SCHEDULE_CARD_SELECTION_SCENARIO.name());

    input.add(
        JsonProperty.CARD_SELECTION_SCENARIO.name(),
        JsonUtil.getParser().toJsonTree(cardSelectionScenario));

    input.addProperty(JsonProperty.NOTIFICATION_MODE.name(), notificationMode.name());

    if (pollingMode != null) {
      input.addProperty(JsonProperty.POLLING_MODE.name(), pollingMode.name());
    }

    // Execute the remote service.
    try {
      DistributedUtilAdapter.executeReaderServiceRemotely(
          input, getRemoteReaderSpi(), getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Notifies all observers of the UNREGISTERED event.<br>
   * Stops the card detection unconditionally.<br>
   * Shuts down the reader's executor service.
   *
   * @since 2.0
   */
  @Override
  void unregister() {
    super.unregister();
    try {
      notifyObservers(
          new ReaderEvent(getPluginName(), getName(), ReaderEvent.EventType.UNREGISTERED, null));
      stopCardDetection();
    } finally {
      clearObservers();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(ReaderObserverSpi observer) {
    checkStatus();
    if (masterReader != null) {
      masterReader.addObserver(observer);
    } else {
      observationManager.addObserver(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(ReaderObserverSpi observer) {
    if (masterReader != null) {
      masterReader.removeObserver(observer);
    } else {
      observationManager.removeObserver(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int countObservers() {
    if (masterReader != null) {
      return masterReader.countObservers();
    } else {
      return observationManager.countObservers();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {
    if (masterReader != null) {
      masterReader.clearObservers();
    } else {
      observationManager.clearObservers();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void startCardDetection(PollingMode pollingMode) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' is starting the card detection with polling mode '{}'.",
          getName(),
          getPluginName(),
          pollingMode);
    }
    Assert.getInstance().notNull(pollingMode, "pollingMode");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.START_CARD_DETECTION.name());
    input.addProperty(JsonProperty.POLLING_MODE.name(), pollingMode.name());

    // Execute the remote service.
    try {
      DistributedUtilAdapter.executeReaderServiceRemotely(
          input, getRemoteReaderSpi(), getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void stopCardDetection() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' is stopping the card detection.",
          getName(),
          getPluginName());
    }

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.STOP_CARD_DETECTION.name());

    // Execute the remote service.
    try {
      DistributedUtilAdapter.executeReaderServiceRemotely(
          input, getRemoteReaderSpi(), getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void finalizeCardProcessing() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' is starting the removal sequence of the card.",
          getName(),
          getPluginName());
    }

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.FINALIZE_CARD_PROCESSING.name());

    // Execute the remote service.
    try {
      DistributedUtilAdapter.executeReaderServiceRemotely(
          input, getRemoteReaderSpi(), getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void setEventNotificationExecutorService(
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
  public void setReaderObservationExceptionHandler(
      ReaderObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }
}
