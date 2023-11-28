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

import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a local {@link ObservablePlugin}.
 *
 * @since 2.0.0
 */
final class ObservableLocalPluginAdapter extends AbstractObservableLocalPluginAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ObservableLocalPluginAdapter.class);

  private final ObservablePluginSpi observablePluginSpi;

  /**
   * Constructor.
   *
   * @param observablePluginSpi The associated plugin SPI.
   * @since 2.0.0
   */
  ObservableLocalPluginAdapter(ObservablePluginSpi observablePluginSpi) {
    super(observablePluginSpi);
    this.observablePluginSpi = observablePluginSpi;
  }

  /**
   * Checks whether the background job is monitoring for new readers.
   *
   * @return True, if the background job is monitoring, false in all other cases.
   * @since 2.0.0
   */
  boolean isMonitoring() {
    return thread != null && thread.isAlive() && thread.isMonitoring();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {
    super.addObserver(observer);
    if (countObservers() == 1) {
      if (logger.isDebugEnabled()) {
        logger.debug("Start monitoring the plugin '{}'.", getName());
      }
      thread = new EventThread(getName());
      thread.setName("PluginEventMonitoringThread");
      thread.setUncaughtExceptionHandler(
          new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
              getObservationManager()
                  .getObservationExceptionHandler()
                  .onPluginObservationError(thread.pluginName, e);
            }
          });
      thread.start();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {
    Assert.getInstance().notNull(observer, "observer");
    if (getObservationManager().getObservers().contains(observer)) {
      super.removeObserver(observer);
      if (countObservers() == 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("Stop the plugin monitoring.");
        }
        if (thread != null) {
          thread.end();
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void clearObservers() {
    super.clearObservers();
    if (thread != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Stop the plugin monitoring.");
      }
      thread.end();
    }
  }

  /** Local thread to monitoring readers presence */
  private EventThread thread;

  /** Thread in charge of reporting live events */
  private class EventThread extends Thread {
    private final String pluginName;
    private final long monitoringCycleDuration;
    private boolean running = true;

    private EventThread(String pluginName) {
      this.pluginName = pluginName;
      monitoringCycleDuration = observablePluginSpi.getMonitoringCycleDuration();
    }

    /** Marks the thread as one that should end when the last threadWaitTimeout occurs */
    private void end() {
      running = false;
      interrupt();
    }

    /** Indicate whether the thread is running or not */
    private boolean isMonitoring() {
      return running;
    }

    /**
     * Adds a reader to the list of known readers (by the plugin)
     *
     * @param readerName The name of the reader.
     * @throws PluginIOException if an error occurs while searching the reader.
     */
    private void addReader(String readerName) throws PluginIOException {
      ReaderSpi readerSpi = observablePluginSpi.searchReader(readerName);
      LocalReaderAdapter reader = buildLocalReaderAdapter(readerSpi);
      reader.register();
      getReadersMap().put(reader.getName(), reader);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}][{}] Plugin thread => Add plugged reader to readers list.",
            pluginName,
            readerName);
      }
    }

    /** Removes a reader from the list of known readers (by the plugin) */
    private void removeReader(CardReader reader) {
      ((LocalReaderAdapter) reader).unregister();
      getReadersMap().remove(reader.getName());
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}][{}] Plugin thread => Remove unplugged reader from readers list.",
            pluginName,
            reader.getName());
      }
    }

    /** Notifies observers of changes in the list of readers */
    private void notifyChanges(PluginEvent.Type type, SortedSet<String> changedReaderNames) {
      /* grouped notification */
      if (logger.isTraceEnabled()) {
        logger.trace(
            "Notifying {}(s): {}",
            type == PluginEvent.Type.READER_CONNECTED ? "connection" : "disconnection",
            changedReaderNames);
      }
      notifyObservers(new PluginEventAdapter(pluginName, changedReaderNames, type));
    }

    /**
     * Compares the list of current readers to the list provided by the system and adds or removes
     * readers accordingly.<br>
     * Observers are notified of changes.
     *
     * @param actualNativeReaderNames the list of readers currently known by the system
     * @throws PluginIOException if an error occurs while searching readers.
     */
    private void processChanges(Set<String> actualNativeReaderNames) throws PluginIOException {
      SortedSet<String> changedReaderNames = new ConcurrentSkipListSet<String>();
      /*
       * parse the current readers list, notify for disappeared readers, update
       * readers list
       */
      final Set<CardReader> readers = getReaders();
      for (CardReader reader : readers) {
        if (!actualNativeReaderNames.contains(reader.getName())) {
          changedReaderNames.add(reader.getName());
        }
      }
      /* notify disconnections if any and update the reader list */
      if (!changedReaderNames.isEmpty()) {
        /* list update */
        for (CardReader reader : readers) {
          if (!actualNativeReaderNames.contains(reader.getName())) {
            removeReader(reader);
          }
        }
        notifyChanges(PluginEvent.Type.READER_DISCONNECTED, changedReaderNames);
        /* clean the list for a possible connection notification */
        changedReaderNames.clear();
      }
      /*
       * parse the new readers list, notify for readers appearance, update readers
       * list
       */
      for (String readerName : actualNativeReaderNames) {
        if (!getReaderNames().contains(readerName)) {
          addReader(readerName);
          /* add to the notification list */
          changedReaderNames.add(readerName);
        }
      }
      /* notify connections if any */
      if (!changedReaderNames.isEmpty()) {
        notifyChanges(PluginEvent.Type.READER_CONNECTED, changedReaderNames);
      }
    }

    /**
     * Reader monitoring loop<br>
     * Checks reader insertions and removals<br>
     * Notifies observers of any changes
     */
    @Override
    public void run() {
      try {
        while (running) {
          /* retrieves the current readers names list */
          Set<String> actualNativeReaderNames = observablePluginSpi.searchAvailableReaderNames();
          /*
           * checks if it has changed this algorithm favors cases where nothing change
           */
          Set<String> currentlyRegisteredReaderNames = getReaderNames();
          if (!currentlyRegisteredReaderNames.containsAll(actualNativeReaderNames)
              || !actualNativeReaderNames.containsAll(currentlyRegisteredReaderNames)) {
            processChanges(actualNativeReaderNames);
          }
          /* sleep for a while. */
          Thread.sleep(monitoringCycleDuration);
        }
      } catch (InterruptedException e) {
        logger.info(
            "[{}] The observation of this plugin is stopped, possibly because there is no more registered observer.",
            pluginName);
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      } catch (PluginIOException e) {
        getObservationManager()
            .getObservationExceptionHandler()
            .onPluginObservationError(
                pluginName,
                new KeyplePluginException("An error occurred while monitoring the readers.", e));
      }
    }
  }
}
