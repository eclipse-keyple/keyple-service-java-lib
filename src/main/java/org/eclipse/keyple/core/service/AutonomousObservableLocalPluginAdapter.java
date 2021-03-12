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

import java.util.Set;
import org.eclipse.keyple.core.plugin.AutonomousObservablePluginManager;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;

final class AutonomousObservableLocalPluginAdapter extends AbstractObservablePluginAdapter
    implements AutonomousObservablePluginManager {

  AutonomousObservableLocalPluginAdapter(Object observablePluginSpi) {
    super(observablePluginSpi);
  }

  @Override
  public void onReaderConnected(Set<ReaderSpi> readers) {}

  @Override
  public void onReaderDisconnected(Set<String> readersNames) {}
}
