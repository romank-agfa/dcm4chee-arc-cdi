package org.dcm4chee.archive.conf.producer;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.dicom.CommonDicomConfigurationWithHL7;
import org.dcm4che3.net.AEExtension;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.net.hl7.HL7ApplicationExtension;
import org.dcm4chee.storage.conf.StorageDeviceExtension;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import java.util.Collection;


@Decorator
public abstract class TemporaryAgilityDicomConfigurationDecorator implements DicomConfiguration {

    private static final String AGILITY_DEVICE_NAME = System.getProperty("org.dcm4chee.archive.deviceName");
    
    @Inject
    @Delegate
    DicomConfiguration delegate;

    
    ////////////////////////////////////////////////////////////////
    ////////////////////// PHASE 1 /////////////////////////////////
    ////////////////////////////////////////////////////////////////

    @Override
    public ApplicationEntity findApplicationEntity(String aet) throws ConfigurationException {
        // if this is an Application Entity of Agility Server - override
        ApplicationEntity applicationEntity = delegate.findApplicationEntity(aet);
        
        // otherwise 
        return applicationEntity;
                
    }

    @Override
    public Device findDevice(String name) throws ConfigurationException {
  
        Device device = delegate.findDevice(name);
        
        
        // override connections and AEs from BPE configuration
                
        device.setConnections(...);
        device.setApplicationEntitiesMap(...);
        
        // override Storage Volumes
        // remove whatever existing ones
        if (device.getDeviceExtension(StorageDeviceExtension.class) != null) {
            device.removeDeviceExtension(device.getDeviceExtension(StorageDeviceExtension.class));
        };
        // add from proprietary storage
        StorageDeviceExtension storageDeviceExtension = new StorageDeviceExtension();
        storageDeviceExtension.addStorageSystemGroup(...);

        return device;
    }

    ////////////////////////////////////////////////////////////////
    ////////////////////// PHASE 2 /////////////////////////////////
    ////////////////////////////////////////////////////////////////


}
