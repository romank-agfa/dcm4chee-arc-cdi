//
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
 * Portions created by the Initial Developer are Copyright (C) 2011
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

package org.dcm4chee.archive.sc.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dcm4chee.archive.sc.StructuralChangeContext;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class BasicStructuralChangeContext implements StructuralChangeContext {
    private final Set<String> affectedStudyUIDs = new HashSet<>();
    private final Set<String> affectedSeriesUIDs = new HashSet<>();
    private final Set<Instance> affectedInstances = new HashSet<>();
    
    protected final Enum<?>[] changeTypes;
    private final long timestamp;
    
    public BasicStructuralChangeContext(Enum<?>... changeType) {
        this(System.currentTimeMillis(), changeType);
    }
    
    public BasicStructuralChangeContext(long timestamp, Enum<?>... changeTypes) {
        this.changeTypes = changeTypes;
        this.timestamp = timestamp;
    }
    
    @Override
    public boolean hasChangeType(Enum<?> changeType) {
        return indexOfChangeType(changeType) != -1;
    }
    
    @Override
    public Enum<?>[] getSubChangeTypeHierarchy(Enum<?> changeType) {
        int idx = indexOfChangeType(changeType);
        if(idx != -1) {
            int subChangeTypesLength = changeTypes.length - idx;
            Enum<?>[] subChangeTypes = new Enum[subChangeTypesLength];
            System.arraycopy(changeTypes, idx, subChangeTypes, 0, subChangeTypesLength);
            return subChangeTypes;
        } else {
            return null;
        }
    }
    
    private int indexOfChangeType(Enum<?> changeType) {
        for (int i = 0; i < changeTypes.length; i++) {
            if(changeTypes[i].equals(changeType)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Enum<?>[] getChangeTypeHierarchy() {
        return changeTypes;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public Set<String> getAffectedStudyUIDs() {
        return affectedStudyUIDs;
    }
    
    @Override
    public Set<String> getAffectedSeriesUIDs() {
        return affectedSeriesUIDs;
    }
    
    @Override
    public Set<Instance> getAffectedInstances() {
        return affectedInstances;
    }
    
    public void addAffectedInstance(Instance affectedInstance) {
        this.affectedStudyUIDs.add(affectedInstance.getStudyInstanceUID());
        this.affectedSeriesUIDs.add(affectedInstance.getSeriesInstanceUID());
        this.affectedInstances.add(affectedInstance);
    }
    
    public void addAffectedInstances(Collection<Instance> affectedInstances) {
        for(Instance affectedInstance : affectedInstances) {
            addAffectedInstance(affectedInstance);
        }
    }
    
    public static class InstanceImpl implements Instance {
        private final String studyInstanceUID;
        private final String seriesInstanceUID;
        private final String sopInstanceUID;
        
        public InstanceImpl(String studyInstanceUID, String seriesInstanceUID, String sopInstanceUID) {
            this.studyInstanceUID = studyInstanceUID;
            this.seriesInstanceUID = seriesInstanceUID;
            this.sopInstanceUID = sopInstanceUID;
        }

        @Override
        public String getStudyInstanceUID() {
            return studyInstanceUID;
        }

        @Override
        public String getSeriesInstanceUID() {
            return seriesInstanceUID;
        }

        @Override
        public String getSopInstanceUID() {
            return sopInstanceUID;
        }
        
    }
    
}