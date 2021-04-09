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

import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.JsonProperty;
import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.PluginService;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.core.distributed.remote.RemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of a remote {@link Plugin}.
 *
 * @since 2.0
 */
class RemotePluginAdapter extends AbstractPluginAdapter implements RemotePluginApi {

  private static final Logger logger = LoggerFactory.getLogger(RemotePluginAdapter.class);

  private final RemotePluginSpi remotePluginSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param remotePluginSpi The associated SPI.
   * @since 2.0
   */
  RemotePluginAdapter(RemotePluginSpi remotePluginSpi) {
    super(remotePluginSpi.getName(), remotePluginSpi);
    this.remotePluginSpi = remotePluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Populates its list of available remote local readers already registered.
   *
   * @since 2.0
   */
  @Override
  final void register() throws PluginIOException {

    super.register();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), PluginService.GET_READERS.name());

    // Execute the remote service.
    Map<String, Boolean> localReaders;
    try {
      JsonObject output = executeRemotely(input);
      if (output == null) {
        return;
      }
      localReaders =
          JsonUtil.getParser()
              .fromJson(
                  output.get(JsonProperty.RESULT.name()).getAsString(),
                  new TypeToken<HashMap<String, Boolean>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return;
    }

    // Build a remote reader for each local reader
    for (Map.Entry<String, Boolean> entry : localReaders.entrySet()) {

      RemoteReaderSpi remoteReaderSpi =
          remotePluginSpi.createRemoteReader(entry.getKey(), entry.getValue());

      RemoteReaderAdapter remoteReaderAdapter;
      if (remoteReaderSpi.isObservable()) {
        remoteReaderAdapter = new ObservableRemoteReaderAdapter(remoteReaderSpi, null, getName());
      } else {
        remoteReaderAdapter = new RemoteReaderAdapter(remoteReaderSpi, getName());
      }
      getReaders().put(remoteReaderSpi.getName(), remoteReaderAdapter);
      remoteReaderAdapter.register();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final void onReaderEvent(String jsonData) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The plugin '{}' is receiving the following reader event : {}", getName(), jsonData);
    }

    checkStatus();
    Assert.getInstance().notEmpty(jsonData, "jsonData");

    // Extract the event.
    ReaderEvent readerEvent;
    try {
      JsonObject json = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      readerEvent =
          JsonUtil.getParser()
              .fromJson(
                  json.get(JsonProperty.READER_EVENT.name()).getAsString(), ReaderEvent.class);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format("The JSON data of the reader event is malformed : %s", e.getMessage()), e);
    }

    // Get the target reader.
    Reader reader = getReader(readerEvent.getReaderName());
    if (!(reader instanceof ObservableReader)) {
      throw new IllegalArgumentException(
          String.format(
              "The reader '%s' does not exists or is not observable : %s",
              readerEvent.getReaderName(), reader));
    }

    // Notify the observers.
    if (reader instanceof ObservableLocalReaderAdapter) {
      ((ObservableLocalReaderAdapter) reader).notifyObservers(readerEvent);
    } else {
      ((ObservableRemoteReaderAdapter) reader).notifyObservers(readerEvent);
    }
  }

  /**
   * (package-private)<br>
   * Executes remotely the provided JSON input data, parses the provide JSON output data, checks if
   * the JSON contains an error and throws the * embedded exception if exists..
   *
   * @param input The JSON input data to process.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   */
  final JsonObject executeRemotely(JsonObject input) throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug("The plugin '{}' is sending the following JSON data : {}", getName(), input);
    }

    String outputJson = remotePluginSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug("The plugin '{}' received the following JSON data : {}", getName(), outputJson);
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
            "The plugin '%s' received an unexpected error : %s", getName(), e.getMessage()),
        e);
  }
}
