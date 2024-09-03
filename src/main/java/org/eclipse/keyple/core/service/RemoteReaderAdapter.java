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
import static org.eclipse.keyple.core.service.InternalLegacyDto.*;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote reader adapter.
 *
 * @since 2.0.0
 */
class RemoteReaderAdapter extends AbstractReaderAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RemoteReaderAdapter.class);

  private static final String MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED =
      "Client Core JSON API level not supported: ";
  private static final String OUTPUT = "output";

  private final RemoteReaderSpi remoteReaderSpi;
  private final SmartCard selectedSmartCard;
  private int clientCoreApiLevel;
  private Boolean isContactless;

  /**
   * Constructor.
   *
   * @param remoteReaderSpi The remote reader SPI.
   * @param pluginName The name of the plugin.
   * @param selectedSmartCard The selected smart card in case of a pool plugin (optional).
   * @param clientCoreApiLevel The JSON API level of the associated client Core layer (equal to -1
   *     if unknown).
   * @since 2.0.0
   */
  RemoteReaderAdapter(
      RemoteReaderSpi remoteReaderSpi,
      String pluginName,
      SmartCard selectedSmartCard,
      int clientCoreApiLevel) {
    super(remoteReaderSpi.getName(), (KeypleReaderExtension) remoteReaderSpi, pluginName);
    this.remoteReaderSpi = remoteReaderSpi;
    this.selectedSmartCard = selectedSmartCard;
    this.clientCoreApiLevel = clientCoreApiLevel;
    isContactless = remoteReaderSpi.isContactless();
  }

  /**
   * Returns the selected smart card in case of a pool plugin.
   *
   * @return Null if there is no selected smart card associated.
   * @since 2.2.0
   */
  final SmartCard getSelectedSmartCard() {
    return selectedSmartCard;
  }

  /**
   * Returns the JSON API level of the associated client Core layer.
   *
   * @return -1 if unknown.
   * @since 3.0.0
   */
  final int getClientCoreApiLevel() {
    return clientCoreApiLevel;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  final List<CardSelectionResponseApi> processCardSelectionRequests(
      List<CardSelector<?>> cardSelectors,
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), clientCoreApiLevel);
    switch (clientCoreApiLevel) {
      case CORE_API_LEVEL:
        input.addProperty(
            JsonProperty.SERVICE.getKey(), ReaderService.TRANSMIT_CARD_SELECTION_REQUESTS.name());

        JsonObject params = new JsonObject();
        params.addProperty(
            JsonProperty.MULTI_SELECTION_PROCESSING.getKey(), multiSelectionProcessing.name());
        params.addProperty(JsonProperty.CHANNEL_CONTROL.getKey(), channelControl.name());

        // Original card selectors
        List<String> cardSelectorsTypes = new ArrayList<>(cardSelectors.size());
        for (CardSelector<?> cardSelector : cardSelectors) {
          cardSelectorsTypes.add(cardSelector.getClass().getName());
        }
        params.add(
            JsonProperty.CARD_SELECTORS_TYPES.getKey(),
            JsonUtil.getParser().toJsonTree(cardSelectorsTypes));
        params.add(
            JsonProperty.CARD_SELECTORS.getKey(), JsonUtil.getParser().toJsonTree(cardSelectors));

        params.add(
            JsonProperty.CARD_SELECTION_REQUESTS.getKey(),
            JsonUtil.getParser().toJsonTree(cardSelectionRequests));

        input.add(JsonProperty.PARAMETERS.getKey(), params);
        break;
      case 1:
        buildProcessCardSelectionRequestsInputV1(
            cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl, input);
        break;
      case 0:
        buildProcessCardSelectionRequestsInputV0(
            cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl, input);
        break;
      case -1:
        buildProcessCardSelectionRequestsInputV1(
            cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl, input);
        buildProcessCardSelectionRequestsInputV0(
            cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl, input);
        break;
      default:
        throw new IllegalArgumentException(
            MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
    }

    // Execute the remote service.
    try {
      JsonObject output = executeReaderServiceRemotely(input, remoteReaderSpi, getName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      if (clientCoreApiLevel == -1) {
        clientCoreApiLevel = output.has(JsonProperty.RESULT.getKey()) ? 1 : 0;
      }

      switch (clientCoreApiLevel) {
        case CORE_API_LEVEL:
        case 1:
          return JsonUtil.getParser()
              .fromJson(
                  output.getAsJsonArray(JsonProperty.RESULT.getKey()).toString(),
                  new TypeToken<ArrayList<CardSelectionResponseAdapter>>() {}.getType());
        case 0:
          return JsonUtil.getParser()
              .fromJson(
                  output.get(JsonProperty.RESULT.name()).getAsString(),
                  new TypeToken<ArrayList<CardSelectionResponseAdapter>>() {}.getType());
        default:
          throw new IllegalArgumentException(
              MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderBrokenCommunicationException e) {
      throw e;
    } catch (CardBrokenCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return Collections.emptyList();
    }
  }

  private static void buildProcessCardSelectionRequestsInputV1(
      List<CardSelector<?>> cardSelectors,
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl,
      JsonObject input) {
    input.addProperty(
        JsonProperty.SERVICE.getKey(), ReaderService.TRANSMIT_CARD_SELECTION_REQUESTS.name());

    JsonObject params = new JsonObject();
    params.addProperty(
        JsonProperty.MULTI_SELECTION_PROCESSING.getKey(), multiSelectionProcessing.name());
    params.addProperty(JsonProperty.CHANNEL_CONTROL.getKey(), channelControl.name());
    params.add(
        JsonProperty.CARD_SELECTION_REQUESTS.getKey(),
        JsonUtil.getParser()
            .toJsonTree(mapToLegacyCardSelectionRequests(cardSelectors, cardSelectionRequests)));

    input.add(JsonProperty.PARAMETERS.getKey(), params);
  }

  private static void buildProcessCardSelectionRequestsInputV0(
      List<CardSelector<?>> cardSelectors,
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl,
      JsonObject input) {
    input.addProperty(
        JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_SELECTION_REQUESTS.name());
    input.addProperty(
        JsonProperty.MULTI_SELECTION_PROCESSING.name(), multiSelectionProcessing.name());
    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());
    input.addProperty(
        JsonProperty.CARD_SELECTION_REQUESTS.name(),
        JsonUtil.getParser()
            .toJson(mapToLegacyCardSelectionRequests(cardSelectors, cardSelectionRequests)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  final CardResponseApi processCardRequest(
      CardRequestSpi cardRequest, ChannelControl channelControl)
      throws CardBrokenCommunicationException, ReaderBrokenCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), clientCoreApiLevel);
    switch (clientCoreApiLevel) {
      case CORE_API_LEVEL:
        input.addProperty(
            JsonProperty.SERVICE.getKey(), ReaderService.TRANSMIT_CARD_REQUEST.name());

        JsonObject params = new JsonObject();
        params.add(
            JsonProperty.CARD_REQUEST.getKey(), JsonUtil.getParser().toJsonTree(cardRequest));
        params.addProperty(JsonProperty.CHANNEL_CONTROL.getKey(), channelControl.name());

        input.add(JsonProperty.PARAMETERS.getKey(), params);
        break;
      case 1:
        buildProcessCardRequestInputV1(cardRequest, channelControl, input);
        break;
      case 0:
        buildProcessCardRequestInputV0(cardRequest, channelControl, input);
        break;
      case -1:
        buildProcessCardRequestInputV1(cardRequest, channelControl, input);
        buildProcessCardRequestInputV0(cardRequest, channelControl, input);
        break;
      default:
        throw new IllegalArgumentException(
            MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
    }

    // Execute the remote service.
    try {
      JsonObject output = executeReaderServiceRemotely(input, remoteReaderSpi, getName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      if (clientCoreApiLevel == -1) {
        clientCoreApiLevel = output.has(JsonProperty.RESULT.getKey()) ? 1 : 0;
      }

      switch (clientCoreApiLevel) {
        case CORE_API_LEVEL:
        case 1:
          return JsonUtil.getParser()
              .fromJson(
                  output.getAsJsonObject(JsonProperty.RESULT.getKey()).toString(),
                  CardResponseAdapter.class);
        case 0:
          return JsonUtil.getParser()
              .fromJson(
                  output.get(JsonProperty.RESULT.name()).getAsString(), CardResponseAdapter.class);
        default:
          throw new IllegalArgumentException(
              MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderBrokenCommunicationException e) {
      throw e;
    } catch (CardBrokenCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return null;
    }
  }

  private static void buildProcessCardRequestInputV1(
      CardRequestSpi cardRequest, ChannelControl channelControl, JsonObject input) {
    input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.TRANSMIT_CARD_REQUEST.name());

    JsonObject params = new JsonObject();
    params.add(
        JsonProperty.CARD_REQUEST.getKey(),
        JsonUtil.getParser().toJsonTree(mapToLegacyCardRequest(cardRequest)));
    params.addProperty(JsonProperty.CHANNEL_CONTROL.getKey(), channelControl.name());

    input.add(JsonProperty.PARAMETERS.getKey(), params);
  }

  private static void buildProcessCardRequestInputV0(
      CardRequestSpi cardRequest, ChannelControl channelControl, JsonObject input) {
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_REQUEST.name());
    input.addProperty(
        JsonProperty.CARD_REQUEST.name(),
        JsonUtil.getParser().toJson(mapToLegacyCardRequest(cardRequest)));
    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final boolean isContactless() {
    if (isContactless == null) {
      isContactless = processIsContactless();
    }
    return isContactless;
  }

  private boolean processIsContactless() {
    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), clientCoreApiLevel);
    switch (clientCoreApiLevel) {
      case CORE_API_LEVEL:
      case 1:
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.IS_CONTACTLESS.name());
        break;
      case 0:
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CONTACTLESS.name());
        break;
      case -1:
        // 1
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.IS_CONTACTLESS.name());
        // 0
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CONTACTLESS.name());
        break;
      default:
        throw new IllegalArgumentException(
            MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
    }

    // Execute the remote service.
    return executeReaderBooleanServiceRemotely(input);
  }

  /**
   * Executes remote reader service for boolean result.
   *
   * @param input The input data.
   * @return The result as a boolean value.
   */
  private boolean executeReaderBooleanServiceRemotely(JsonObject input) {
    try {
      JsonObject output = executeReaderServiceRemotely(input, remoteReaderSpi, getName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      if (clientCoreApiLevel == -1) {
        clientCoreApiLevel = output.has(JsonProperty.RESULT.getKey()) ? 1 : 0;
      }

      switch (clientCoreApiLevel) {
        case CORE_API_LEVEL:
        case 1:
          return output.get(JsonProperty.RESULT.getKey()).getAsBoolean();
        case 0:
          return output.get(JsonProperty.RESULT.name()).getAsBoolean();
        default:
          throw new IllegalArgumentException(
              MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final boolean isCardPresent() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), clientCoreApiLevel);
    switch (clientCoreApiLevel) {
      case CORE_API_LEVEL:
      case 1:
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.IS_CARD_PRESENT.name());
        break;
      case 0:
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CARD_PRESENT.name());
        break;
      case -1:
        // 1
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.IS_CARD_PRESENT.name());
        // 0
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CARD_PRESENT.name());
        break;
      default:
        throw new IllegalArgumentException(
            MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
    }

    // Execute the remote service.
    return executeReaderBooleanServiceRemotely(input);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void releaseChannel() throws ReaderBrokenCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), clientCoreApiLevel);
    switch (clientCoreApiLevel) {
      case CORE_API_LEVEL:
      case 1:
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.RELEASE_CHANNEL.name());
        break;
      case 0:
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.RELEASE_CHANNEL.name());
        break;
      case -1:
        // 1
        input.addProperty(JsonProperty.SERVICE.getKey(), ReaderService.RELEASE_CHANNEL.name());
        // 0
        input.addProperty(JsonProperty.SERVICE.name(), ReaderService.RELEASE_CHANNEL.name());
        break;
      default:
        throw new IllegalArgumentException(
            MSG_CLIENT_CORE_JSON_API_LEVEL_NOT_SUPPORTED + clientCoreApiLevel);
    }

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, remoteReaderSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderBrokenCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }
}
