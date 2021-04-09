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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.distributed.remote.RemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of a local or remote {@link Plugin}.
 *
 * @param <P> The type of plugin.
 * @since 2.0
 */
class PluginAdapter<P> implements Plugin, RemotePluginApi {

  private static final Logger logger = LoggerFactory.getLogger(PluginAdapter.class);

  private final P pluginSpi;
  private final PluginSpi localPluginSpi;
  private final PoolPluginSpi localPoolPluginSpi;
  private final RemotePluginSpi remotePluginSpi;
  private final String pluginName;
  private boolean isRegistered;
  private final Map<String, Reader> readers;

  /**
   * (package-private)<br>
   * Creates an instance of {@link PluginAdapter}.
   *
   * <p>The expected plugin SPI should be either a {@link PluginSpi} or a {@link RemotePluginSpi}.
   *
   * @param pluginSpi The specific plugin SPI.
   * @throws IllegalArgumentException If the SPI is null or of an unexpected type.
   * @since 2.0
   */
  PluginAdapter(P pluginSpi) {

    this.pluginSpi = pluginSpi;

    if (pluginSpi instanceof PluginSpi) {
      this.localPluginSpi = (PluginSpi) pluginSpi;
      this.localPoolPluginSpi = null;
      this.remotePluginSpi = null;
      this.pluginName = this.localPluginSpi.getName();

    } else if (pluginSpi instanceof PoolPluginSpi) {
      this.localPluginSpi = null;
      this.localPoolPluginSpi = (PoolPluginSpi) pluginSpi;
      this.remotePluginSpi = null;
      this.pluginName = this.localPoolPluginSpi.getName();

    } else if (pluginSpi instanceof RemotePluginSpi) {
      this.localPluginSpi = null;
      this.localPoolPluginSpi = null;
      this.remotePluginSpi = (RemotePluginSpi) pluginSpi;
      this.pluginName = this.remotePluginSpi.getName();

    } else {
      throw new IllegalArgumentException("Unexpected plugin SPI type.");
    }

    this.readers = new ConcurrentHashMap<String, Reader>();
  }

  /**
   * (package-private)<br>
   * Check if the plugin is registered.
   *
   * @throws IllegalStateException is thrown when plugin is not or no longer registered.
   * @since 2.0
   */
  final void checkStatus() {
    if (!isRegistered) {
      throw new IllegalStateException(
          String.format("The plugin '%s' is not or no longer registered.", getName()));
    }
  }

  /**
   * (package-private)<br>
   * Registers the plugin, populates its list of readers and registers each of them.
   *
   * @throws PluginIOException If registration failed.
   * @since 2.0
   */
  final void register() throws PluginIOException {
    isRegistered = true;
    if (localPluginSpi != null) {
      registerLocal();
    } else if (remotePluginSpi != null) {
      registerRemote();
    }
  }

  /**
   * (private)<br>
   * Registers local plugin.
   *
   * @throws PluginIOException If registration failed.
   */
  private void registerLocal() throws PluginIOException {

    Set<ReaderSpi> readerSpis = localPluginSpi.searchAvailableReaders();

    for (ReaderSpi readerSpi : readerSpis) {
      LocalReaderAdapter localReaderAdapter;
      if (readerSpi instanceof ObservableReaderSpi) {
        localReaderAdapter =
            new ObservableLocalReaderAdapter((ObservableReaderSpi) readerSpi, pluginName);
      } else {
        localReaderAdapter = new LocalReaderAdapter(readerSpi, pluginName);
      }
      readers.put(readerSpi.getName(), localReaderAdapter);
      localReaderAdapter.register();
    }
  }

  /**
   * (private)<br>
   * Registers remote plugin.
   */
  private void registerRemote() {

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
        remoteReaderAdapter = new ObservableRemoteReaderAdapter(remoteReaderSpi, null, pluginName);
      } else {
        remoteReaderAdapter = new RemoteReaderAdapter(remoteReaderSpi, pluginName);
      }
      readers.put(remoteReaderSpi.getName(), remoteReaderAdapter);
      remoteReaderAdapter.register();
    }
  }

  /**
   * (package-private)<br>
   * Unregisters the plugin and the readers present in its list.
   *
   * @since 2.0
   */
  void unregister() {
    isRegistered = false;
    for (String key : readers.keySet()) {
      Reader reader = readers.remove(key);
      ((AbstractReaderAdapter) reader).unregister();
    }
    if (localPluginSpi != null) {
      localPluginSpi.unregister();
    } else if (localPoolPluginSpi != null) {
      localPoolPluginSpi.unregister();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final String getName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionType) {
    checkStatus();
    return (T) pluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Map<String, Reader> getReaders() {
    checkStatus();
    return readers;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Set<String> getReadersNames() {
    checkStatus();
    return readers.keySet();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Reader getReader(String name) {
    checkStatus();
    return readers.get(name);
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
          "The plugin '{}' is receiving the following reader event : {}", pluginName, jsonData);
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
    Reader reader = readers.get(readerEvent.getReaderName());
    if (!(reader instanceof ObservableReader)) {
      throw new IllegalArgumentException(
          String.format(
              "The reader '%s' does not exists or is not observable : %s",
              readerEvent.getReaderName(), reader));
    }

    // Notify the observers.
    if (localPluginSpi != null) {
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
