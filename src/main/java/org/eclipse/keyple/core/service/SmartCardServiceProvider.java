/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

/**
 * Provider of the {@link SmartCardService}.
 *
 * @since 2.0.0
 */
public final class SmartCardServiceProvider {

  /** Private constructor */
  private SmartCardServiceProvider() {}

  /**
   * Gets the unique instance of {@link SmartCardService}.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public static SmartCardService getService() {
    return SmartCardServiceAdapter.getInstance();
  }
}
