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
package org.dcm4chee.archive.impl;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.dcm4che3.conf.core.api.ConfigChangeEvent;
import org.dcm4che3.conf.api.IApplicationEntityCache;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4che3.net.hl7.service.HL7Service;
import org.dcm4che3.net.hl7.service.HL7ServiceRegistry;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4chee.archive.ArchiveService;
import org.dcm4chee.archive.ArchiveServiceReloaded;
import org.dcm4chee.archive.ArchiveServiceStarted;
import org.dcm4chee.archive.ArchiveServiceStopped;
import org.dcm4chee.archive.dto.Participant;
import org.dcm4chee.archive.event.ConnectionEventSource;
import org.dcm4chee.archive.event.LocalSource;
import org.dcm4chee.archive.event.StartStopReloadEvent;
import org.dcm4chee.conf.decorators.ConfiguredDynamicDecorators;
import org.dcm4chee.conf.decorators.DynamicDecoratorsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
@Singleton
@Startup
public class ArchiveServiceImpl implements ArchiveService {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveServiceImpl.class);
    
    @Inject
    private ArchiveDeviceProducer deviceProducer;

    @Inject
    private IApplicationEntityCache aeCache;

    private static String[] JBOSS_PROPERITIES = {
        "jboss.home",
        "jboss.modules",
        "jboss.server.base",
        "jboss.server.config",
        "jboss.server.data",
        "jboss.server.deploy",
        "jboss.server.log",
        "jboss.server.temp",
    };

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    @Inject
    private Instance<DicomService> dicomServices;

    @Inject
    private Instance<HL7Service> hl7Services;

    @Inject @ArchiveServiceStarted
    private Event<StartStopReloadEvent> archiveServiceStarted;

    @Inject @ArchiveServiceStopped
    private Event<StartStopReloadEvent> archiveServiceStopped;

    @Inject @ArchiveServiceReloaded
    private Event<StartStopReloadEvent> archiveServiceReloaded;

    @Inject
    private ConnectionEventSource connectionEventSource;

    @Inject
    private Device device;

    @Inject
    @ConfiguredDynamicDecorators
    DynamicDecoratorsConfig decoratorsConfig;

    private boolean running;

    private final DicomService echoscp = new BasicCEchoSCP();

    private final DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();

    private final HL7ServiceRegistry hl7ServiceRegistry = new HL7ServiceRegistry();

    private void addJBossDirURLSystemProperties() {
        for (String key : JBOSS_PROPERITIES) {
            String url = new File(System.getProperty(key + ".dir"))
                .toURI().toString();
            System.setProperty(key + ".url", url.substring(0, url.length()-1));
        }
    }

    @PostConstruct
    public void init() {
        addJBossDirURLSystemProperties();
        try {
            executor = Executors.newCachedThreadPool();
            scheduledExecutor = Executors.newScheduledThreadPool(10);
            device.setConnectionMonitor(connectionEventSource);
            device.setExecutor(executor);
            device.setScheduledExecutor(scheduledExecutor);
            serviceRegistry.addDicomService(echoscp);
            for (DicomService service : dicomServices) {
                serviceRegistry.addDicomService(service);
            }
            for (HL7Service service : hl7Services) {
                hl7ServiceRegistry.addHL7Service(service);
            }
            device.setDimseRQHandler(serviceRegistry);
            HL7DeviceExtension hl7Extension =
                    device.getDeviceExtension(HL7DeviceExtension.class);
            if (hl7Extension != null) {
                hl7Extension.setHL7MessageListener(hl7ServiceRegistry);
            }
            start(new LocalSource());
        } catch (RuntimeException re) {
            destroy();
            throw re;
        } catch (Exception e) {
            destroy();
            throw new RuntimeException(e);
        }

        // this just touched and thus init'ed here to improve init log output, TBD where to move it
        decoratorsConfig.getDecoratedServices();

    }

    private void shutdown(ExecutorService executor) {
        if (executor != null)
            executor.shutdown();
    }

    @PreDestroy
    public void destroy() {
        stop(new LocalSource());

        serviceRegistry.removeDicomService(echoscp);
        for (DicomService service : dicomServices) {
            serviceRegistry.removeDicomService(service);
        }
        for (HL7Service service : hl7Services) {
            hl7ServiceRegistry.removeHL7Service(service);
        }
        shutdown(executor);
        shutdown(scheduledExecutor);
    }

    @Override
    public void start(Participant source) throws Exception {
        device.bindConnections();
        running = true;
        archiveServiceStarted.fire(new StartStopReloadEvent(device, source));
    }

    @Override
    public void stop(Participant source) {
        device.unbindConnections();
        if (running) {
            archiveServiceStopped.fire(new StartStopReloadEvent(device, source));
            running = false;
        }
    }

    @Override
    public void onConfigChange(@Observes ConfigChangeEvent configChange) {
        try {
            reload(null);
        } catch (Exception e) {
            LOG.error("Error while reloading configuration", e);
        }
    }
    
    @Override
    public void reload(Participant source) throws Exception {
        aeCache.clear();
        deviceProducer.reloadConfiguration();
        device.rebindConnections();
        archiveServiceReloaded.fire(new StartStopReloadEvent(device, source));
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
