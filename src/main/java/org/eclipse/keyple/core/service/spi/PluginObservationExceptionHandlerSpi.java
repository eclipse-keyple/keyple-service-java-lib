/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.spi;

/**
 * Plugin observation error handler to be notified of exceptions that may occur during operations
 * carried out by the monitoring processes.
 *
 * <p>These exceptions can be thrown either in the internal monitoring layers of the readers or in
 * the application itself.
 *
 * @since 2.0.0
 */
public interface PluginObservationExceptionHandlerSpi {

  /**
   * Invoked when a runtime exception occurs in the observed plugin.
   *
   * @param pluginName The plugin name
   * @param e The original exception
   * @since 2.0.0
   */
  void onPluginObservationError(String pluginName, Throwable e);
}
