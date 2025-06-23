/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.keyple.core.util.HexUtil;
import org.junit.Test;

public class ApduResponseAdapterTest {

  ApduResponseAdapter apduResponseAdapter;
  public static String HEX_REQUEST = "123456789000";
  public static String HEX_REQUEST_DATA = "12345678";

  @Test
  public void buildApduResponseAdapter() {
    apduResponseAdapter = new ApduResponseAdapter(HexUtil.toByteArray(HEX_REQUEST));
    assertThat(apduResponseAdapter.getApdu()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST));
    assertThat(apduResponseAdapter.getStatusWord()).isEqualTo(0x9000);
    assertThat(apduResponseAdapter.getDataOut()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST_DATA));
  }
}
