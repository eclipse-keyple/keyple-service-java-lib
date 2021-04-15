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
import org.eclipse.keyple.core.util.Assert;
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

  private static final String OUTPUT = "output";

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
      JsonObject output =
          DistributedUtilAdapter.executeReaderServiceRemotely(
              input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

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
      DistributedUtilAdapter.throwRuntimeException(e);
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
      JsonObject output =
          DistributedUtilAdapter.executeReaderServiceRemotely(
              input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      return JsonUtil.getParser()
          .fromJson(output.get(JsonProperty.RESULT.name()).getAsString(), CardResponse.class);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderCommunicationException e) {
      throw e;
    } catch (CardCommunicationException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
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
      JsonObject output =
          DistributedUtilAdapter.executeReaderServiceRemotely(
              input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      return output.get(JsonProperty.RESULT.name()).getAsBoolean();

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
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
      JsonObject output =
          DistributedUtilAdapter.executeReaderServiceRemotely(
              input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      return output.get(JsonProperty.RESULT.name()).getAsBoolean();

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
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
    throw new UnsupportedOperationException(
        "The method 'activateProtocol' is not supported by the remote reader, use it only locally.");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public final void deactivateProtocol(String readerProtocol) {
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
      DistributedUtilAdapter.executeReaderServiceRemotely(
          input, remoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderCommunicationException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    }
  }
}
