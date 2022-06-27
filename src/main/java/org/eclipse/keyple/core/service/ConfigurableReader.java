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

import org.calypsonet.terminal.reader.ConfigurableCardReader;

/**
 * Drives the underlying hardware to configure the protocol to use.
 *
 * @since 2.0.0
 * @deprecated Use {@link ConfigurableCardReader} instead.
 */
@Deprecated
public interface ConfigurableReader extends Reader, ConfigurableCardReader {}
