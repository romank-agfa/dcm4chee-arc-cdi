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

import org.dcm4che3.net.Device;
import org.dcm4chee.archive.api.FetchService;
import org.dcm4chee.archive.fetch.forward.FetchSyncEJB;
import org.dcm4chee.cache.Cache;
import org.dcm4chee.cache.CacheByName;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.*;

/**
 * Rationale:
 * Fetch is preferably done on series level, due to
 * - not all support instance level cmove
 * - on study level it could be too much at once and is more error prone
 * <p/>
 * Then it falls back to study level.
 * <p/>
 * Once an instance or series is requested, the whole study is fetched implicitly in the background.
 */
@EJB(name = FetchService.JNDI_NAME, beanInterface = FetchService.class)
@Stateless
public class AsyncFetchServiceBean implements FetchService {

    @Inject
    Device device;

    @Inject
    @CacheByName("fetch-forward")
    private Cache<String, String> ffCache;

    @Inject
    FetchSyncEJB fetchPrivateEJB;

    @Override
    public FetchProgress fetchStudyAsync(final String studyUID) {


        final FutureTask<FetchResult> fetchResultFuture = new FutureTask<FetchResult>(new FetchResultCallable(studyUID));


        device.getExecutor().execute(fetchResultFuture);


        return new FetchProgress() {
            @Override
            public Future<FetchResult> getFuture() {
                return fetchResultFuture;
            }
        };
    }

    @Override
    public FetchProgress fetchSeriesAsync(String seriesUID) {

        // handle not existent
        // handle already available
        throw new RuntimeException("not implemented");
    }

    @Override
    public Future<Path> fetchInstanceAsync(String sopIUID) {

        // handle not existent
        // handle already available

        throw new RuntimeException("not implemented");
    }




    private static class FetchFuture extends FutureTask<FetchResult> {


        public FetchFuture(Callable<FetchResult> callable) {
            super(callable);
        }
    }

    private class FetchResultCallable implements Callable<FetchResult> {
        private final String studyUID;

        public FetchResultCallable(String studyUID) {
            this.studyUID = studyUID;
        }

        @Override
        public FetchResult call() throws Exception {

            // handle not existent


            while (true) {

                // if already available - return
                if (fetchPrivateEJB.isWholeStudyFetched(studyUID)) {
                    return new FetchResult(studyUID);
                }

                // try to insert a value into the sync'ed map

                // if success
                fetchPrivateEJB.fetchStudy(studyUID);

                break;

            }


            return null;
        }
    }
}
