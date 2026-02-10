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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.distributed.remote.RemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemoteReaderSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a remote {@link Plugin}.
 *
 * @since 2.0.0
 */
class RemotePluginAdapter extends AbstractPluginAdapter implements RemotePluginApi {

  private static final Logger logger = LoggerFactory.getLogger(RemotePluginAdapter.class);

  private final RemotePluginSpi remotePluginSpi;

  /**
   * Constructor.
   *
   * @param remotePluginSpi The associated SPI.
   * @since 2.0.0
   */
  RemotePluginAdapter(RemotePluginSpi remotePluginSpi) {
    super(remotePluginSpi.getName(), (KeyplePluginExtension) remotePluginSpi);
    this.remotePluginSpi = remotePluginSpi;
    this.remotePluginSpi.connect(this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Populates its list of available remote local readers already registered.
   *
   * @since 2.0.0
   */
  @Override
  final void register() throws PluginIOException {

    super.register();

    int distributedApiLevel = remotePluginSpi.exchangeApiLevel(CORE_API_LEVEL);
    logger.info(
        "[plugin={}] Registering distributed remote plugin [coreApiLevel={}, remotePluginApiLevel={}]",
        getName(),
        CORE_API_LEVEL,
        distributedApiLevel);

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    input.addProperty(JsonProperty.SERVICE.getKey(), PluginService.GET_READERS.name());

    // Execute the remote service.
    Map<String, Boolean> localReaders;
    try {
      JsonObject output = executePluginServiceRemotely(input, remotePluginSpi, getName(), logger);
      if (output == null) {
        return;
      }
      localReaders =
          JsonUtil.getParser()
              .fromJson(
                  output.getAsJsonObject(JsonProperty.RESULT.getKey()).toString(),
                  new TypeToken<HashMap<String, Boolean>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return;
    }

    // Build a remote reader for each local reader
    for (Map.Entry<String, Boolean> entry : localReaders.entrySet()) {

      String localReaderName = entry.getKey();
      String remoteReaderName = localReaderName + REMOTE_READER_NAME_SUFFIX;
      boolean isObservable = entry.getValue();

      RemoteReaderAdapter remoteReaderAdapter = null;
      if (isObservable) {
        try {
          ObservableRemoteReaderSpi observableRemoteReaderSpi =
              remotePluginSpi.createObservableRemoteReader(remoteReaderName, localReaderName);
          remoteReaderAdapter =
              new ObservableRemoteReaderAdapter(
                  observableRemoteReaderSpi, getName(), CORE_API_LEVEL);
        } catch (IllegalStateException e) {
          logger.warn(
              "[plugin={}] Failed to create observable remote reader [remoteReaderName={}, localReaderName={}, reason={}]",
              getName(),
              remoteReaderName,
              localReaderName,
              e.getMessage());
          isObservable = false;
        }
      }
      if (!isObservable) {
        RemoteReaderSpi remoteReaderSpi =
            remotePluginSpi.createRemoteReader(remoteReaderName, localReaderName);
        remoteReaderAdapter =
            new RemoteReaderAdapter(remoteReaderSpi, getName(), null, CORE_API_LEVEL);
      }

      getReadersMap().put(localReaderName, remoteReaderAdapter);
      remoteReaderAdapter.register();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    try {
      remotePluginSpi.onUnregister();
    } catch (Exception e) {
      logger.warn(
          "[plugin={}] Failed to unregister plugin extension [reason={}]",
          getName(),
          e.getMessage());
    }
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void onReaderEvent(String jsonData) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug("[plugin={}] Receiving remote reader event [jsonData={}]", getName(), jsonData);
    }
    Assert.getInstance().notEmpty(jsonData, "jsonData");

    // Extract the event.
    CardReaderEvent readerEvent;
    try {
      JsonObject json = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
      readerEvent =
          JsonUtil.getParser()
              .fromJson(
                  json.getAsJsonObject(JsonProperty.READER_EVENT.getKey()).toString(),
                  ReaderEventAdapter.class);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format("JSON data of the reader event is malformed : %s", e.getMessage()), e);
    }

    // Get the target reader.
    CardReader reader = getReader(readerEvent.getReaderName());
    if (!(reader instanceof ObservableCardReader)) {
      throw new IllegalArgumentException(
          String.format(
              "Reader [%s] does not exists or is not observable: %s",
              readerEvent.getReaderName(), reader));
    }

    // Notify the observers.
    if (reader instanceof ObservableLocalReaderAdapter) {
      ((ObservableLocalReaderAdapter) reader).notifyObservers(readerEvent);
    } else {
      ((ObservableRemoteReaderAdapter) reader).notifyObservers(readerEvent);
    }
  }
}
