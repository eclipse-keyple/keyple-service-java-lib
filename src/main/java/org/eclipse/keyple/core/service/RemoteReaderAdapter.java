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
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote reader adapter.
 *
 * @since 2.0.0
 */
class RemoteReaderAdapter extends AbstractReaderAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RemoteReaderAdapter.class);

  private static final String OUTPUT = "output";

  private final RemoteReaderSpi remoteReaderSpi;

  /**
   * Constructor.
   *
   * @param remoteReaderSpi The remote reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0.0
   */
  RemoteReaderAdapter(RemoteReaderSpi remoteReaderSpi, String pluginName) {
    super(remoteReaderSpi.getName(), (KeypleReaderExtension) remoteReaderSpi, pluginName);
    this.remoteReaderSpi = remoteReaderSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  final List<CardSelectionResponseApi> processCardSelectionRequests(
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl)
      throws ReaderBrokenCommunicationException, CardBrokenCommunicationException {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(
        JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_SELECTION_REQUESTS.name());

    input.addProperty(
        JsonProperty.CARD_SELECTION_REQUESTS.name(),
        JsonUtil.getParser().toJson(cardSelectionRequests));

    input.addProperty(
        JsonProperty.MULTI_SELECTION_PROCESSING.name(), multiSelectionProcessing.name());

    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());

    // Execute the remote service.
    try {
      JsonObject output =
          executeReaderServiceRemotely(input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      return JsonUtil.getParser()
          .fromJson(
              output.get(JsonProperty.RESULT.name()).getAsString(),
              new TypeToken<ArrayList<CardSelectionResponseAdapter>>() {}.getType());

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
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.TRANSMIT_CARD_REQUEST.name());

    input.addProperty(JsonProperty.CARD_REQUEST.name(), JsonUtil.getParser().toJson(cardRequest));

    input.addProperty(JsonProperty.CHANNEL_CONTROL.name(), channelControl.name());

    // Execute the remote service.
    try {
      JsonObject output =
          executeReaderServiceRemotely(input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

      return JsonUtil.getParser()
          .fromJson(
              output.get(JsonProperty.RESULT.name()).getAsString(), CardResponseAdapter.class);

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

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final boolean isContactless() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CONTACTLESS.name());

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
      JsonObject output =
          executeReaderServiceRemotely(input, remoteReaderSpi, getName(), getPluginName(), logger);

      Assert.getInstance().notNull(output, OUTPUT);

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
   * @since 2.0.0
   */
  @Override
  public final boolean isCardPresent() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.IS_CARD_PRESENT.name());

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
    input.addProperty(JsonProperty.SERVICE.name(), ReaderService.RELEASE_CHANNEL.name());

    // Execute the remote service.
    try {
      executeReaderServiceRemotely(input, remoteReaderSpi, getName(), getPluginName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (ReaderBrokenCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    }
  }
}
