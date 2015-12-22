/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011-2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.retrieve.scu.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import javax.enterprise.context.ApplicationScoped;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.*;
import org.dcm4chee.archive.retrieve.scu.CMoveSCU;
import org.dcm4chee.archive.retrieve.scu.CMoveSCU.CmoveReturnState;
import org.dcm4chee.archive.retrieve.scu.CMoveSCUService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hesham Elbadawi <bsdreko@gmail.com>
 * @author Roman K
 */
@ApplicationScoped
public class CMoveSCUServiceImpl implements CMoveSCUService {

    private static final Logger log = LoggerFactory.getLogger(CMoveSCU.class);

    @Override
    public CmoveReturnState cmove(ApplicationEntity localAE,
                                  ApplicationEntity remoteAE,
                                  Attributes keys,
                                  final DimseRSPHandler handler,
                                  String destinationAET,
                                  boolean relational)
            throws IOException, InterruptedException, GeneralSecurityException, IncompatibleConnectionException {
        final CMoveSCU scu = new CMoveSCU(localAE, remoteAE);
        try {
            Association as = scu.open(relational);
            scu.cmove(keys, new DimseRSPHandler(as.nextMessageID()) {
                public void onDimseRSP(Association as, Attributes cmd, Attributes data) {

                    int status = cmd.getInt(Tag.Status, -1);
                    int numberOfSubOps = cmd.getInt(Tag.NumberOfCompletedSuboperations, -1);
                    int numberOfWarningSubOps = cmd.getInt(Tag.NumberOfWarningSuboperations, -1);
                    int numberOfFailedSubOps = cmd.getInt(Tag.NumberOfFailedSuboperations, -1);


                    // TODO analyze
                    // from standard: When the number of Remaining C-STORE sub-operations reaches zero, the SCP generates a final response with a status equal to Success, Warning, Failed, or Refused.

                    if ((status != Status.Pending) && (status != Status.PendingWarning)) {
                        if (numberOfSubOps > 0 || numberOfWarningSubOps > 0) {
                            if (numberOfFailedSubOps >= 0) {
                                scu.setReturnState(CmoveReturnState.PartiallyCompleted);
                            } else {
                                scu.setReturnState(CmoveReturnState.Completed);
                            }
                        }
                    }
                    if (handler != null) {
                        handler.onDimseRSP(as, cmd, data);
                    } else {
                        super.onDimseRSP(as, cmd, data);
                    }
                }
            }, destinationAET);
        } finally {
            try {
                scu.close();
            } catch (Exception e) {
                if (!scu.getAs().isReadyForDataTransfer())
                    log.error("Unable to close association {}, already closed", scu.getAs());
                else
                    log.error("General exception while closing association " + scu.getAs(), e);
            }
        }
        return scu.getReturnState();
    }

    @Override
    public CmoveReturnState moveStudy(ApplicationEntity localAE,
                                      String studyInstanceUID,
                                      int instancesInStudy,
                                      DimseRSPHandler handler,
                                      List<ApplicationEntity> possibleLocations,
                                      String destination)
            throws InterruptedException, GeneralSecurityException, IncompatibleConnectionException, IOException {
        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
        CmoveReturnState state = null;
        for (ApplicationEntity remoteAE : possibleLocations) {
            new CMoveSCU(localAE, remoteAE);
            state = cmove(localAE, remoteAE, keys, handler, destination, false);
            if (state == CmoveReturnState.Completed)
                break;
        }
        return state;
    }


    /**
     * @param studyInstanceUID
     * @param localAE
     * @param remoteAE
     * @param handler            [optional] extra handler logic
     * @param destinationAETitle
     * @return
     */
    @Override
    public CmoveReturnState moveStudy(String studyInstanceUID,
                                      ApplicationEntity localAE,
                                      ApplicationEntity remoteAE,
                                      DimseRSPHandler handler,
                                      String destinationAETitle)
            throws InterruptedException, GeneralSecurityException, IncompatibleConnectionException, IOException {
        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
        return cmove(localAE, remoteAE, keys, handler, destinationAETitle, false);
    }

}
