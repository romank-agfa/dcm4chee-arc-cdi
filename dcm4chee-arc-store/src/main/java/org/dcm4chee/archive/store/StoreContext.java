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

package org.dcm4chee.archive.store;

import org.dcm4che3.data.Attributes;
import org.dcm4chee.archive.conf.StoreAction;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Location;
import org.dcm4chee.storage.StorageContext;

import java.io.InputStream;
import java.util.TimeZone;
import java.util.concurrent.Future;

/**
 * StoreContext represents the internal state of the current StoreService.store( )
 * operation. There is one StoreContext instance per received DICOM instance.
 *
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
public interface StoreContext {


    Attributes getAttributes();

    void setAttributes(Attributes attributes);

    Attributes getCoercedOriginalAttributes();

    void setCoercedOrginalAttributes(Attributes attributes);

    Attributes getFileMetainfo();

    void setFileMetainfo(Attributes fileMetainfo);

    StoreSession getStoreSession();

    String getSpoolFileSuffix();

    void setSpoolFileSuffix(String spoolFileSuffix);

    String getNoDBAttsDigest();

    void setNoDBAttsDigest(String noDBAttsDigest);    

    String getTransferSyntax();

    void setTransferSyntax(String transferSyntax);

    StoreAction getStoreAction();

    void setStoreAction(StoreAction action);

    Location getFileRef();

    void setFileRef(Location createFileRef);

    Instance getInstance();

    void setInstance(Instance instance);

    Object getProperty(String key);

    Object removeProperty(String key);

    void setProperty(String key, Object value);
    
    boolean isFail();
    
    Throwable getThrowable();
    
    void setThrowable(Throwable t);

    TimeZone getSourceTimeZone();

    void setSourceTimeZone(TimeZone sourceTimeZone);

    String getSourceTimeZoneID();

    boolean isFetch();

    void setFetch(boolean fetch);

    InputStream getInputStream();

    void setInputStream(InputStream inputStream);

    StorageContext getSpoolingContext();

    void setSpoolingContext(StorageContext spoolingContext);

    Future<StorageContext> getBulkdataContext();

    void setBulkdataContext(Future<StorageContext> bulkdataContext);

    Future<StorageContext> getMetadataContext();

    void setMetadataContext(Future<StorageContext> metadataContext);

    void setOldNONEIOCMChangeUID(String oldNONEIOCMChangeUID);

    String getOldNONEIOCMChangeUID();
    
}
