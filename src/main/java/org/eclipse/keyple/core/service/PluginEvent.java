/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import java.util.SortedSet;

/**
 * All information about a change of reader state within an {@link ObservablePlugin}.
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
public interface PluginEvent {

  /**
   * Gets the name of the plugin to which the reader that generated the event belongs.
   *
   * @return A not empty string.
   * @since 2.0.0
   */
  String getPluginName();

  /**
   * Gets the names of the readers related to the event in the form of a sorted set.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  SortedSet<String> getReaderNames();

  /**
   * Gets the plugin event type.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  Type getType();

  /**
   * The two types of reader event
   *
   * @since 2.0.0
   */
  public enum Type {

    /**
     * A reader has been connected.
     *
     * @since 2.0.0
     */
    READER_CONNECTED,

    /**
     * A reader has been disconnected.
     *
     * @since 2.0.0
     */
    READER_DISCONNECTED,

    /**
     * This plugin has become unavailable.
     *
     * @since 2.0.0
     */
    UNAVAILABLE
  }
}
