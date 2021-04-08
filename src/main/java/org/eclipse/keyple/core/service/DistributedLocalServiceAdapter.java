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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtension;
import org.eclipse.keyple.core.distributed.local.LocalServiceApi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of public {@link DistributedLocalService} API.
 *
 * @since 2.0
 */
final class DistributedLocalServiceAdapter
    implements DistributedLocalService, PluginObserverSpi, ReaderObserverSpi, LocalServiceApi {

  private static final Logger logger =
      LoggerFactory.getLogger(DistributedLocalServiceAdapter.class);

  private static final String SERVICE = "service";
  private static final String RESULT = "result";
  private static final String ERROR = "error";

  private final String name;
  private final LocalServiceSpi localServiceSpi;

  private List<String> poolPluginNames;
  private boolean isRegistered;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param localServiceSpi The associated SPI.
   * @since 2.0
   */
  DistributedLocalServiceAdapter(LocalServiceSpi localServiceSpi) {
    this.name = localServiceSpi.getName();
    this.localServiceSpi = localServiceSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String executeLocally(String jsonData, String readerName) {
    if (readerName != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "The distributed local service '{}' is processing the following data on the reader '{}' : {}",
            name,
            readerName,
            jsonData);
      }
      return new LocalReaderExecutor(jsonData, readerName).execute();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "The distributed local service '{}' is processing the following data on plugins : {}",
            name,
            jsonData);
      }
      return new LocalPluginExecutor(jsonData).execute();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public boolean isReaderObservable(String readerName) {
    checkStatus();
    Reader reader = getReader(readerName);
    if (reader == null) {
      throw new IllegalStateException(
          String.format(
              "There is no local reader registered with the name '%s' or the associated plugin is no longer registered.",
              readerName));
    }
    return reader instanceof ObservableReader;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void startReaderObservation(String readerName) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is starting the observation of the local reader '{}'.",
          name,
          readerName);
    }

    checkStatus();
    Reader reader = getReader(readerName);
    if (reader == null) {
      throw new IllegalStateException(
          String.format(
              "There is no local reader registered with the name '%s' or the associated plugin is no longer registered.",
              readerName));
    }
    if (!(reader instanceof ObservableReader)) {
      throw new IllegalStateException(
          String.format("The local reader '%s' is not observable.", readerName));
    }
    ((ObservableReader) reader).addObserver(this);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void stopReaderObservation(String readerName) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is stopping the observation of the local reader '{}'.",
          name,
          readerName);
    }

    Reader reader = getReader(readerName);
    if (reader instanceof ObservableReader) {
      ((ObservableReader) reader).removeObserver(this);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void startPluginsObservation() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is starting the observation of all local plugins.",
          name);
    }

    checkStatus();
    boolean isObservationStarted = false;
    for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins().values()) {
      if (plugin instanceof ObservablePlugin) {
        ((ObservablePlugin) plugin).addObserver(this);
        isObservationStarted = true;
      }
    }
    if (!isObservationStarted) {
      throw new IllegalStateException("There is no observable local plugin.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void stopPluginsObservation() {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is stopping the observation of all local plugins.",
          name);
    }

    for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins().values()) {
      if (plugin instanceof ObservablePlugin) {
        ((ObservablePlugin) plugin).removeObserver(this);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void setPoolPluginNames(String... poolPluginNames) {
    checkStatus();
    this.poolPluginNames = Arrays.asList(poolPluginNames);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public <T extends KeypleDistributedLocalServiceExtension> T getExtension(
      Class<T> distributedLocalServiceExtensionType) {
    return (T) localServiceSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is forwarding the plugin event '{}' associated to the local reader '{}' of the local plugin '{}'.",
          name,
          pluginEvent.getEventType().name(),
          pluginEvent.getReadersNames().first(),
          pluginEvent.getPluginName());
    }

    JsonObject body = new JsonObject();
    body.add("pluginEvent", JsonUtil.getParser().toJsonTree(pluginEvent));

    localServiceSpi.onPluginEvent(
        pluginEvent.getReadersNames().first(), body.toString(), pluginEvent);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderEvent(ReaderEvent readerEvent) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The distributed local service '{}' is forwarding the reader event '{}' associated to the local reader '{}' of the local plugin '{}'.",
          name,
          readerEvent.getEventType().name(),
          readerEvent.getReaderName(),
          readerEvent.getPluginName());
    }

    JsonObject body = new JsonObject();
    body.add("readerEvent", JsonUtil.getParser().toJsonTree(readerEvent));

    localServiceSpi.onReaderEvent(readerEvent.getReaderName(), body.toString(), readerEvent);
  }

  /**
   * (private)<br>
   * Retrieves the first register reader having the provided name among all plugins.
   *
   * @param readerName The name of the reader to be found.
   * @return null if no reader is found with this name.
   */
  private AbstractReaderAdapter getReader(String readerName) {
    for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins().values()) {
      try {
        return (AbstractReaderAdapter) plugin.getReader(readerName);
      } catch (IllegalStateException e) {
        // The plugin is no longer register, then continue.
      }
    }
    return null;
  }

  /**
   * (private)<br>
   * Retrieves the pool plugin that contains the provided reader group reference.
   *
   * @param readerGroupReference The target reader group reference.
   * @return null if no pool plugin is found containing the provided group reference.
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

  /**
   * (package-private)<br>
   * Registers the distributed local service.
   *
   * @since 2.0
   */
  final void register() {
    isRegistered = true;
  }

  /**
   * (package-private)<br>
   * Unregisters the distributed local service and stop all plugins and readers events observations.
   *
   * @since 2.0
   */
  void unregister() {
    isRegistered = false;
    for (Plugin plugin : SmartCardServiceProvider.getService().getPlugins().values()) {
      if (plugin instanceof ObservablePlugin) {
        ((ObservablePlugin) plugin).removeObserver(this);
      }
      for (Reader reader : plugin.getReaders().values()) {
        if (reader instanceof ObservableReader) {
          ((ObservableReader) reader).removeObserver(this);
        }
      }
    }
  }

  /**
   * (private)<br>
   * Check if the distributed local service is registered.
   *
   * @throws IllegalStateException is thrown when the distributed local service is not (or no
   *     longer) registered.
   */
  private void checkStatus() {
    if (!isRegistered) {
      throw new IllegalStateException(
          String.format(
              "The distributed local service '%s' is not or no longer registered.", name));
    }
  }

  /**
   * (package-private)<br>
   * Enumeration of the available local services that can be invoked on a local reader from the
   * remote reader.
   *
   * @since 2.0
   */
  enum ReaderService {

    /**
     * Refers to {@link ProxyReader#transmitCardRequest(CardRequest, ChannelControl)}
     *
     * @since 2.0
     */
    TRANSMIT_CARD_REQUEST,

    /**
     * Refers to {@link AbstractReaderAdapter#transmitCardSelectionRequests(List,
     * MultiSelectionProcessing, ChannelControl)}
     *
     * @since 2.0
     */
    TRANSMIT_CARD_SELECTION_REQUESTS,

    /**
     * Refers to {@link
     * ObservableLocalReaderAdapter#scheduleCardSelectionScenario(CardSelectionScenario,
     * ObservableReader.NotificationMode, ObservableReader.PollingMode)} and TODO
     * ObservableRemote...
     *
     * @since 2.0
     */
    SCHEDULE_CARD_SELECTION_SCENARIO,

    /**
     * Refers to {@link Reader#isCardPresent()}
     *
     * @since 2.0
     */
    IS_CARD_PRESENT,

    /**
     * Refers to {@link Reader#isContactless()}
     *
     * @since 2.0
     */
    IS_CONTACTLESS,

    /**
     * Refers to {@link ObservableReader#startCardDetection(ObservableReader.PollingMode)}
     *
     * @since 2.0
     */
    START_CARD_DETECTION,

    /**
     * Refers to {@link ObservableReader#startCardDetection(ObservableReader.PollingMode)}
     *
     * @since 2.0
     */
    STOP_CARD_DETECTION,

    /**
     * Refers to {@link ObservableReader#finalizeCardProcessing()}
     *
     * @since 2.0
     */
    FINALIZE_CARD_PROCESSING,

    /**
     * Refers to {@link ProxyReader#releaseChannel()}
     *
     * @since 2.0
     */
    RELEASE_CHANNEL;
  }

  /**
   * (private)<br>
   * Inner class used to execute a service on a specific local reader.
   */
  private final class LocalReaderExecutor {

    private final AbstractReaderAdapter reader;
    private final JsonObject input;
    private final JsonObject output;

    /**
     * (private)<br>
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     * @param readerName The name of the target reader.
     */
    private LocalReaderExecutor(String jsonData, String readerName) {

      this.reader = getReader(readerName);
      if (reader == null) {
        throw new IllegalStateException(
            String.format(
                "There is no local reader registered with the name '%s' or the associated plugin is no longer registered.",
                readerName));
      }
      this.input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      this.output = new JsonObject();
    }

    /**
     * (private)<br>
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.add(SERVICE, input.get(SERVICE));
      try {
        checkStatus();
        ReaderService service = ReaderService.valueOf(input.get(SERVICE).getAsString());
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
        output.add(ERROR, JsonUtil.getParser().toJsonTree(new BodyError(e)));
      }
      return output.toString();
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#TRANSMIT_CARD_REQUEST}.
     *
     * @throws CardCommunicationException If a card communication error occurs.
     * @throws ReaderCommunicationException If a reader communication error occurs. request and the
     *     card returned an unexpected code.
     */
    private void transmitCardRequest()
        throws CardCommunicationException, ReaderCommunicationException {

      // Extract info from the message
      ChannelControl channelControl =
          ChannelControl.valueOf(input.get("channelControl").getAsString());

      CardRequest cardRequest =
          JsonUtil.getParser().fromJson(input.get("cardRequest").getAsString(), CardRequest.class);

      // Execute the service on the reader
      CardResponse cardResponse = reader.transmitCardRequest(cardRequest, channelControl);

      // Build result
      output.add(RESULT, JsonUtil.getParser().toJsonTree(cardResponse, CardResponse.class));
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#TRANSMIT_CARD_SELECTION_REQUESTS}.
     *
     * @throws CardCommunicationException If a card communication error occurs.
     * @throws ReaderCommunicationException If a reader communication error occurs.
     */
    private void transmitCardSelectionRequests()
        throws CardCommunicationException, ReaderCommunicationException {

      // Extract info from the message
      List<CardSelectionRequest> cardSelectionRequests =
          JsonUtil.getParser()
              .fromJson(
                  input.get("cardSelectionRequests").getAsString(),
                  new TypeToken<ArrayList<CardSelectionRequest>>() {}.getType());

      MultiSelectionProcessing multiSelectionProcessing =
          MultiSelectionProcessing.valueOf(input.get("multiSelectionProcessing").getAsString());

      ChannelControl channelControl =
          ChannelControl.valueOf(input.get("channelControl").getAsString());

      // Execute the service on the reader
      List<KeypleCardSelectionResponse> cardSelectionResponses =
          reader.transmitCardSelectionRequests(
              cardSelectionRequests, multiSelectionProcessing, channelControl);

      // Build result
      output.add(
          RESULT,
          JsonUtil.getParser()
              .toJsonTree(
                  cardSelectionResponses,
                  new TypeToken<ArrayList<KeypleCardSelectionResponse>>() {}.getType()));
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#SCHEDULE_CARD_SELECTION_SCENARIO}.
     */
    private void scheduleCardSelectionScenario() {

      // Extract info from the message
      CardSelectionScenario cardSelectionScenario =
          JsonUtil.getParser()
              .fromJson(input.get("cardSelectionScenario"), CardSelectionScenario.class);

      ObservableReader.NotificationMode notificationMode =
          ObservableReader.NotificationMode.valueOf(input.get("notificationMode").getAsString());

      ObservableReader.PollingMode pollingMode = null;
      if (input.has("pollingMode")) {
        pollingMode = ObservableReader.PollingMode.valueOf(input.get("pollingMode").getAsString());
      }

      // Execute the service on the reader
      if (reader instanceof ObservableLocalReaderAdapter) {
        ((ObservableLocalReaderAdapter) reader)
            .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, pollingMode);
      } // TODO else if (reader instanceof ObservableRemoteReaderAdapter) {
      //  ((ObservableRemoteReaderAdapter) reader).scheduleCardSelectionScenario(
      //        cardSelectionScenario, notificationMode, pollingMode);
      // }
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#IS_CARD_PRESENT}.
     */
    private void isCardPresent() {

      // Execute the service on the reader
      boolean isCardPresent = reader.isCardPresent();

      // Build result
      output.addProperty(RESULT, isCardPresent);
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#IS_CONTACTLESS}.
     */
    private void isContactless() {

      // Execute the service on the reader
      boolean isContactless = reader.isContactless();

      // Build result
      output.addProperty(RESULT, isContactless);
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#START_CARD_DETECTION}.
     */
    private void startCardDetection() {

      // Extract info from the message
      ObservableReader.PollingMode pollingMode =
          ObservableReader.PollingMode.valueOf(input.get("pollingMode").getAsString());

      // Execute the service on the reader
      ((ObservableReader) reader).startCardDetection(pollingMode);
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#STOP_CARD_DETECTION}.
     */
    private void stopCardDetection() {

      // Execute the service on the reader
      ((ObservableReader) reader).stopCardDetection();
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#FINALIZE_CARD_PROCESSING}.
     */
    private void finalizeCardProcessing() {

      // Execute the service on the reader
      ((ObservableReader) reader).finalizeCardProcessing();
    }

    /**
     * (private)<br>
     * Service {@link ReaderService#RELEASE_CHANNEL}.
     *
     * @throws ReaderCommunicationException If a reader communication error occurs.
     */
    private void releaseChannel() throws ReaderCommunicationException {

      // Execute the service on the reader
      reader.releaseChannel();
    }
  }

  /**
   * (package-private)<br>
   * Enumeration of the available local services that can be invoked on local plugins from the
   * remote plugin.
   *
   * @since 2.0
   */
  enum PluginService {

    /**
     * Refers to {@link PoolPlugin#getReaderGroupReferences()}
     *
     * @since 2.0
     */
    GET_READER_GROUP_REFERENCES,

    /**
     * Refers to {@link PoolPlugin#allocateReader(String)}
     *
     * @since 2.0
     */
    ALLOCATE_READER,

    /**
     * Refers to {@link PoolPlugin#releaseReader(Reader)}
     *
     * @since 2.0
     */
    RELEASE_READER;
  }

  /**
   * (private)<br>
   * Inner class used to execute a service on local plugins.
   */
  private final class LocalPluginExecutor {

    private final JsonObject input;
    private final JsonObject output;

    /**
     * (private)<br>
     * Constructor.
     *
     * @param jsonData The JSON service input data.
     */
    private LocalPluginExecutor(String jsonData) {
      this.input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      this.output = new JsonObject();
    }

    /**
     * (private)<br>
     * The main method.
     *
     * @return A not null JSON string which can eventually contain an exception.
     */
    private String execute() {

      output.add(SERVICE, input.get(SERVICE));
      try {
        checkStatus();
        PluginService service = PluginService.valueOf(input.get(SERVICE).getAsString());
        switch (service) {
          case GET_READER_GROUP_REFERENCES:
            getReaderGroupReferences();
            break;
          case ALLOCATE_READER:
            allocateReader();
            break;
          case RELEASE_READER:
            releaseReader();
            break;
          default:
            throw new IllegalArgumentException(service.name());
        }
      } catch (Exception e) {
        output.add(ERROR, JsonUtil.getParser().toJsonTree(new BodyError(e)));
      }
      return output.toString();
    }

    /**
     * (private)<br>
     * Service {@link PluginService#GET_READER_GROUP_REFERENCES}.
     */
    private void getReaderGroupReferences() {

      // Execute the service on the plugins
      SortedSet<String> readerGroupReferences = new TreeSet<String>();
      for (String poolPluginName : poolPluginNames) {
        PoolPlugin poolPlugin =
            (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        readerGroupReferences.addAll(poolPlugin.getReaderGroupReferences());
      }

      // Build result
      output.add(RESULT, JsonUtil.getParser().toJsonTree(readerGroupReferences));
    }

    /**
     * (private)<br>
     * Service {@link PluginService#ALLOCATE_READER}.
     */
    private void allocateReader() {

      // Extract info from the message
      String readerGroupReference = input.get("readerGroupReference").getAsString();

      // Execute the service on the plugins
      PoolPlugin poolPlugin = getPoolPlugin(readerGroupReference);
      Reader reader = poolPlugin.allocateReader(readerGroupReference);

      // Build result
      output.addProperty(RESULT, reader.getName());
    }

    /**
     * (private)<br>
     * Service {@link PluginService#RELEASE_READER}.
     */
    private void releaseReader() {

      // Extract info from the message
      String readerName = input.get("readerName").getAsString();

      // Execute the service on the plugins
      PoolPlugin poolPlugin;
      for (String poolPluginName : poolPluginNames) {
        poolPlugin = (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(poolPluginName);
        if (poolPlugin != null && poolPlugin.getReadersNames().contains(readerName)) {
          poolPlugin.releaseReader(poolPlugin.getReader(readerName));
        }
      }
    }
  }
}
