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
package org.eclipse.keyple.core.service.util;

import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;

public class PluginExceptionHandlerMock implements PluginObservationExceptionHandlerSpi {
  boolean invoked = false;
  String pluginName;
  Throwable e;
  RuntimeException throwEx;

  public PluginExceptionHandlerMock(RuntimeException throwEx) {
    this.throwEx = throwEx;
  }

  @Override
  public void onPluginObservationError(String pluginName, Throwable e) {
    this.invoked = true;
    if (throwEx != null) {
      throw throwEx;
    }
    this.pluginName = pluginName;
    this.e = e;
  }

  public boolean isInvoked() {
    return invoked;
  }

  public String getPluginName() {
    return pluginName;
  }

  public Throwable getE() {
    return e;
  }
}
