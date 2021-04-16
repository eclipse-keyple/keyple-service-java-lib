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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class CardSelectorJsonAdapterTest {

  @BeforeClass
  public static void beforeClass() {
    JsonUtil.registerTypeAdapter(CardSelector.class, new CardSelectorJsonAdapter(), true);
  }

  @Test
  public void serialize_deserialize() {
    // create a CardSelector
    CardSelector cardSelectorIn =
        CardSelector.builder()
            .filterByCardProtocol("CARD PROTOCOL")
            .filterByAtr("12.*56")
            .filterByDfName("1122334455667788")
            .setFileOccurrence(CardSelector.FileOccurrence.FIRST)
            .setFileControlInformation(CardSelector.FileControlInformation.FCI)
            .addSuccessfulStatusCode(0x1234)
            .addSuccessfulStatusCode(0x5678)
            .build();
    // convert CardSelector to Json
    String json = JsonUtil.toJson(cardSelectorIn);
    // convert Json to CardSelector
    CardSelector cardSelectorOut = JsonUtil.getParser().fromJson(json, CardSelector.class);
    // compare the original and the result of the conversions
    assertThat(cardSelectorOut.getCardProtocol()).isEqualTo(cardSelectorIn.getCardProtocol());
    assertThat(cardSelectorOut.getAtrRegex()).isEqualTo(cardSelectorIn.getAtrRegex());
    assertThat(cardSelectorOut.getAid()).containsExactly(cardSelectorIn.getAid());
    assertThat(cardSelectorOut.getFileOccurrence()).isEqualTo(cardSelectorIn.getFileOccurrence());
    assertThat(cardSelectorOut.getFileControlInformation())
        .isEqualTo(cardSelectorIn.getFileControlInformation());
    assertThat(cardSelectorOut.getSuccessfulSelectionStatusCodes())
        .isEqualTo(cardSelectorIn.getSuccessfulSelectionStatusCodes());
  }
}
