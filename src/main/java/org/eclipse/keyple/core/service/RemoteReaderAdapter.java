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
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Remote reader adapter.
 *
 * @since 2.0
 */
class RemoteReaderAdapter extends AbstractReaderAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RemoteReaderAdapter.class);

  private final RemoteReaderSpi remoteReaderSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param remoteReaderSpi The remote reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0
   */
  RemoteReaderAdapter(RemoteReaderSpi remoteReaderSpi, String pluginName) {
    super(remoteReaderSpi.getName(), remoteReaderSpi, pluginName);
    this.remoteReaderSpi = remoteReaderSpi;
  }

  /**
   * (package-private)<br>
   * Gets the associated SPI.
   *
   * @return A not null reference.
   * @since 2.0
   */
  final RemoteReaderSpi getRemoteReaderSpi() {
    return remoteReaderSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final List<KeypleCardSelectionResponse> processCardSelectionRequests(
      List<CardSelectionRequest> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderCommunicationException, CardCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(
        JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_SELECTION_REQUESTS.name());

    input.addProperty(
        JsonProperty.CARD_SELECTION_REQUESTS.name(),
        JsonUtil.getParser()
            .toJson(
                cardSelectionRequests,
                new TypeToken<ArrayList<CardSelectionRequest>>() {}.getType()));

    input.addProperty(
        JsonProperty.MULTI_SELECTION_PROCESSING.name(), multiSelectionProcessing.name());

    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());

    // Execute the remote service.
    try {
      JsonObject output = executeRemotely(input);
      return JsonUtil.getParser()
          .fromJson(
              output.get(JsonProperty.RESULT.name()).getAsString(),
              new TypeToken<ArrayList<CardSelectionResponse>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderCommunicationException e) {
      throw e;
    } catch (CardCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return Collections.emptyList();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  final CardResponse processCardRequest(CardRequest cardRequest, ChannelControl channelControl)
      throws CardCommunicationException, ReaderCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_REQUEST.name());

    input.addProperty(
        JsonProperty.CARD_REQUEST.name(),
        JsonUtil.getParser().toJson(cardRequest, CardRequest.class));

    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());

    // Execute the remote service.
    try {
      JsonObject output = executeRemotely(input);
      return JsonUtil.getParser()
          .fromJson(output.get(JsonProperty.RESULT.name()).getAsString(), CardResponse.class);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderCommunicationException e) {
      throw e;
    } catch (CardCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final boolean isContactless() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CONTACTLESS.name());

    // Execute the remote service.
    try {
      JsonObject output = executeRemotely(input);
      return output.get(JsonProperty.RESULT.name()).getAsBoolean();

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
   * @since 2.0
   */
  @Override
  public final boolean isCardPresent() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CARD_PRESENT.name());

    // Execute the remote service.
    try {
      JsonObject output = executeRemotely(input);
      return output.get(JsonProperty.RESULT.name()).getAsBoolean();

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
   * @since 2.0
   */
  @Override
  public final void activateProtocol(String readerProtocol, String applicationProtocol) {
    checkStatus();
    throw new UnsupportedOperationException(
        "The method 'activateProtocol' is not supported by the remote reader, use it only locally.");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public final void deactivateProtocol(String readerProtocol) {
    checkStatus();
    throw new UnsupportedOperationException(
        "The method 'deactivateProtocol' is not supported by the remote reader, use it only locally.");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void releaseChannel() throws ReaderCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.RELEASE_CHANNEL.name());

    // Execute the remote service.
    try {
      executeRemotely(input);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }

  /**
   * (package-private)<br>
   * Executes remotely the provided JSON input data, parses the provide JSON output data, checks if
   * the JSON contains an error and throws the embedded exception if exists.
   *
   * @param input The JSON input data to process.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   */
  final JsonObject executeRemotely(JsonObject input) throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' is sending the following JSON data : {}",
          getName(),
          getPluginName(),
          input);
    }

    String outputJson = remoteReaderSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' received the following JSON data : {}",
          getName(),
          getPluginName(),
          outputJson);
    }

    if (outputJson == null || outputJson.isEmpty()) {
      return null;
    }

    JsonObject output = JsonUtil.getParser().fromJson(outputJson, JsonObject.class);
    if (output.has(JsonProperty.ERROR.name())) {
      BodyError body =
          JsonUtil.getParser()
              .fromJson(output.get(JsonProperty.ERROR.name()).getAsString(), BodyError.class);
      throw body.getException();
    }
    return output;
  }

  /**
   * (package-private)<br>
   * Throws a runtime exception containing the provided exception.
   *
   * @param e The cause.
   * @throws RuntimeException The thrown runtime exception.
   */
  final void throwRuntimeException(Exception e) {
    throw new RuntimeException( // NOSONAR
        String.format(
            "The reader '%s' of plugin '%s' received an unexpected error : %s",
            getName(), getPluginName(), e.getMessage()),
        e);
  }
}
