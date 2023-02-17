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
import static org.eclipse.keyple.core.service.InternalDto.*;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtension;
import org.eclipse.keyple.core.distributed.local.LocalServiceApi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of public {@link DistributedLocalService} API.
 *
 * @since 2.0.0
 */
final class DistributedLocalServiceAdapter
    implements DistributedLocalService, PluginObserverSpi, CardReaderObserverSpi, LocalServiceApi {

  private static final Logger logger =
      LoggerFactory.getLogger(DistributedLocalServiceAdapter.class);

  private static final String READER_NOT_FOUND_TEMPLATE =
      "There is no local reader registered with the name '%s' or the associated plugin is no longer registered.";

  private final String name;
  private final LocalServiceSpi localServiceSpi;

  private List<String> poolPluginNames;
  private boolean isRegistered;

  /**
   * Constructor.
   *
   * @param localServiceSpi The associated SPI.
   * @since 2.0.0
   */
  DistributedLocalServiceAdapter(LocalServiceSpi localServiceSpi) {
    this.name = localServiceSpi.getName();
    this.localServiceSpi = localServiceSpi;
    localServiceSpi.connect(this);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void setPoolPluginNames(String... poolPluginNames) {
    this.poolPluginNames = poolPluginNames != null ? Arrays.asList(poolPluginNames) : null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String executeLocally(String jsonData, String readerName) {
    if (readerName != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Service '{}' processes data on the reader '{}' : {}", name, readerName, jsonData);
      }
      return new LocalReaderExecutor(jsonData, readerName).execute();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Service '{}' processes data on plugins : {}", name, jsonData);
      }
      return new LocalPluginExecutor(jsonData).execute();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public <T extends KeypleDistributedLocalServiceExtension> T getExtension(
      Class<T> distributedLocalServiceExtensionClass) {
    checkStatus();
    return (T) localServiceSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Service '{}' forwards the plugin event '{}' associated to the local reader '{}' of the local plugin '{}'.",
          name,
          pluginEvent.getType().name(),
          pluginEvent.getReaderNames().first(),
          pluginEvent.getPluginName());
    }

    JsonObject body = new JsonObject();
    body.addProperty(JsonProperty.PLUGIN_EVENT.name(), JsonUtil.toJson(pluginEvent));

    localServiceSpi.onPluginEvent(pluginEvent.getReaderNames().first(), body.toString());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderEvent(CardReaderEvent readerEvent) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Service '{}' forwards the reader event '{}' associated to the local reader '{}' of the local plugin '{}'.",
          name,
          readerEvent.getType().name(),
          readerEvent.getReaderName(),
          ((ReaderEventAdapter) readerEvent).getPluginName());
    }

    JsonObject body = new JsonObject();
    body.addProperty(JsonProperty.READER_EVENT.name(), JsonUtil.toJson(readerEvent));

    localServiceSpi.onReaderEvent(readerEvent.getReaderName(), body.toString());
  }

  /**
   * Registers the distributed local service.
   *
   * @since 2.0.0
   */
  void register() {
    isRegistered = true;
  }

  /**
   * Unregisters the distributed local service and stop all plugins and readers events observations.
   *
   * @since 2.0.0
   */
  void unregister() {
    for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
      if (plugin instanceof ObservablePlugin) {
        ((ObservablePlugin) plugin).removeObserver(this);
      }
      for (CardReader reader : plugin.getReaders()) {
        if (reader instanceof ObservableCardReader) {
          ((ObservableCardReader) reader).removeObserver(this);
        }
      }
    }
    isRegistered = false;
  }

  /**
   * Check if the distributed local service is registered.
   *
   * @throws IllegalStateException is thrown when the distributed local service is not (or no
   *     longer) registered.
   */
  private void checkStatus() {
    if (!isRegistered) {
      throw new IllegalStateException(
          String.format("Service '%s' is not or no longer registered.", name));
    }
  }

  /** Inner class used to execute a service on a specific local reader. */
  private final class LocalReaderExecutor {

    private final AbstractReaderAdapter reader;
    private final JsonObject input;
    private final JsonObject output;

    /**
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     * @param readerName The name of the target reader.
     */
    private LocalReaderExecutor(String jsonData, String readerName) {

      this.reader = getReader(readerName);
      if (reader == null) {
        throw new IllegalStateException(String.format(READER_NOT_FOUND_TEMPLATE, readerName));
      }
      this.input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      this.output = new JsonObject();
    }

    /**
     * Retrieves the first register reader having the provided name among all plugins.
     *
     * @param readerName The name of the reader to be found.
     * @return null if no reader is found with this name.
     */
    private AbstractReaderAdapter getReader(String readerName) {
      for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
        try {
          CardReader localReader = plugin.getReader(readerName);
          if (localReader != null) {
            return (AbstractReaderAdapter) localReader;
          }
        } catch (IllegalStateException e) {
          // The plugin is no longer register, then continue.
        }
      }
      return null;
    }

    /**
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.add(JsonProperty.SERVICE.name(), input.get(JsonProperty.SERVICE.name()));
      try {
        checkStatus();
        ReaderService service =
            ReaderService.valueOf(input.get(JsonProperty.SERVICE.name()).getAsString());
        switch (service) {
          case TRANSMIT_CARD_REQUEST:
            transmitCardRequest();
            break;
          case TRANSMIT_CARD_SELECTION_REQUESTS:
            transmitCardSelectionRequests();
            break;
          case SCHEDULE_CARD_SELECTION_SCENARIO:
            scheduleCardSelectionScenario();
            break;
          case IS_CARD_PRESENT:
            isCardPresent();
            break;
          case IS_CONTACTLESS:
            isContactless();
            break;
          case START_CARD_DETECTION:
            startCardDetection();
            break;
          case STOP_CARD_DETECTION:
            stopCardDetection();
            break;
          case FINALIZE_CARD_PROCESSING:
            finalizeCardProcessing();
            break;
          case RELEASE_CHANNEL:
            releaseChannel();
            break;
          default:
            throw new IllegalArgumentException(service.name());
        }
      } catch (Exception e) {
        output.addProperty(JsonProperty.ERROR.name(), JsonUtil.toJson(new BodyError(e)));
      }
      return output.toString();
    }

    /**
     * Service {@link ReaderService#TRANSMIT_CARD_REQUEST}.
     *
     * @throws CardBrokenCommunicationException If a card communication error occurs.
     * @throws ReaderBrokenCommunicationException If a reader communication error occurs.
     */
    private void transmitCardRequest()
        throws CardBrokenCommunicationException, ReaderBrokenCommunicationException,
            UnexpectedStatusWordException {

      // Extract info from the message
      ChannelControl channelControl =
          ChannelControl.valueOf(input.get(JsonProperty.CHANNEL_CONTROL.name()).getAsString());

      CardRequestSpi cardRequest =
          JsonUtil.getParser()
              .fromJson(
                  input.get(JsonProperty.CARD_REQUEST.name()).getAsString(), CardRequest.class);

      // Execute the service on the reader
      CardResponseApi cardResponse = reader.transmitCardRequest(cardRequest, channelControl);

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), JsonUtil.toJson(cardResponse));
    }

    /**
     * Service {@link ReaderService#TRANSMIT_CARD_SELECTION_REQUESTS}.
     *
     * @throws CardBrokenCommunicationException If a card communication error occurs.
     * @throws ReaderBrokenCommunicationException If a reader communication error occurs.
     */
    private void transmitCardSelectionRequests()
        throws CardBrokenCommunicationException, ReaderBrokenCommunicationException {

      // Extract info from the message
      List<CardSelectionRequestSpi> cardSelectionRequests =
          JsonUtil.getParser()
              .fromJson(
                  input.get(JsonProperty.CARD_SELECTION_REQUESTS.name()).getAsString(),
                  new TypeToken<ArrayList<CardSelectionRequest>>() {}.getType());

      MultiSelectionProcessing multiSelectionProcessing =
          MultiSelectionProcessing.valueOf(
              input.get(JsonProperty.MULTI_SELECTION_PROCESSING.name()).getAsString());

      ChannelControl channelControl =
          ChannelControl.valueOf(input.get(JsonProperty.CHANNEL_CONTROL.name()).getAsString());

      // Execute the service on the reader
      List<CardSelectionResponseApi> cardSelectionResponses =
          reader.transmitCardSelectionRequests(
              cardSelectionRequests, multiSelectionProcessing, channelControl);

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), JsonUtil.toJson(cardSelectionResponses));
    }

    /** Service {@link ReaderService#SCHEDULE_CARD_SELECTION_SCENARIO}. */
    private void scheduleCardSelectionScenario() {

      // Extract info from the message
      CardSelectionScenarioAdapter cardSelectionScenario =
          JsonUtil.getParser()
              .fromJson(
                  input.get(JsonProperty.CARD_SELECTION_SCENARIO.name()),
                  CardSelectionScenarioAdapter.class);

      ObservableCardReader.NotificationMode notificationMode =
          ObservableCardReader.NotificationMode.valueOf(
              input.get(JsonProperty.NOTIFICATION_MODE.name()).getAsString());

      ObservableCardReader.DetectionMode detectionMode = null;
      if (input.has(JsonProperty.POLLING_MODE.name())) {
        detectionMode =
            ObservableCardReader.DetectionMode.valueOf(
                input.get(JsonProperty.POLLING_MODE.name()).getAsString());
      }

      // Execute the service on the reader
      if (reader instanceof ObservableLocalReaderAdapter) {
        ((ObservableLocalReaderAdapter) reader)
            .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, detectionMode);
      } else if (reader instanceof ObservableRemoteReaderAdapter) {
        ((ObservableRemoteReaderAdapter) reader)
            .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, detectionMode);
      } else {
        throw new IllegalStateException(
            String.format("Reader '%s' is not observable", reader.getName()));
      }
    }

    /** Service {@link ReaderService#IS_CARD_PRESENT}. */
    private void isCardPresent() {

      // Execute the service on the reader
      boolean isCardPresent = reader.isCardPresent();

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), isCardPresent);
    }

    /** Service {@link ReaderService#IS_CONTACTLESS}. */
    private void isContactless() {

      // Execute the service on the reader
      boolean isContactless = reader.isContactless();

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), isContactless);
    }

    /** Service {@link ReaderService#START_CARD_DETECTION}. */
    private void startCardDetection() {

      // Extract info from the message
      ObservableCardReader.DetectionMode detectionMode =
          ObservableCardReader.DetectionMode.valueOf(
              input.get(JsonProperty.POLLING_MODE.name()).getAsString());

      // Execute the service on the reader
      ((ObservableCardReader) reader).addObserver(DistributedLocalServiceAdapter.this);
      ((ObservableCardReader) reader).startCardDetection(detectionMode);
    }

    /** Service {@link ReaderService#STOP_CARD_DETECTION}. */
    private void stopCardDetection() {

      // Execute the service on the reader
      ((ObservableCardReader) reader).removeObserver(DistributedLocalServiceAdapter.this);
      ((ObservableCardReader) reader).stopCardDetection();
    }

    /** Service {@link ReaderService#FINALIZE_CARD_PROCESSING}. */
    private void finalizeCardProcessing() {

      // Execute the service on the reader
      ((ObservableCardReader) reader).finalizeCardProcessing();
    }

    /**
     * Service {@link ReaderService#RELEASE_CHANNEL}.
     *
     * @throws ReaderBrokenCommunicationException If a reader communication error occurs.
     */
    private void releaseChannel() throws ReaderBrokenCommunicationException {

      // Execute the service on the reader
      reader.releaseChannel();
    }
  }

  /** Inner class used to execute a service on local plugins. */
  private final class LocalPluginExecutor {

    private final JsonObject input;
    private final JsonObject output;

    /**
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     */
    private LocalPluginExecutor(String jsonData) {
      this.input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      this.output = new JsonObject();
    }

    /**
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.add(JsonProperty.SERVICE.name(), input.get(JsonProperty.SERVICE.name()));
      try {
        checkStatus();
        PluginService service =
            PluginService.valueOf(input.get(JsonProperty.SERVICE.name()).getAsString());
        switch (service) {
          case GET_READERS:
            getReaders();
            break;
          case GET_READER_GROUP_REFERENCES:
            getReaderGroupReferences();
            break;
          case ALLOCATE_READER:
            allocateReader();
            break;
          case RELEASE_READER:
            releaseReader();
            break;
          case START_READER_DETECTION:
            startReaderDetection();
            break;
          case STOP_READER_DETECTION:
            stopReaderDetection();
            break;
          default:
            throw new IllegalArgumentException(service.name());
        }
      } catch (Exception e) {
        output.addProperty(JsonProperty.ERROR.name(), JsonUtil.toJson(new BodyError(e)));
      }
      return output.toString();
    }

    /** Service {@link PluginService#GET_READERS}. */
    private void getReaders() {

      // Execute the service on the plugins
      Map<String, Boolean> readers = new HashMap<String, Boolean>();
      for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
        for (CardReader reader : plugin.getReaders()) {
          readers.put(reader.getName(), reader instanceof ObservableCardReader);
        }
      }

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), JsonUtil.toJson(readers));
    }

    /**
     * Retrieves the pool plugin that contains the provided reader group reference.
     *
     * @param readerGroupReference The target reader group reference.
     * @return Null if no pool plugin is found containing the provided group reference.
     */
    private PoolPlugin getPoolPlugin(String readerGroupReference) {
      PoolPlugin poolPlugin;
      for (String poolPluginName : poolPluginNames) {
        poolPlugin = (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        if (poolPlugin != null
            && poolPlugin.getReaderGroupReferences().contains(readerGroupReference)) {
          return poolPlugin;
        }
      }
      return null;
    }

    /** Service {@link PluginService#GET_READER_GROUP_REFERENCES}. */
    private void getReaderGroupReferences() {

      // Execute the service on the plugins
      SortedSet<String> readerGroupReferences = new TreeSet<String>();
      for (String poolPluginName : poolPluginNames) {
        PoolPlugin poolPlugin =
            (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        readerGroupReferences.addAll(poolPlugin.getReaderGroupReferences());
      }

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), JsonUtil.toJson(readerGroupReferences));
    }

    /** Service {@link PluginService#ALLOCATE_READER}. */
    private void allocateReader() {

      // Extract info from the message
      String readerGroupReference =
          input.get(JsonProperty.READER_GROUP_REFERENCE.name()).getAsString();

      // Execute the service on the plugins
      PoolPlugin poolPlugin = getPoolPlugin(readerGroupReference);
      if (poolPlugin == null) {
        throw new IllegalStateException(
            String.format(
                "There is no local pool plugin registered having the reader group name '%s'.",
                readerGroupReference));
      }
      CardReader reader = poolPlugin.allocateReader(readerGroupReference);

      // Build result
      output.addProperty(JsonProperty.RESULT.name(), reader.getName());
    }

    /** Service {@link PluginService#RELEASE_READER}. */
    private void releaseReader() {

      // Extract info from the message
      String readerName = input.get(JsonProperty.READER_NAME.name()).getAsString();

      // Execute the service on the plugins
      PoolPlugin poolPlugin;
      for (String poolPluginName : poolPluginNames) {
        poolPlugin = (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        if (poolPlugin != null && poolPlugin.getReaderNames().contains(readerName)) {
          poolPlugin.releaseReader(poolPlugin.getReader(readerName));
        }
      }
    }

    /** Service {@link PluginService#START_READER_DETECTION}. */
    private void startReaderDetection() {

      // Start the observation of all observable local plugins.
      boolean isObservationStarted = false;
      for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
        if (plugin instanceof ObservablePlugin) {
          ((ObservablePlugin) plugin).addObserver(DistributedLocalServiceAdapter.this);
          isObservationStarted = true;
        }
      }
      if (!isObservationStarted) {
        throw new IllegalStateException("There is no observable local plugin.");
      }
    }

    /** Service {@link PluginService#STOP_READER_DETECTION}. */
    private void stopReaderDetection() {

      // Stop the observation of all observable local plugins.
      for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
        if (plugin instanceof ObservablePlugin) {
          ((ObservablePlugin) plugin).removeObserver(DistributedLocalServiceAdapter.this);
        }
      }
    }
  }
}
