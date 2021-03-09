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
package org.eclipse.keyple.core.service.spi;

import org.eclipse.keyple.core.service.PluginEvent;

/**
 * Plugin observer recipient of the {@link PluginEvent} from a {@link
 * org.eclipse.keyple.core.service.ObservablePlugin}.
 *
 * @since 2.0
 */
public interface PluginObserverSpi {

  /**
   * Called when a plugin event occurs.
   *
   * <p>Note that this method is called <b>sequentially</b> on all registered observers.
   *
   * @param pluginEvent The plugin event.
   * @since 2.0
   */
  void onPluginEvent(final PluginEvent pluginEvent);
}
