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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.card.CardCommunicationException;
import org.eclipse.keyple.core.card.CardSelectionResponse;
import org.eclipse.keyple.core.card.CardSelectionScenario;
import org.eclipse.keyple.core.card.ReaderCommunicationException;
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.WaitForCardInsertionAutonomousReaderManager;
import org.eclipse.keyple.core.plugin.WaitForCardRemovalAutonomousReaderManager;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation for {@link ObservableReader}, {@link WaitForCardInsertionAutonomousReaderManager}
 * and {@link WaitForCardRemovalAutonomousReaderManager}.
 *
 * @since 2.0
 */
final class ObservableLocalReaderAdapter extends LocalReaderAdapter
    implements ObservableReader,
        WaitForCardInsertionAutonomousReaderManager,
        WaitForCardRemovalAutonomousReaderManager {

  private static final Logger logger = LoggerFactory.getLogger(ObservableLocalReaderAdapter.class);

  private static final byte[] APDU_PING_CARD_PRESENCE = {
    (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00
  };

  private final ObservableReaderSpi observableReaderSpi;
  private ReaderObservationExceptionHandlerSpi exceptionHandler;
  private final ObservableReaderStateServiceAdapter stateService;
  private List<ReaderObserverSpi> observers;
  private CardSelectionScenario cardSelectionScenario;
  private NotificationMode notificationMode;
  private PollingMode currentPollingMode;
  private ExecutorService eventNotificationExecutorService;
  /*
   * this object will be used to synchronize the access to the observers list in order to be
   * thread safe
   */
  private final Object sync = new Object();

  /**
   * (package-private)<br>
   * The events that drive the card's observation state machine.
   *
   * @since 2.0
   */
  enum InternalEvent {
    /**
     * A card has been inserted
     *
     * @since 2.0
     */
    CARD_INSERTED,
    /**
     * The card has been removed
     *
     * @since 2.0
     */
    CARD_REMOVED,
    /**
     * The application has completed the processing of the card
     *
     * @since 2.0
     */
    SE_PROCESSED,
    /**
     * The application has requested the start of card detection
     *
     * @since 2.0
     */
    START_DETECT,
    /**
     * The application has requested that card detection is to be stopped.
     *
     * @since 2.0
     */
    STOP_DETECT,
    /**
     * A timeout has occurred (not yet implemented)
     *
     * @since 2.0
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
   * @since 2.0
   */
  ObservableLocalReaderAdapter(ObservableReaderSpi observableReaderSpi, String pluginName) {
    super((ReaderSpi) observableReaderSpi, pluginName);
    this.observableReaderSpi = observableReaderSpi;
    stateService = new ObservableReaderStateServiceAdapter(this);
  }

  /**
   * (package-private)<br>
   * Gets the SPI of the reader.
   *
   * @return A not null reference.
   * @since 2.0
   */
  ObservableReaderSpi getObservableReaderSpi() {
    return observableReaderSpi;
  }

  /**
   * Check the presence of a card
   *
   * <p>This method is recommended for non-observable readers.
   *
   * <p>When the card is not present the logical and physical channels status may be refreshed
   * through a call to the processCardRemoved method.
   *
   * @return true if the card is present
   * @throws KeypleReaderCommunicationException if the communication with the reader failed.
   * @throws IllegalStateException is called when reader is no longer registered
   * @since 2.0
   */
  @Override
  public boolean isCardPresent() {
    checkStatus();
    if (super.isCardPresent()) {
      return true;
    } else {
      /*
       * if the card is no longer present but one of the channels is still open, then the
       * card removal sequence is initiated.
       */
      if (isLogicalChannelOpen() || ((ReaderSpi) observableReaderSpi).isPhysicalChannelOpen()) {
        processCardRemoved();
      }
      return false;
    }
  }

  /**
   * (package-private)<br>
   * Gets the exception handler used to notify the application of exceptions raised during the
   * observation process.
   *
   * @return A not null reference.
   * @since 2.0
   */
  ReaderObservationExceptionHandlerSpi getObservationExceptionHandler() {
    return exceptionHandler;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(ReaderObserverSpi observer) {

    Assert.getInstance().notNull(observer, "observer");

    if (logger.isTraceEnabled()) {
      logger.trace(
          "Adding '{}' as an observer of '{}'.", observer.getClass().getSimpleName(), getName());
    }

    synchronized (sync) {
      if (observers == null) {
        if (getObservationExceptionHandler() == null) {
          throw new IllegalStateException("No reader observation exception handler has been set.");
        }
        observers = new ArrayList<ReaderObserverSpi>(1);
      }
      observers.add(observer);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(ReaderObserverSpi observer) {
    if (observer == null) {
      return;
    }

    if (logger.isTraceEnabled()) {
      logger.trace("[{}] Deleting a reader observer", getName());
    }

    synchronized (sync) {
      if (observers != null) {
        observers.remove(observer);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int countObservers() {
    return observers == null ? 0 : observers.size();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {
    if (observers != null) {
      this.observers.clear();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void finalizeCardProcessing() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] start removal sequence of the reader", getName());
    }
    this.stateService.onEvent(InternalEvent.SE_PROCESSED);
  }

  /**
   * (package-private)<br>
   * If defined, the prepared {@link CardSelectionScenario} will be processed as soon as a card is
   * inserted. The result of this request set will be added to the reader event notified to the
   * application.
   *
   * <p>If it is not defined (set to null), a simple card detection will be notified in the end.
   *
   * <p>Depending on the notification policy, the observer will be notified whenever a card is
   * inserted, regardless of the selection status, or only if the current card matches the selection
   * criteria.
   *
   * @param cardSelectionScenario The card selection scenario.
   * @param notificationMode The notification policy.
   * @param pollingMode The polling policy.
   * @since 2.0
   */
  void scheduleCardSelectionScenario(
      CardSelectionScenario cardSelectionScenario,
      ObservableReader.NotificationMode notificationMode,
      ObservableReader.PollingMode pollingMode) {
    this.cardSelectionScenario = cardSelectionScenario;
    this.notificationMode = notificationMode;
    this.currentPollingMode = pollingMode;
  }

  /**
   * (package-private)<br>
   * Starts the card detection. Once activated, the application can be notified of the arrival of a
   * card.
   *
   * <p>The polling mode indicates the action to be followed after processing the card: if {@link
   * org.eclipse.keyple.core.service.ObservableReader.PollingMode#REPEATING}, the card detection is
   * restarted, if {@link org.eclipse.keyple.core.service.ObservableReader.PollingMode#SINGLESHOT},
   * the card detection is stopped until a new call to startCardDetection is made.
   *
   * @param pollingMode The polling policy.
   * @since 2.0
   */
  public void startCardDetection(ObservableReader.PollingMode pollingMode) {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] start the card Detection with pollingMode {}", getName(), pollingMode);
    }
    this.currentPollingMode = pollingMode;
    this.stateService.onEvent(InternalEvent.START_DETECT);
  }

  /**
   * Stops the card detection.
   *
   * <p>This method must be overloaded by readers depending on the particularity of their management
   * of the start of card detection.
   *
   * @since 2.0
   */
  @Override
  public void stopCardDetection() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] stop the card Detection", getName());
    }
    this.stateService.onEvent(InternalEvent.STOP_DETECT);
  }

  /**
   * This method is invoked by a {@link
   * org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionAutonomousSpi}
   * reader when a card is inserted.
   *
   * @since 2.0
   */
  public void onCardInserted() {
    this.stateService.onEvent(InternalEvent.CARD_INSERTED);
  }

  /**
   * This method is invoked by a {@link
   * org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalAutonomousSpi}
   * reader when a card is removed.
   *
   * @since 2.0
   */
  public void onCardRemoved() {
    this.stateService.onEvent(InternalEvent.CARD_REMOVED);
  }

  /**
   * Sets the optional {@link ExecutorService} to use when notifying the observers.
   *
   * <p>When the executor service is set, the observers are notified asynchronously using the thread
   * pool provided by the service.
   *
   * <p>When the executor service is not set, the observers are notified synchronously and
   * sequentially in the order they have been added.
   *
   * @param eventNotificationExecutorService The executor service provided by the application.
   * @since 2.0
   */
  public void setEventNotificationExecutorService(
      ExecutorService eventNotificationExecutorService) {
    this.eventNotificationExecutorService = eventNotificationExecutorService;
  }

  /**
   * Sets the {@link ReaderObservationExceptionHandlerSpi} used to notify the application that an
   * exception has been raised during the observation process.
   *
   * <p>After the exception handler is invoked, the observation process is terminated and no more
   * reader events will be notified.
   *
   * @param exceptionHandler The exception handler implemented by the application.
   * @since 2.0
   */
  public void setReaderObservationExceptionHandler(
      ReaderObservationExceptionHandlerSpi exceptionHandler) {
    Assert.getInstance().notNull(exceptionHandler, "exceptionHandler");
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * (package-private)<br>
   * Notify all registered observers with the provided {@link ReaderEvent}
   *
   * @param event The reader event.
   * @since 2.0
   */
  void notifyObservers(ReaderEvent event) {

    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Notifying a reader event to {} observers. EVENTNAME = {}",
          getName(),
          this.countObservers(),
          event.getEventType().name());
    }

    List<ReaderObserverSpi> observersCopy;

    synchronized (sync) {
      if (observers == null) {
        return;
      }
      observersCopy = new ArrayList<ReaderObserverSpi>(observers);
    }

    for (ReaderObserverSpi observer : observersCopy) {
      observer.onReaderEvent(event);
    }
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
   * CardSelectionResponse}.
   *
   * @return null if the card has been rejected by the card selection scenario.
   * @since 2.0
   */
  ReaderEvent processCardInserted() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] process the inserted card", getName());
    }
    if (cardSelectionScenario == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] no card selection scenario defined, notify CARD_INSERTED", getName());
      }
      /* no default request is defined, just notify the card insertion */
      return new ReaderEvent(getPluginName(), getName(), ReaderEvent.EventType.CARD_INSERTED, null);
    } else {
      /*
       * a card selection scenario is defined, send it and notify according to the notification mode
       * and the selection status
       */
      boolean aCardMatched = false;
      try {
        List<KeypleCardSelectionResponse> cardSelectionResponses =
            transmitCardSelectionRequests(
                cardSelectionScenario.getCardSelectionRequests(),
                cardSelectionScenario.getMultiSelectionProcessing(),
                cardSelectionScenario.getChannelControl());

        for (KeypleCardSelectionResponse cardSelectionResponse : cardSelectionResponses) {
          if (cardSelectionResponse != null
              && ((CardSelectionResponse) cardSelectionResponse)
                  .getSelectionStatus()
                  .hasMatched()) {
            if (logger.isTraceEnabled()) {
              logger.trace("[{}] a default selection has matched", getName());
            }
            aCardMatched = true;
            break;
          }
        }

        if (notificationMode == ObservableReader.NotificationMode.MATCHED_ONLY) {
          /* notify only if a card matched the selection, just ignore if not */
          if (aCardMatched) {
            return new ReaderEvent(
                getPluginName(),
                getName(),
                ReaderEvent.EventType.CARD_MATCHED,
                cardSelectionResponses);
          } else {
            if (logger.isTraceEnabled()) {
              logger.trace(
                  "[{}] selection hasn't matched"
                      + " do not thrown any event because of MATCHED_ONLY flag",
                  getName());
            }
            return null;
          }
        } else {
          // ObservableReader.NotificationMode.ALWAYS
          if (aCardMatched) {
            /* the card matched, notify a CARD_MATCHED event with the received response */
            return new ReaderEvent(
                getPluginName(),
                getName(),
                ReaderEvent.EventType.CARD_MATCHED,
                cardSelectionResponses);
          } else {
            /*
             * the card didn't match, notify an CARD_INSERTED event with the received
             * response
             */
            if (logger.isTraceEnabled()) {
              logger.trace(
                  "[{}] none of {} default selection matched",
                  getName(),
                  cardSelectionResponses.size());
            }
            return new ReaderEvent(
                getPluginName(),
                getName(),
                ReaderEvent.EventType.CARD_INSERTED,
                cardSelectionResponses);
          }
        }
      } catch (ReaderCommunicationException e) {
        // Notify the reader communication failure with the exception handler.
        getObservationExceptionHandler()
            .onReaderObservationError(this.getPluginName(), getName(), e);
      } catch (CardCommunicationException e) {
        // The last transmission failed, close the logical and physical channels.
        closeLogicalAndPhysicalChannels();
        // The card was removed or not read correctly, no exception raising or event notification,
        // just log.
        logger.debug(
            "An card communication exception occurred while processing the card selection scenario. {}",
            e.getMessage());
      }
    }

    // Here we close the physical channel in case it was opened for a card excluded by the selection
    // scenario.
    try {
      ((ReaderSpi) observableReaderSpi).closePhysicalChannel();
    } catch (ReaderIOException e) {
      // Notify the reader communication failure with the exception handler.
      getObservationExceptionHandler().onReaderObservationError(this.getPluginName(), getName(), e);
    }

    // no event returned
    return null;
  }

  /**
   * (package-private)<br>
   * Sends a neutral APDU to the card to check its presence. The status of the response is not
   * verified as long as the mere fact that the card responds is sufficient to indicate whether or
   * not it is present.
   *
   * <p>This method has to be called regularly until the card no longer respond.
   *
   * @return true if the card still responds, false if not
   * @since 2.0
   */
  boolean isCardPresentPing() {
    // transmits the APDU and checks for the IO exception.
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("[{}] Ping card", getName());
      }
      ((ReaderSpi) observableReaderSpi).transmitApdu(APDU_PING_CARD_PRESENCE);
    } catch (ReaderIOException e) {
      // Notify the reader communication failure with the exception handler.
      getObservationExceptionHandler().onReaderObservationError(this.getPluginName(), getName(), e);
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
   * This method is invoked when a card is removed to notify the application of the {@link
   * ReaderEvent.EventType#CARD_REMOVED} event.
   *
   * <p>It will also be invoked if {@link #isCardPresent()} is called and at least one of the
   * physical or logical channels is still open.
   *
   * @since 2.0
   */
  void processCardRemoved() {
    closeLogicalAndPhysicalChannels();
    notifyObservers(
        new ReaderEvent(getPluginName(), getName(), ReaderEvent.EventType.CARD_REMOVED, null));
  }

  /**
   * (package-private)<br>
   * Gets the current {@link org.eclipse.keyple.core.service.ObservableReader.PollingMode}.
   *
   * @return null if the polling mode has not been defined.
   * @since 2.0
   */
  ObservableReader.PollingMode getPollingMode() {
    return currentPollingMode;
  }

  /**
   * (package-private)<br>
   * Changes the state of the state machine
   *
   * @param stateId new stateId
   * @since 2.0
   */
  void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {
    this.stateService.switchState(stateId);
  }

  /**
   * (package-private)<br>
   * Get the current monitoring state
   *
   * @return current getMonitoringState
   * @since 2.0
   */
  AbstractObservableStateAdapter.MonitoringState getCurrentMonitoringState() {
    return this.stateService.getCurrentMonitoringState();
  }

  /**
   * (package-private)<br>
   * {@inheritDoc}
   *
   * <p>Notifies all observers of the UNREGISTERED event.<br>
   * Stops the card detection unconditionally.<br>
   * Shuts down the reader's executor service.
   *
   * @since 2.0
   */
  @Override
  void unregister() {
    super.unregister();
    try {
      notifyObservers(
          new ReaderEvent(getPluginName(), getName(), ReaderEvent.EventType.UNREGISTERED, null));
      stopCardDetection();
    } finally {
      clearObservers();
      stateService.shutdown();
    }
  }
}
