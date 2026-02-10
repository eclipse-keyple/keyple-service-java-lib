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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtension;
import org.eclipse.keyple.core.distributed.local.LocalServiceApi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
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
      "There is no local reader registered with the name [%s] or the associated plugin is no longer registered";

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
    name = localServiceSpi.getName();
    this.localServiceSpi = localServiceSpi;
    localServiceSpi.connect(this);
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.3.0
   */
  @Override
  public boolean isReaderContactless(String readerName) {
    CardReader reader = SmartCardServiceProvider.getService().findReader(Pattern.quote(readerName));
    if (reader == null) {
      throw new IllegalStateException(String.format(READER_NOT_FOUND_TEMPLATE, readerName));
    }
    return reader.isContactless();
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
            "[localService={}] Processing locally data on reader [name={}, jsonData={}]",
            name,
            readerName,
            jsonData);
      }
      return new LocalReaderExecutor(jsonData, readerName).execute();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "[localService={}] Processing locally data on plugins [jsonData={}]", name, jsonData);
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
    return distributedLocalServiceExtensionClass.cast(localServiceSpi);
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
          "[localService={}] Forwarding local plugin event [pluginEvent={}, reader={}, plugin={}]",
          name,
          pluginEvent.getType().name(),
          pluginEvent.getReaderNames().first(),
          pluginEvent.getPluginName());
    }

    JsonObject body = new JsonObject();
    body.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    body.add(JsonProperty.PLUGIN_EVENT.getKey(), JsonUtil.getParser().toJsonTree(pluginEvent));

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
          "[localService={}] Forwarding local reader event [readerEvent={}, reader={}, plugin={}]",
          name,
          readerEvent.getType().name(),
          readerEvent.getReaderName(),
          ((ReaderEventAdapter) readerEvent).getPluginName());
    }

    JsonObject body = new JsonObject();
    body.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    body.add(JsonProperty.READER_EVENT.getKey(), JsonUtil.getParser().toJsonTree(readerEvent));

    localServiceSpi.onReaderEvent(readerEvent.getReaderName(), body.toString());
  }

  /**
   * Registers the distributed local service.
   *
   * @since 2.0.0
   */
  void register() {
    int distributedApiLevel = localServiceSpi.exchangeApiLevel(CORE_API_LEVEL);
    logger.info(
        "[localService={}] Registering distributed local service [coreApiLevel={}, localServiceApiLevel={}]",
        name,
        CORE_API_LEVEL,
        distributedApiLevel);
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
          String.format("Service [%s] is not or no longer registered", name));
    }
  }

  /** Inner class used to execute a service on a specific local reader. */
  private final class LocalReaderExecutor {

    private final AbstractReaderAdapter reader;
    private final JsonObject input;
    private final JsonObject output;
    private final int inputCoreApiLevel;

    /**
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     * @param readerName The name of the target reader.
     */
    private LocalReaderExecutor(String jsonData, String readerName) {

      reader =
          (AbstractReaderAdapter)
              SmartCardServiceProvider.getService().findReader(Pattern.quote(readerName));
      if (reader == null) {
        throw new IllegalStateException(String.format(READER_NOT_FOUND_TEMPLATE, readerName));
      }
      input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      output = new JsonObject();
      if (input.has(JsonProperty.CORE_API_LEVEL.getKey())) {
        inputCoreApiLevel = input.get(JsonProperty.CORE_API_LEVEL.getKey()).getAsInt();
      } else {
        inputCoreApiLevel = 1;
      }
    }

    /**
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), inputCoreApiLevel);
      output.add(JsonProperty.SERVICE.getKey(), input.get(JsonProperty.SERVICE.getKey()));
      try {
        checkStatus();
        ReaderService service =
            ReaderService.valueOf(input.get(JsonProperty.SERVICE.getKey()).getAsString());
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
        output.add(JsonProperty.ERROR.getKey(), JsonUtil.getParser().toJsonTree(new BodyError(e)));
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
        throws CardBrokenCommunicationException,
            ReaderBrokenCommunicationException,
            UnexpectedStatusWordException {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      ChannelControl channelControl =
          ChannelControl.valueOf(params.get(JsonProperty.CHANNEL_CONTROL.getKey()).getAsString());

      CardRequestSpi cardRequest =
          JsonUtil.getParser()
              .fromJson(
                  params.getAsJsonObject(JsonProperty.CARD_REQUEST.getKey()).toString(),
                  CardRequest.class);

      // Execute the service on the reader
      CardResponseApi cardResponse = reader.transmitCardRequest(cardRequest, channelControl);

      // Build result
      output.add(JsonProperty.RESULT.getKey(), JsonUtil.getParser().toJsonTree(cardResponse));
    }

    /**
     * Service {@link ReaderService#TRANSMIT_CARD_SELECTION_REQUESTS}.
     *
     * @throws CardBrokenCommunicationException If a card communication error occurs.
     * @throws ReaderBrokenCommunicationException If a reader communication error occurs.
     */
    private void transmitCardSelectionRequests()
        throws CardBrokenCommunicationException, ReaderBrokenCommunicationException {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      MultiSelectionProcessing multiSelectionProcessing =
          MultiSelectionProcessing.valueOf(
              params.get(JsonProperty.MULTI_SELECTION_PROCESSING.getKey()).getAsString());

      ChannelControl channelControl =
          ChannelControl.valueOf(params.get(JsonProperty.CHANNEL_CONTROL.getKey()).getAsString());

      // Card selectors
      List<String> cardSelectorsTypes =
          JsonUtil.getParser()
              .fromJson(
                  params.get(JsonProperty.CARD_SELECTORS_TYPES.getKey()).getAsJsonArray(),
                  new TypeToken<ArrayList<String>>() {}.getType());

      JsonArray cardSelectorsJsonArray =
          params.get(JsonProperty.CARD_SELECTORS.getKey()).getAsJsonArray();

      List<CardSelector<?>> cardSelectors = new ArrayList<>(cardSelectorsTypes.size());
      for (int i = 0; i < cardSelectorsTypes.size(); i++) {
        CardSelector<?> cardSelector;
        try {
          Class<?> classOfCardSelector = Class.forName(cardSelectorsTypes.get(i));
          cardSelector =
              (CardSelector<?>)
                  JsonUtil.getParser().fromJson(cardSelectorsJsonArray.get(i), classOfCardSelector);
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException(
              "Original CardSelector type " + cardSelectorsTypes.get(i) + " not found", e);
        }
        cardSelectors.add(cardSelector);
      }

      List<CardSelectionRequestSpi> cardSelectionRequests =
          JsonUtil.getParser()
              .fromJson(
                  params.getAsJsonArray(JsonProperty.CARD_SELECTION_REQUESTS.getKey()).toString(),
                  new TypeToken<ArrayList<CardSelectionRequest>>() {}.getType());

      // Execute the service on the reader
      List<CardSelectionResponseApi> cardSelectionResponses =
          reader.transmitCardSelectionRequests(
              cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl);

      // Build result
      output.add(
          JsonProperty.RESULT.getKey(), JsonUtil.getParser().toJsonTree(cardSelectionResponses));
    }

    /** Service {@link ReaderService#SCHEDULE_CARD_SELECTION_SCENARIO}. */
    private void scheduleCardSelectionScenario() {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      CardSelectionScenarioAdapter cardSelectionScenario =
          JsonUtil.getParser()
              .fromJson(
                  params.get(JsonProperty.CARD_SELECTION_SCENARIO.getKey()),
                  CardSelectionScenarioAdapter.class);

      ObservableCardReader.NotificationMode notificationMode =
          ObservableCardReader.NotificationMode.valueOf(
              params.get(JsonProperty.NOTIFICATION_MODE.getKey()).getAsString());

      // Execute the service on the reader
      if (reader instanceof ObservableLocalReaderAdapter) {
        ((ObservableLocalReaderAdapter) reader)
            .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode);
      } else if (reader instanceof ObservableRemoteReaderAdapter) {
        ((ObservableRemoteReaderAdapter) reader)
            .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode);
      } else {
        throw new IllegalStateException(
            String.format("Reader [%s] is not observable", reader.getName()));
      }
    }

    /** Service {@link ReaderService#IS_CARD_PRESENT}. */
    private void isCardPresent() {

      // Execute the service on the reader
      boolean isCardPresent = reader.isCardPresent();

      // Build result
      output.addProperty(JsonProperty.RESULT.getKey(), isCardPresent);
    }

    /** Service {@link ReaderService#IS_CONTACTLESS}. */
    private void isContactless() {

      // Execute the service on the reader
      boolean isContactless = reader.isContactless();

      // Build result
      output.addProperty(JsonProperty.RESULT.getKey(), isContactless);
    }

    /** Service {@link ReaderService#START_CARD_DETECTION}. */
    private void startCardDetection() {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      ObservableCardReader.DetectionMode detectionMode =
          ObservableCardReader.DetectionMode.valueOf(
              params.get(JsonProperty.POLLING_MODE.getKey()).getAsString());

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
    private final int inputCoreApiLevel;

    /**
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     */
    private LocalPluginExecutor(String jsonData) {
      input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      output = new JsonObject();
      if (input.has(JsonProperty.CORE_API_LEVEL.getKey())) {
        inputCoreApiLevel = input.get(JsonProperty.CORE_API_LEVEL.getKey()).getAsInt();
      } else {
        inputCoreApiLevel = 1;
      }
    }

    /**
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), inputCoreApiLevel);
      output.add(JsonProperty.SERVICE.getKey(), input.get(JsonProperty.SERVICE.getKey()));
      try {
        checkStatus();
        PluginService service =
            PluginService.valueOf(input.get(JsonProperty.SERVICE.getKey()).getAsString());
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
        output.add(JsonProperty.ERROR.getKey(), JsonUtil.getParser().toJsonTree(new BodyError(e)));
      }
      return output.toString();
    }

    /** Service {@link PluginService#GET_READERS}. */
    private void getReaders() {

      // Execute the service on the plugins
      Map<String, Boolean> readers = new HashMap<>();
      for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins()) {
        for (CardReader reader : plugin.getReaders()) {
          readers.put(reader.getName(), reader instanceof ObservableCardReader);
        }
      }

      // Build result
      output.add(JsonProperty.RESULT.getKey(), JsonUtil.getParser().toJsonTree(readers));
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
      SortedSet<String> readerGroupReferences = new TreeSet<>();
      for (String poolPluginName : poolPluginNames) {
        PoolPlugin poolPlugin =
            (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        readerGroupReferences.addAll(poolPlugin.getReaderGroupReferences());
      }

      // Build result
      output.add(
          JsonProperty.RESULT.getKey(), JsonUtil.getParser().toJsonTree(readerGroupReferences));
    }

    /** Service {@link PluginService#ALLOCATE_READER}. */
    private void allocateReader() {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      String readerGroupReference =
          params.get(JsonProperty.READER_GROUP_REFERENCE.getKey()).getAsString();

      // Execute the service on the plugins
      PoolPlugin poolPlugin = getPoolPlugin(readerGroupReference);
      if (poolPlugin == null) {
        throw new IllegalStateException(
            String.format(
                "There is no local pool plugin registered having the reader group name [%s]",
                readerGroupReference));
      }
      CardReader reader = poolPlugin.allocateReader(readerGroupReference);

      // Build result
      JsonObject result = new JsonObject();

      // Reader name
      result.addProperty(JsonProperty.READER_NAME.getKey(), reader.getName());

      // Selected smart card
      SmartCard selectedSmartCard = poolPlugin.getSelectedSmartCard(reader);
      if (selectedSmartCard != null) {
        result.addProperty(
            JsonProperty.SELECTED_SMART_CARD_CLASS_NAME.getKey(),
            selectedSmartCard.getClass().getName());
        result.add(
            JsonProperty.SELECTED_SMART_CARD.getKey(),
            JsonUtil.getParser().toJsonTree(selectedSmartCard));
      }

      output.add(JsonProperty.RESULT.getKey(), result);
    }

    /** Service {@link PluginService#RELEASE_READER}. */
    private void releaseReader() {

      // Extract parameters from the message
      JsonObject params = input.getAsJsonObject(JsonProperty.PARAMETERS.getKey());

      String readerName = params.get(JsonProperty.READER_NAME.getKey()).getAsString();

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
        throw new IllegalStateException("There is no observable local plugin");
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
