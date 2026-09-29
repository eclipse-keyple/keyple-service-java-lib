/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.util;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.distributed.remote.ObservableRemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.RemotePluginApi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemoteReaderSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * Simulated observable remote plugin, whose "server" provides a modifiable list of local readers
 * through the remote service "GET_READERS".
 */
public class ObservableRemotePluginSpiMock
    implements ObservableRemotePluginSpi, KeyplePluginExtension {

  private final String name;
  private final Map<String, Boolean> serverReaders = new ConcurrentHashMap<String, Boolean>();
  private final AtomicInteger getReadersCount = new AtomicInteger();
  private final List<String> createdReaderNames = new CopyOnWriteArrayList<String>();
  private volatile long getReadersDelayMillis;
  private volatile boolean isGetReadersFailing;

  public ObservableRemotePluginSpiMock(String name) {
    this.name = name;
  }

  /** Adds (or updates) a local reader on the server side. */
  public void addServerReader(String readerName, boolean isObservable) {
    serverReaders.put(readerName, isObservable);
  }

  /** Removes a local reader on the server side. */
  public void removeServerReader(String readerName) {
    serverReaders.remove(readerName);
  }

  /** Adds a delay to the processing of the remote service "GET_READERS". */
  public void setGetReadersDelayMillis(long getReadersDelayMillis) {
    this.getReadersDelayMillis = getReadersDelayMillis;
  }

  /** Makes the remote service "GET_READERS" fail. */
  public void setGetReadersFailing(boolean isGetReadersFailing) {
    this.isGetReadersFailing = isGetReadersFailing;
  }

  /** Gets the number of invocations of the remote service "GET_READERS". */
  public int getGetReadersCount() {
    return getReadersCount.get();
  }

  /** Gets the names of the local readers for which a remote reader has been created. */
  public List<String> getCreatedReaderNames() {
    return createdReaderNames;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int exchangeApiLevel(int coreApiLevel) {
    return coreApiLevel;
  }

  @Override
  public String executeRemotely(String jsonData) {
    JsonObject input = JsonUtil.getParser().fromJson(jsonData, JsonObject.class);
    if (!"GET_READERS".equals(input.get("service").getAsString())) {
      return null;
    }
    getReadersCount.incrementAndGet();
    if (getReadersDelayMillis > 0) {
      try {
        Thread.sleep(getReadersDelayMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (isGetReadersFailing) {
      throw new IllegalStateException("GET_READERS failure");
    }
    JsonObject output = new JsonObject();
    output.addProperty("coreApiLevel", 2);
    output.addProperty("service", "GET_READERS");
    output.add("result", JsonUtil.getParser().toJsonTree(serverReaders));
    return output.toString();
  }

  @Override
  public void onUnregister() {}

  @Override
  public void connect(RemotePluginApi remotePluginApi) {}

  @Override
  public void connect(ObservableRemotePluginApi observableRemotePluginApi) {}

  @Override
  public RemoteReaderSpi createRemoteReader(String remoteReaderName, String localReaderName) {
    createdReaderNames.add(localReaderName);
    return new RemoteReaderSpiMock(remoteReaderName);
  }

  @Override
  public ObservableRemoteReaderSpi createObservableRemoteReader(
      String remoteReaderName, String localReaderName) {
    createdReaderNames.add(localReaderName);
    return new ObservableRemoteReaderSpiMock(remoteReaderName);
  }

  @Override
  public ExecutorService getExecutorService() {
    return null;
  }

  @Override
  public void onStartObservation() {}

  @Override
  public void onStopObservation() {}

  /** Simulated remote reader. */
  public static class RemoteReaderSpiMock implements RemoteReaderSpi, KeypleReaderExtension {

    private final String name;

    RemoteReaderSpiMock(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Boolean isContactless() {
      return true;
    }

    @Override
    public String executeRemotely(String jsonData) {
      return null;
    }
  }

  /** Simulated observable remote reader. */
  public static class ObservableRemoteReaderSpiMock extends RemoteReaderSpiMock
      implements ObservableRemoteReaderSpi {

    ObservableRemoteReaderSpiMock(String name) {
      super(name);
    }

    @Override
    public void onStartObservation() {}

    @Override
    public void onStopObservation() {}
  }
}
