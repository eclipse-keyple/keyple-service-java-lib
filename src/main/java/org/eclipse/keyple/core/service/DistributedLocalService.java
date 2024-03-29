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

import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtension;

/**
 * Local Service API of the Keyple Distributed Solution.
 *
 * @since 2.0.0
 */
public interface DistributedLocalService {

  /**
   * Returns the name of the distributed local service.
   *
   * @return A not empty string.
   * @since 2.0.0
   */
  String getName();

  /**
   * Returns the specific implementation of a {@link KeypleDistributedLocalServiceExtension}
   *
   * <p><u>Note</u> : the provided argument is used at compile time to check the type consistency.
   *
   * @param distributedLocalServiceExtensionClass the specific class of the local service
   * @param <T> The type of the service extension
   * @return a {@link KeypleDistributedLocalServiceExtension}
   */
  <T extends KeypleDistributedLocalServiceExtension> T getExtension(
      Class<T> distributedLocalServiceExtensionClass);
}
