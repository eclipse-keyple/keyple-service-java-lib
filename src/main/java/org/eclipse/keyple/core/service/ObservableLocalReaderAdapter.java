/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import java.util.*;
import org.calypsonet.terminal.card.CardBrokenCommunicationException;
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.calypsonet.terminal.card.ReaderBrokenCommunicationException;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ReaderCommunicationException;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.WaitForCardInsertionAutonomousReaderApi;
import org.eclipse.keyple.core.plugin.WaitForCardRemovalAutonomousReaderApi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionAutonomousSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalAutonomousSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation for {@link ObservableReader}, {@link WaitForCardInsertionAutonomousReaderApi} and
 * {@link WaitForCardRemovalAutonomousReaderApi}.
 *
 * @since 2.0.0
 */
class ObservableLocalReaderAdapter extends LocalReaderAdapter
    implements ObservableReader,
        WaitForCardInsertionAutonomousReaderApi,
        WaitForCardRemovalAutonomousReaderApi {

  private static final Logger logger = LoggerFactory.getLogger(ObservableLocalReaderAdapter.class);

  private static final byte[] APDU_PING_CARD_PRESENCE = {
    (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00
  };
  public static final String READER_MONITORING_ERROR =
      "An error occurred while monitoring the reader.";

  private final ObservableReaderSpi observableReaderSpi;
  private final ObservableReaderStateServiceAdapter stateService;
  private final ObservationManagerAdapter<
          CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi>
      observationManager;

  private CardSelectionScenarioAdapter cardSelectionScenario;
  private NotificationMode notificationMode;
  private DetectionMode detectionMode;

  /**
   * (package-private)<br>
   * The events that drive the card's observation state machine.
   *
   * @since 2.0.0
   */
  enum InternalEvent {
    /**
     * A card has been inserted
     *
     * @since 2.0.0
     */
    CARD_INSERTED,
    /**
     * The card has been removed
     *
     * @since 2.0.0
     */
    CARD_REMOVED,
    /**
     * The application has completed the processing of the card
     *
     * @since 2.0.0
     */
    CARD_PROCESSED,
    /**
     * The application has requested the start of card detection
     *
     * @since 2.0.0
     */
    START_DETECT,
    /**
     * The application has requested that card detection is to be stopped.
     *
     * @since 2.0.0
     */
    STOP_DETECT,
    /**
     * A timeout has occurred (not yet implemented)
     *
     * @since 2.0.0
     */
    TIME_OUT
  }

  /**
   * (package-private)<br>
   * Creates an instance of {@link ObservableLocalReaderAdapter}.
   *
   * <p>Creates the {@link ObservableReaderStateServiceAdapter} with the possible states and their
   * implementation.
   *
   * @param observableReaderSpi The reader SPI.
   * @param pluginName The plugin name.
   * @since 2.0.0
   */
  ObservableLocalReaderAdapter(ObservableReaderSpi observableReaderSpi, String pluginName) {
    super(observableReaderSpi, pluginName);
    this.observableReaderSpi = observableReaderSpi;
    this.stateService = new ObservableReaderStateServiceAdapter(this);
    this.observationManager =
        new ObservationManagerAdapter<
            CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi>(pluginName, getName());
    if (observableReaderSpi instanceof WaitForCardInsertionAutonomousSpi) {
      ((WaitForCardInsertionAutonomousSpi) observableReaderSpi).connect(this);
    }
    if (observableReaderSpi instanceof WaitForCardRemovalAutonomousSpi) {
      ((WaitForCardRemovalAutonomousSpi) observableReaderSpi).connect(this);
    }
  }

  /**
   * (package-private)<br>
   * Gets the SPI of the reader.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  final ObservableReaderSpi getObservableReaderSpi() {
    return observableReaderSpi;
  }

  /**
   * (package-private)<br>
   * Gets the exception handler used to notify the application of exceptions raised during the
   * observation process.
   *
   * @return Null if no exception has been set.
   * @since 2.0.0
   */
  final CardReaderObservationExceptionHandlerSpi getObservationExceptionHandler() {
    return observationManager.getObservationExceptionHandler();
  }

  /**
   * (package-private)<br>
   * Gets the current {@link DetectionMode}.
   *
   * @return Null if the polling mode has not been defined.
   * @since 2.0.0
   */
  final DetectionMode getDetectionMode() {
    return detectionMode;
  }

  /**
   * (package-private)<br>
   * Get the current monitoring state
   *
   * @return current getMonitoringState
   * @since 2.0.0
   */
  final AbstractObservableStateAdapter.MonitoringState getCurrentMonitoringState() {
    return stateService.getCurrentMonitoringState();
  }

  /**
   * (package-private)<br>
   * Sends a neutral APDU to the card to check its presence. The status of the response is not
   * verified as long as the mere fact that the card responds is sufficient to indicate whether or
   * not it is present.
   *
   * <p>This method has to be called regularly until the card no longer respond.
   *
   * @return True if the card still responds, false if not
   * @since 2.0.0
   */
  final boolean isCardPresentPing() {
    // transmits the APDU and checks for the IO exception.
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] Ping card", getName());
      }
      observableReaderSpi.transmitApdu(APDU_PING_CARD_PRESENCE);
    } catch (ReaderIOException e) {
      // Notify the reader communication failure with the exception handler.
      getObservationExceptionHandler()
          .onReaderObservationError(
              getPluginName(),
              getName(),
              new ReaderCommunicationException(READER_MONITORING_ERROR, e));
    } catch (CardIOException e) {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}] Exception occurred in isCardPresentPing. Message: {}", getName(), e.getMessage());
      }
      return false;
    }
    return true;
  }

  /**
   * (package-private)<br>
   * This method is invoked by the card insertion monitoring process when a card is inserted.
   *
   * <p>It will return a {@link ReaderEvent} or null:
   *
   * <ul>
   *   <li>CARD_INSERTED: if no card selection scenario was defined.
   *   <li>CARD_MATCHED: if a card selection scenario was defined in any mode and a card matched the
   *       selection.
   *   <li>CARD_INSERTED: if a card selection scenario was defined in ALWAYS mode but no card
   *       matched the selection (the DefaultSelectionsResponse is however transmitted).
   * </ul>
   *
   * <p>It returns null if a card selection scenario is defined in MATCHED_ONLY mode but no card
   * matched the selection.
   *
   * <p>The selection data and the responses to the optional requests that may be present in the
   * card selection scenario are embedded into the {@link ReaderEvent} as a list of {@link
   * CardSelectionResponseApi}.
   *
   * @return Null if the card has been rejected by the card selection scenario.
   * @since 2.0.0
   */
  final ReaderEvent processCardInserted() {

    // RL-DET-INSNOTIF.1
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] process the inserted card", getName());
    }

    if (cardSelectionScenario == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] no card selection scenario defined, notify CARD_INSERTED", getName());
      }
      /* no default request is defined, just notify the card insertion */
      return new ReaderEventAdapter(
          getPluginName(), getName(), CardReaderEvent.Type.CARD_INSERTED, null);
    }

    // a card selection scenario is defined, send it and notify according to the notification mode
    // and the selection status
    try {
      List<CardSelectionResponseApi> cardSelectionResponses =
          transmitCardSelectionRequests(
              cardSelectionScenario.getCardSelectionRequests(),
              cardSelectionScenario.getMultiSelectionProcessing(),
              cardSelectionScenario.getChannelControl());

      if (hasACardMatched(cardSelectionResponses)) {
        return new ReaderEventAdapter(
            getPluginName(),
            getName(),
            CardReaderEvent.Type.CARD_MATCHED,
            new ScheduledCardSelectionsResponseAdapter(cardSelectionResponses));
      }

      if (notificationMode == NotificationMode.MATCHED_ONLY) {
        /* notify only if a card matched the selection, just ignore if not */
        if (logger.isTraceEnabled()) {
          logger.trace(
              "[{}] selection hasn't matched"
                  + " do not thrown any event because of MATCHED_ONLY flag",
              getName());
        }
        return null;
      }

      // the card didn't match, notify an CARD_INSERTED event with the received response
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}] none of {} default selection matched", getName(), cardSelectionResponses.size());
      }
      return new ReaderEventAdapter(
          getPluginName(),
          getName(),
          CardReaderEvent.Type.CARD_INSERTED,
          new ScheduledCardSelectionsResponseAdapter(cardSelectionResponses));

    } catch (ReaderBrokenCommunicationException e) {
      // Notify the reader communication failure with the exception handler.
      getObservationExceptionHandler()
          .onReaderObservationError(
              getPluginName(),
              getName(),
              new ReaderCommunicationException(READER_MONITORING_ERROR, e));

    } catch (CardBrokenCommunicationException e) {
      // The last transmission failed, close the logical and physical channels.
      closeLogicalAndPhysicalChannelsSilently();
      // The card was removed or not read correctly, no exception raising or event notification,
      // just log.
      logger.debug(
          "A card error or communication exception occurred while processing the card selection scenario. {}",
          e.getMessage());
    }

    // Here we close the physical channel in case it was opened for a card excluded by the selection
    // scenario.
    try {
      observableReaderSpi.closePhysicalChannel();
    } catch (ReaderIOException e) {
      // Notify the reader communication failure with the exception handler.
      getObservationExceptionHandler()
          .onReaderObservationError(
              getPluginName(),
              getName(),
              new ReaderCommunicationException(READER_MONITORING_ERROR, e));
    }

    // no event returned
    return null;
  }

  /**
   * Check if a card has matched.
   *
   * @param cardSelectionResponses The responses received.
   * @return True if a card has matched, false if not.
   */
  private boolean hasACardMatched(List<CardSelectionResponseApi> cardSelectionResponses) {
    for (CardSelectionResponseApi cardSelectionResponse : cardSelectionResponses) {
      if (cardSelectionResponse != null && cardSelectionResponse.hasMatched()) {
        if (logger.isTraceEnabled()) {
          logger.trace("[{}] a default selection has matched", getName());
        }
        return true;
      }
    }
    return false;
  }

  /**
   * (package-private)<br>
   * This method is invoked when a card is removed to notify the application of the {@link
   * CardReaderEvent.Type#CARD_REMOVED} event.
   *
   * <p>It will also be invoked if {@link #isCardPresent()} is called and at least one of the
   * physical or logical channels is still open.
   *
   * @since 2.0.0
   */
  final void processCardRemoved() {
    // RL-DET-REMNOTIF.1
    closeLogicalAndPhysicalChannelsSilently();
    notifyObservers(
        new ReaderEventAdapter(
            getPluginName(), getName(), CardReaderEvent.Type.CARD_REMOVED, null));
  }

  /**
   * (package-private)<br>
   * Changes the state of the state machine
   *
   * @param stateId new stateId
   * @since 2.0.0
   */
  final void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {
    stateService.switchState(stateId);
  }

  /**
   * (package-private)<br>
   * Notifies all registered observers with the provided {@link ReaderEvent}.
   *
   * <p>This method never throws an exception. Any errors at runtime are notified to the application
   * using the exception handler.
   *
   * @param event The reader event.
   * @since 2.0.0
   */
  final void notifyObservers(final ReaderEvent event) {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' notifies the reader event '{}' to {} observer(s).",
          getName(),
          event.getType().name(),
          countObservers());
    }

    for (CardReaderObserverSpi observer : observationManager.getObservers()) {
      notifyObserver(observer, event);
    }
  }

  /**
   * Notifies a single observer of an event.
   *
   * @param observer The observer to notify.
   * @param event The event.
   */
  private void notifyObserver(CardReaderObserverSpi observer, ReaderEvent event) {
    try {
      observer.onReaderEvent(event);
    } catch (Exception e) {
      try {
        observationManager
            .getObservationExceptionHandler()
            .onReaderObservationError(getPluginName(), getName(), e);
      } catch (Exception e2) {
        logger.error("Exception during notification", e2);
        logger.error("Original cause", e);
      }
    }
  }

  /**
   * (package-private)<br>
   * If defined, the prepared {@link CardSelectionScenarioAdapter} will be processed as soon as a
   * card is inserted. The result of this request set will be added to the reader event notified to
   * the application.
   *
   * <p>If it is not defined (set to null), a simple card detection will be notified in the end.
   *
   * <p>Depending on the notification policy, the observer will be notified whenever a card is
   * inserted, regardless of the selection status, or only if the current card matches the selection
   * criteria.
   *
   * @param cardSelectionScenario The card selection scenario.
   * @param notificationMode The notification policy.
   * @param detectionMode The polling policy (optional).
   * @since 2.0.0
   */
  final void scheduleCardSelectionScenario(
      CardSelectionScenarioAdapter cardSelectionScenario,
      NotificationMode notificationMode,
      DetectionMode detectionMode) {
    this.cardSelectionScenario = cardSelectionScenario;
    this.notificationMode = notificationMode;
    this.detectionMode = detectionMode;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Notifies all observers of the UNAVAILABLE event.<br>
   * Stops the card detection unconditionally.<br>
   * Shuts down the reader's executor service.
   *
   * @since 2.0.0
   */
  @Override
  final void unregister() {
    try {
      stopCardDetection();
      stateService.shutdown();
    } catch (Exception e) {
      logger.error("Error during the stop card detection of reader '{}'", getName(), e);
    }
    notifyObservers(
        new ReaderEventAdapter(getPluginName(), getName(), CardReaderEvent.Type.UNAVAILABLE, null));
    clearObservers();
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final boolean isCardPresent() {
    checkStatus();
    if (super.isCardPresent()) {
      return true;
    } else {
      /*
       * if the card is no longer present but one of the channels is still open, then the
       * card removal sequence is initiated.
       */
      if (isLogicalChannelOpen() || observableReaderSpi.isPhysicalChannelOpen()) {
        processCardRemoved();
      }
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void addObserver(CardReaderObserverSpi observer) {
    checkStatus();
    observationManager.addObserver(observer);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void removeObserver(CardReaderObserverSpi observer) {
    Assert.getInstance().notNull(observer, "observer");
    if (observationManager.getObservers().contains(observer)) {
      observationManager.removeObserver(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final int countObservers() {
    return observationManager.countObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void clearObservers() {
    observationManager.clearObservers();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void startCardDetection(DetectionMode detectionMode) {
    // RL-DET-REMCTRL.1
    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' starts the card detection with polling mode '{}'.",
          getName(),
          getPluginName(),
          detectionMode);
    }
    Assert.getInstance().notNull(detectionMode, "detectionMode");
    this.detectionMode = detectionMode;
    stateService.onEvent(InternalEvent.START_DETECT);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void stopCardDetection() {
    // RL-DET-REMCTRL.1
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' stops the card detection.", getName(), getPluginName());
    }
    stateService.onEvent(InternalEvent.STOP_DETECT);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  public final void finalizeCardProcessing() {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' starts the removal sequence of the card.",
          getName(),
          getPluginName());
    }
    stateService.onEvent(InternalEvent.CARD_PROCESSED);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void setReaderObservationExceptionHandler(
      CardReaderObservationExceptionHandlerSpi exceptionHandler) {
    checkStatus();
    observationManager.setObservationExceptionHandler(exceptionHandler);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void onCardInserted() {
    stateService.onEvent(InternalEvent.CARD_INSERTED);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final void onCardRemoved() {
    stateService.onEvent(InternalEvent.CARD_REMOVED);
  }
}
