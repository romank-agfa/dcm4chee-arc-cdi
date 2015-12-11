/*
 * *** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2015
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */

package org.dcm4chee.archive.fetch.forward.async;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4chee.archive.api.FetchService;
import org.dcm4chee.archive.conf.ArchiveDeviceExtension;
import org.dcm4chee.archive.retrieve.scu.CMoveSCUService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.Future;

/**
 * @author Roman K
 */
@Stateless
public class  AsyncFetchPrivateEJB {

    Logger log = LoggerFactory.getLogger(AsyncFetchPrivateEJB.class);


    @Inject
    CMoveSCUService cMoveSCUService;

    @Inject
    Device device;



    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<FetchService.FetchResult> moveStudyInAsync(String studyUID) {
        moveStudyIn(studyUID);
        return new AsyncResult<>(new FetchService.FetchResult(studyUID));
    }

    public void moveStudyIn(String studyUID) {

        // calculate devicesToFetch - from ExternalRetrieveLocation
        List<Device> devicesToFetchFrom = new ArrayList<>();
        //TODO

        // sort according to ext dev priorities
        // TODO


        for (Device device : devicesToFetchFrom) {

            Iterator<ApplicationEntity> extAEiterator = listAEsForRetrieveOnExtDevice(device);

            if (!extAEiterator.hasNext()) {
                log.error("Cannot find an AE with compatible transfer capabilities for fetching on external device " + device.getDeviceName());
                continue;
            }

            boolean success = false;
            while (extAEiterator.hasNext()) {
                ApplicationEntity chosenAE = extAEiterator.next();

                // do cmove towards chosen AE
                try {
                    cMoveSCUService.moveStudy(
                            studyUID,
                            device.getDefaultAE(),
                            chosenAE,
                            null,
                            device.getDeviceExtension(ArchiveDeviceExtension.class).getFetchAETitle()
                    );
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while fetching study "+studyUID, e);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException("Unexpected error", e);
                } catch (IncompatibleConnectionException e) {
                    // try another AE
                    log.warn("Cannot find compatible connection for AE "+chosenAE);
                    continue;
                } catch (IOException e) {
                    // try another AE
                    log.warn("Was not able to CMOVE from "+chosenAE);
                    continue;
                }

                // if no errors - we're good, leave
                success = true;
                break;
            }

            // if succeeded and whole study is available, finish
            if (success && isWholeStudyFetched(studyUID)) break;

        }

        if (!isWholeStudyFetched(studyUID)) {
            throw new RuntimeException("After trying "+devicesToFetchFrom.size()+" external locations, could not fetch the study "+studyUID+" fully");
        }
    }

    /**
     * @return true if all instances of a study are available locally,
     * i.e. there are no more instances available on ext devices but not locally
     *
     * @param studyUID
     */
    private boolean isWholeStudyFetched(String studyUID) {
        //TODO
        return false;
    }

    /**
     * Returns an iterator over the AEs on the specified ext device that
     * <ul>
     * <li>support CMOVE TCs</li>
     * <li>are sorted in the order specified by ExternalArchiveAEExtension#fetchAEPriority</li>
     * </ul>
     */
    private Iterator<ApplicationEntity> listAEsForRetrieveOnExtDevice(Device device) throws NoSuchElementException {

        SortedSet<ApplicationEntity> candidateAEsByAETitle = new TreeSet<ApplicationEntity>(new Comparator<ApplicationEntity>() {
            @Override
            public int compare(ApplicationEntity ae1, ApplicationEntity ae2) {
                int priority1 = ae1.getAEExtension(ExternalArchiveAEExtension.class).getAeFetchPriority();
                int priority2 = ae2.getAEExtension(ExternalArchiveAEExtension.class).getAeFetchPriority();
                return priority1 - priority2;
            }
        });

        candidateAEsByAETitle.addAll(device.getAEsSupportingTransferCapability(
                new TransferCapability("", UID.StudyRootQueryRetrieveInformationModelMOVE, TransferCapability.Role.SCP),
                true
        ));
        candidateAEsByAETitle.addAll(device.getAEsSupportingTransferCapability(
                new TransferCapability("", UID.PatientRootQueryRetrieveInformationModelMOVE, TransferCapability.Role.SCP),
                true
        ));

        return candidateAEsByAETitle.iterator();
    }

    private Comparator<ApplicationEntity> fetchAEPriorityComparator() {
        return new Comparator<ApplicationEntity>() {
            @Override
            public int compare(ApplicationEntity ae1, ApplicationEntity ae2) {
                int priority1 = ae1.getAEExtension(ExternalArchiveAEExtension.class).getAeFetchPriority();
                int priority2 = ae2.getAEExtension(ExternalArchiveAEExtension.class).getAeFetchPriority();
                return priority1 - priority2;
            }
        };
    }
}
