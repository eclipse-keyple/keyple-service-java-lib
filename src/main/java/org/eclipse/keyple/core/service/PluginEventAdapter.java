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

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * (package-private)<br>
 * This POJO contains all information about a change of reader state within an {@link
 * ObservablePlugin}.
 *
 * <p>The {@link #getReaderNames()} and {@link #getType()} methods allow the event recipient to
 * retrieve the names of the readers involved and the type of the event.
 *
 * <p>Since the event provides a list of reader names, a single event can be used to notify a change
 * for one or more readers.
 *
 * <p>However, only one type of event is notified at a time.
 *
 * @since 2.0.0
 */
final class PluginEventAdapter implements PluginEvent {

  private final String pluginName;
  private final SortedSet<String> readerNames;
  private final Type type;

  /**
   * (package-private)<br>
   * Create a PluginEvent for a single reader from the plugin and reader names and the type of
   * event.
   *
   * @param pluginName A string containing the name of the plugin (should be not null).
   * @param readerName A string containing the name of the reader (should be not null).
   * @param type An event type {@link Type#READER_CONNECTED} or {@link Type#READER_DISCONNECTED}
   *     (should be not null).
   * @since 2.0.0
   */
  PluginEventAdapter(String pluginName, String readerName, Type type) {
    this.pluginName = pluginName;
    this.readerNames = new TreeSet<String>(Collections.singleton(readerName));
    this.type = type;
  }

  /**
   * (package-private)<br>
   * Create a PluginEvent for multiple readers from the plugin name, multiple reader names and the
   * type of event.
   *
   * <p>Note: gathering several readers in the same event is always done for a same type of event
   * (e.g. simultaneous disconnection of 2 readers).
   *
   * @param pluginName A string containing the name of the plugin (must be not empty).
   * @param readerNames A set of string containing the readers names (must be not empty).
   * @param type An event type {@link Type#READER_CONNECTED} or {@link Type#READER_DISCONNECTED}
   *     (must be not null).
   * @since 2.0.0
   */
  PluginEventAdapter(String pluginName, Set<String> readerNames, Type type) {
    this.pluginName = pluginName;
    this.readerNames = new TreeSet<String>(readerNames);
    this.type = type;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getPluginName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SortedSet<String> getReaderNames() {
    return readerNames;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Type getType() {
    return type;
  }
}
