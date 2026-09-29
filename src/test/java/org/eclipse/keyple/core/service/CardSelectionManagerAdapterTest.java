/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import org.eclipse.keypleother.registeredextension.TestCardExtension;
import org.eclipse.keypleother.registeredextension.TestCardSelectionExtension;
import org.junit.Before;
import org.junit.Test;

public class CardSelectionManagerAdapterTest {

  private CardSelectionManagerAdapter manager;

  @Before
  public void setUp() {
    manager =
        (CardSelectionManagerAdapter)
            SmartCardServiceProvider.getService()
                .getReaderApiFactory()
                .createCardSelectionManager();
  }

  @Test(expected = IllegalStateException.class)
  public void exportProcessedCardSelectionScenario_whenScenarioIsNotProcessed_shouldThrowISE() {
    manager.exportProcessedCardSelectionScenario();
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsNull_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsNull2_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("null");
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsEmpty_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsMalformed_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("test");
  }

  @Test
  public void importCardSelectionScenario_whenCardSelectorTypeIsNotACardSelector_shouldThrowIAE() {
    String json =
        buildScenarioJson(
            NotACardSelector.class.getName(), BasicCardSelectorAdapter.class.getName());
    assertThatThrownBy(() -> manager.importCardSelectionScenario(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not supported");
    assertThat(unexpectedSelectorLoaded).isFalse();
  }

  @Test
  public void
      importCardSelectionScenario_whenCardSelectionTypeIsNotACardSelectionExtension_shouldThrowIAE() {
    String json =
        buildScenarioJson(
            BasicCardSelectorAdapter.class.getName(), NotACardSelection.class.getName());
    assertThatThrownBy(() -> manager.importCardSelectionScenario(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not a subtype of");
    assertThat(unexpectedSelectionLoaded).isFalse();
  }

  @Test
  public void
      importCardSelectionScenario_whenCardSelectionTypeDoesNotBelongToRegisteredExtension_shouldUseDefaultSelection()
          throws Exception {
    String json =
        buildScenarioJson(
            BasicCardSelectorAdapter.class.getName(),
            "org.eclipse.keypleother.unregisteredextension.UnregisteredCardSelectionExtension");
    manager.importCardSelectionScenario(json);
    assertThat(getCardSelections())
        .singleElement()
        .isInstanceOf(InternalDto.CardSelectionAdapter.class);
    assertThat(unregisteredTypeLoaded).isFalse();
  }

  @Test
  public void
      importCardSelectionScenario_whenCardSelectionTypeBelongsToRegisteredExtension_shouldRebuildOriginalSelection()
          throws Exception {
    SmartCardServiceProvider.getService().checkCardExtension(new TestCardExtension());
    String json =
        buildScenarioJson(
            IsoCardSelectorAdapter.class.getName(), TestCardSelectionExtension.class.getName());
    manager.importCardSelectionScenario(json);
    assertThat(getCardSelections()).singleElement().isInstanceOf(TestCardSelectionExtension.class);
  }

  private List<?> getCardSelections() throws Exception {
    Field field = CardSelectionManagerAdapter.class.getDeclaredField("cardSelections");
    field.setAccessible(true);
    return (List<?>) field.get(manager);
  }

  private static String buildScenarioJson(String cardSelectorType, String cardSelectionType) {
    return "{\"multiSelectionProcessing\":\"FIRST_MATCH\",\"channelControl\":\"KEEP_OPEN\","
        + "\"cardSelectorsTypes\":[\""
        + cardSelectorType
        + "\"],\"cardSelectors\":[{}],"
        + "\"cardSelectionsTypes\":[\""
        + cardSelectionType
        + "\"],\"cardSelections\":[{}],"
        + "\"defaultCardSelections\":[{}]}";
  }

  // Flags are held outside the test types so that reading them does not load these types
  public static boolean unregisteredTypeLoaded;
  private static boolean unexpectedSelectorLoaded;
  private static boolean unexpectedSelectionLoaded;

  public static class NotACardSelector {
    static {
      unexpectedSelectorLoaded = true;
    }

    public NotACardSelector() {
      unexpectedSelectorLoaded = true;
    }
  }

  public static class NotACardSelection {
    static {
      unexpectedSelectionLoaded = true;
    }

    public NotACardSelection() {
      unexpectedSelectionLoaded = true;
    }
  }
}
