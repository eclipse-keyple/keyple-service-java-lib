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

/**
 * Indicates that the plugin is not found, generally when it has not been previously registered to
 * the {@link SmartCardService}.
 *
 * @since 2.0
 */
public class KeyplePluginNotFoundException extends RuntimeException {

  /**
   * @param message the message to identify the exception context
   * @since 2.0
   */
  public KeyplePluginNotFoundException(String message) {
    super(message);
  }
}
