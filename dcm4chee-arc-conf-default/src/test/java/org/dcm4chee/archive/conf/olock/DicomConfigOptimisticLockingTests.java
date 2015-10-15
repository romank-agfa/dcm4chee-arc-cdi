package org.dcm4chee.archive.conf.olock;

import org.dcm4che3.conf.core.api.ConfigurableClassExtension;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.api.OptimisticLockException;
import org.dcm4che3.conf.core.olock.OptimisticLockingConfiguration;
import org.dcm4che3.conf.core.storage.InMemoryConfiguration;
import org.dcm4che3.conf.core.util.Extensions;
import org.dcm4che3.conf.dicom.CommonDicomConfigurationWithHL7;
import org.dcm4che3.net.Device;
import org.dcm4chee.archive.conf.ArchiveAEExtension;
import org.dcm4chee.archive.conf.ArchiveDeviceTest;
import org.dcm4chee.archive.conf.defaults.DefaultArchiveConfigurationFactory.FactoryParams;
import org.dcm4chee.archive.conf.defaults.DefaultDicomConfigInitializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Some close-to-reality tests for optimistic locking
 *
 * @author Roman K
 */
public class DicomConfigOptimisticLockingTests {


    private CommonDicomConfigurationWithHL7 dicomConfig;


    @Before
    public void before() {

        // prepare storage
        ArrayList<Class> allExtensionClasses = new ArrayList<Class>();
        for (ConfigurableClassExtension extension : ArchiveDeviceTest.getDefaultExtensions())
            allExtensionClasses.add(extension.getClass());

//        Configuration storage = new SingleJsonFileConfigurationStorage("target/config.json");
        Configuration storage = new InMemoryConfiguration();

        storage = new OptimisticLockingConfiguration(storage, allExtensionClasses, storage);

        dicomConfig = new CommonDicomConfigurationWithHL7(
                storage,
                Extensions.getAMapOfExtensionsByBaseExtension(ArchiveDeviceTest.getDefaultExtensions())
        );


        // wipe out clean
        dicomConfig.purgeConfiguration();

        // init default config
        DefaultDicomConfigInitializer init = new DefaultDicomConfigInitializer();

        FactoryParams params = new FactoryParams();
        params.generateUUIDsBasedOnName = true;
        params.useGroupBasedTCConfig = true;

        init.persistDefaultConfig(dicomConfig, dicomConfig, params);

    }


    /**
     * if we have
     */
    @Test
    public void testMultiDepthMerge() {


        Device device1 = dicomConfig.findDevice("dcm4chee-arc");
        Device device2 = dicomConfig.findDevice("dcm4chee-arc");

        // modify device and ae ext
        device1.setLimitOpenAssociations(10);
        device1.getApplicationEntity("DCM4CHEE").getAEExtension(ArchiveAEExtension.class).setModifyingSystem("asdgf");

        dicomConfig.merge(device1);

        // modify ae param
        //device2.getApplicationEntity("DCM4CHEE").getConnections().remove(0);
//        device2.getApplicationEntity("DCM4CHEE").setAeInstalled(false);
        dicomConfig.merge(device2);


        // all 3 changes should make it

        Device loaded = dicomConfig.findDevice("dcm4chee-arc");

        Assert.assertEquals(10, loaded.getLimitOpenAssociations());
        Assert.assertEquals("asdgf", loaded.getApplicationEntity("DCM4CHEE").getAEExtension(ArchiveAEExtension.class).getModifyingSystem());
        Assert.assertEquals(1, device2.getApplicationEntity("DCM4CHEE").getConnections().size());

    }


    @Test
    public void testBiggerChanges() {

        // what if there are bigger changes and how merge works then

        // eg some items added, some deleted from the list
    }


    @Test
    public void testNulls() {

        // what if we pesist null-object somehwere

        // what if we pesist something where null-object was before


    }


    @Test
    public void testRandomScenarios() throws Exception {

        Device device = dicomConfig.findDevice("dcm4chee-arc");
        Device deviceCopy = dicomConfig.findDevice("dcm4chee-arc");

        // change ae ref
        device.setDefaultAE(device.getApplicationEntity("DCM4CHEE_ADMIN"));
        dicomConfig.merge(device);

        // conflicting change
        deviceCopy.setLimitOpenAssociations(25);


        try {
            dicomConfig.merge(deviceCopy);
            Assert.fail("Should have failed");
        } catch (OptimisticLockException e) {
            //it correct
        }


    }
}
