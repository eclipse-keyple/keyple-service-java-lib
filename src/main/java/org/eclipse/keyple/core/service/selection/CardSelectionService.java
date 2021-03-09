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
package org.eclipse.keyple.core.service.selection;

import java.util.List;
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse;
import org.eclipse.keyple.core.service.ObservableReader;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.selection.spi.CardSelection;

/**
 * Service dedicated to card selection based on the preparation of a card selection scenario.
 *
 * <p>A card selection scenario consists of one or more selection cases based on a {@link
 * CardSelection}.<br>
 * A card selection targets a specific card and defines optional commands to be executed after the
 * successful selection of the card. <br>
 * If a card selection fails, the service will try with the next selection defined in the scenario,
 * if any. <br>
 * If a card selection succeeds, the service will execute the next selection according to the multi
 * selection processing policy defined at the factory level ({@link MultiSelectionProcessing}).
 *
 * <p>This service allows to:
 *
 * <ul>
 *   <li>prepare the card selection scenario,
 *   <li>make an explicit selection of a card (when the card is already present),
 *   <li>to schedule the execution of the selection as soon as a card is presented to an observable
 *       reader.
 * </ul>
 *
 * <p>The logical channel established with the card can be left open (default) or closed after
 * selection.
 *
 * @since 2.0
 */
public interface CardSelectionService {

  /**
   * Appends a card selection case to the card selection scenario.
   *
   * <p>The method returns the index giving the current position of the selection in the selection
   * scenario (0 for the first application, 1 for the second, etc.).
   *
   * @param cardSelection The card selection.
   * @return A positive int.
   * @since 2.0
   */
  int prepareSelection(CardSelection cardSelection);

  /**
   * Requests the closing of the physical channel at the end of the execution of the card selection
   * request.
   *
   * <p>This makes it possible to chain several selections on the same card selection scenario by
   * restarting the card connection sequence.
   *
   * @since 2.0
   */
  void prepareReleaseChannel();

  /**
   * Executes explicitly a previously prepared card selection scenario and returns the card
   * selection result.
   *
   * @param reader The reader to communicate with the card.
   * @return A not null reference.
   * @since 2.0
   */
  CardSelectionResult processCardSelectionScenario(Reader reader);

  /**
   * Schedules the execution of the prepared card selection scenario as soon as a card is presented
   * to the provided {@link ObservableReader}.
   *
   * <p>{@link org.eclipse.keyple.core.service.ReaderEvent} are notified to the observer according
   * to the specified notification policy.
   *
   * <p>The card polling policy defining the behavior of the reader at the end of the card
   * processing {@link org.eclipse.keyple.core.service.ObservableReader.PollingMode} is defaulted to
   * {@link org.eclipse.keyple.core.service.ObservableReader.PollingMode#REPEATING}.
   *
   * @param observableReader The reader with which the card communication is carried out.
   * @param notificationMode The card notification policy.
   * @since 2.0
   */
  void scheduleCardSelectionScenario(
      ObservableReader observableReader, ObservableReader.NotificationMode notificationMode);

  /**
   * Schedules the execution of the prepared card selection scenario as soon as a card is presented
   * to the provided {@link ObservableReader}.
   *
   * <p>{@link org.eclipse.keyple.core.service.ReaderEvent} are notified to the observer according
   * to the specified notification policy.
   *
   * <p>The reader's behavior at the end of the card processing is defined by the specified {@link
   * org.eclipse.keyple.core.service.ObservableReader.PollingMode}.
   *
   * @param observableReader The reader with which the card communication is carried out.
   * @param notificationMode The card notification policy.
   * @param pollingMode The card polling policy.
   * @since 2.0
   */
  void scheduleCardSelectionScenario(
      ObservableReader observableReader,
      ObservableReader.NotificationMode notificationMode,
      ObservableReader.PollingMode pollingMode);

  /**
   * Analyzes the responses received in return of the execution of a card selection scenario and
   * returns the {@link CardSelectionResult}.
   *
   * @param cardSelectionResponses The card selection scenario execution response.
   * @return A not null reference.
   * @since 2.0
   */
  CardSelectionResult processCardSelectionResponses(
      List<KeypleCardSelectionResponse> cardSelectionResponses);
}
