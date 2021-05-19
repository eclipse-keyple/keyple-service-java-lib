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

import java.util.HashSet;
import java.util.Set;

public class PluginAdapterTestUtils {

  public static final String PLUGIN_NAME = "plugin";
  public static final String READER_NAME_1 = "reader1";
  public static final String READER_NAME_2 = "reader2";
  public static final String OBSERVABLE_READER_NAME = "observableReader";
  public static final Set<String> READER_NAMES = new HashSet<String>();

  static {
    READER_NAMES.add(READER_NAME_1);
    READER_NAMES.add(READER_NAME_2);
  }
}
