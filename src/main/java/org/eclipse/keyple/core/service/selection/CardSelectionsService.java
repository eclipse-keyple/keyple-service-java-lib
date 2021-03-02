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

import org.eclipse.keyple.core.common.KeypleDefaultSelectionsRequest;
import org.eclipse.keyple.core.common.KeypleDefaultSelectionsResponse;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.selection.spi.CardSelection;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;

/**
 * Provides the means to prepare and execute a card selection.
 *
 * <p>It provides a way to do an explicit card selection or to post process a default card
 * selection.<br>
 * The channel is kept open by default, but can be closed after each selection cases (see
 * prepareReleaseChannel).
 *
 * @since 2.0
 */
public final class CardSelectionsService {

  /**
   * Constructor.
   *
   * @param multiSelectionProcessing the multi card processing mode
   * @since 2.0
   */
  public CardSelectionsService(MultiSelectionProcessing multiSelectionProcessing) {
    // TODO complete
  }

  /** Alternate constructor for standard usages. */
  public CardSelectionsService() {
    this(MultiSelectionProcessing.FIRST_MATCH);
  }

  /**
   * Prepare a selection: add the selection request from the provided selector to the selection
   * request set.
   *
   * <p>
   *
   * @param cardSelection the card selection to prepare
   * @return the selection index giving the current selection position in the selection request.
   * @since 2.0
   */
  public int prepareSelection(CardSelection cardSelection) {
    // TODO complete
    return 0;
  }

  /**
   * Prepare to close the card channel.<br>
   * If this command is called before a "process" selection command then the last transmission to
   * the PO will be associated with the indication CLOSE_AFTER in order to close the card channel.
   * <br>
   * This makes it possible to chain several selections on the same card if necessary.
   *
   * @since 2.0
   */
  public final void prepareReleaseChannel() {
    // TODO complete
  }

  /**
   * Execute the selection process and return a list of {@link SmartCard}.
   *
   * <p>Selection requests are transmitted to the card through the supplied Reader.
   *
   * <p>The process stops in the following cases:
   *
   * <ul>
   *   <li>All the selection requests have been transmitted
   *   <li>A selection request matches the current card and the keepChannelOpen flag was true
   * </ul>
   *
   * <p>
   *
   * @param reader the Reader on which the selection is made
   * @return A not null {@link CardSelectionsResult} reference
   * @since 2.0
   */
  public CardSelectionsResult processExplicitSelections(Reader reader) {
    // TODO complete
    return null;
  }

  /**
   * The SelectionOperation is the {@link KeypleDefaultSelectionsRequest} to process in ordered to
   * select a card among others through the selection process. This method is useful to build the
   * prepared selection to be executed by a reader just after a card insertion.
   *
   * @return the {@link KeypleDefaultSelectionsRequest} previously prepared with prepareSelection
   * @since 2.0
   */
  public KeypleDefaultSelectionsRequest getDefaultSelectionsRequest() {
    // TODO complete
    return null;
  }

  /**
   * Parses the response to a selection operation sent to a card and return a list of {@link
   * SmartCard}
   *
   * <p>Selection cases that have not matched the current card are set to null.
   *
   * @param defaultSelectionsResponse the response from the reader to the {@link
   *     KeypleDefaultSelectionsRequest}
   * @return A not null {@link CardSelectionsResult} reference
   * @since 2.0
   */
  public CardSelectionsResult processDefaultSelectionsResponse(
      KeypleDefaultSelectionsResponse defaultSelectionsResponse) {
    // TODO complete
    return null;
  }
}
