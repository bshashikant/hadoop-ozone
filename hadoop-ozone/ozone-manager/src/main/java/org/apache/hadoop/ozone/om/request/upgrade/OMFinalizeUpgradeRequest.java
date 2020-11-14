/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.ozone.om.request.upgrade;

import static org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.Type.FinalizeUpgrade;

import org.apache.hadoop.ozone.om.OzoneManager;
import org.apache.hadoop.ozone.om.ratis.utils.OzoneManagerDoubleBufferHelper;
import org.apache.hadoop.ozone.om.request.OMClientRequest;
import org.apache.hadoop.ozone.om.request.util.OmResponseUtil;
import org.apache.hadoop.ozone.om.response.OMClientResponse;
import org.apache.hadoop.ozone.om.response.upgrade.OMFinalizeUpgradeResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.FinalizeUpgradeRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.FinalizeUpgradeResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.UpgradeFinalizationStatus;
import org.apache.hadoop.ozone.upgrade.UpgradeFinalizer.StatusAndMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles finalizeUpgrade request.
 */
public class OMFinalizeUpgradeRequest extends OMClientRequest {
  private static final Logger LOG =
      LoggerFactory.getLogger(OMFinalizeUpgradeRequest.class);

  public OMFinalizeUpgradeRequest(OMRequest omRequest) {
    super(omRequest);
  }

  @Override
  public OMClientResponse validateAndUpdateCache(
      OzoneManager ozoneManager, long transactionLogIndex,
      OzoneManagerDoubleBufferHelper ozoneManagerDoubleBufferHelper) {
    LOG.trace("Request: {}", getOmRequest());
    OMResponse.Builder responseBuilder =
        OmResponseUtil.getOMResponseBuilder(getOmRequest());
    responseBuilder.setCmdType(FinalizeUpgrade);
    OMClientResponse response = null;

    try {
      FinalizeUpgradeRequest request =
          getOmRequest().getFinalizeUpgradeRequest();

      String upgradeClientID = request.getUpgradeClientId();

      StatusAndMessages omStatus =
          ozoneManager.finalizeUpgrade(upgradeClientID);

      UpgradeFinalizationStatus.Status protoStatus =
          UpgradeFinalizationStatus.Status.valueOf(omStatus.status().name());
      UpgradeFinalizationStatus responseStatus =
          UpgradeFinalizationStatus.newBuilder()
              .setStatus(protoStatus)
              .build();

      FinalizeUpgradeResponse omResponse =
          FinalizeUpgradeResponse.newBuilder()
              .setStatus(responseStatus)
              .build();
      responseBuilder.setFinalizeUpgradeResponse(omResponse);
      response = new OMFinalizeUpgradeResponse(responseBuilder.build());
      LOG.trace("Returning response: {}", response);
    } catch (IOException e) {
      response = new OMFinalizeUpgradeResponse(
          createErrorOMResponse(responseBuilder, e));
    }

    return response;
  }

  public static String getRequestType() {
    return FinalizeUpgrade.name();
  }
}