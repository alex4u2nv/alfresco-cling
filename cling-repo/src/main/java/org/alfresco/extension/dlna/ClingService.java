package org.alfresco.extension.dlna;

import org.alfresco.jlan.server.config.ServerConfiguration;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.transport.impl.apache.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.apache.StreamClientImpl;
import org.fourthline.cling.transport.spi.StreamClient;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

import java.io.IOException;

/**
 * Created by alexmahabir on 11/3/15.
 */
public class ClingService extends AbstractLifecycleBean {

    private ServerConfiguration filesysConfig;

    @Override
    protected void onBootstrap(ApplicationEvent event) {
        try {

            UpnpServiceConfiguration config = new DefaultUpnpServiceConfiguration(){

                @Override
                public StreamClient createStreamClient() {
                    return new StreamClientImpl(
                            new StreamClientConfigurationImpl(
                                    getSyncProtocolExecutorService()
                            )
                    );
                }
            };

            final UpnpService upnpService = new UpnpServiceImpl(config);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(
                    createDevice()
            );

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

   @Override
    protected void onShutdown(ApplicationEvent event) {

    }
    LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {

        DeviceIdentity identity =
                new DeviceIdentity(
                        UDN.uniqueSystemIdentifier(filesysConfig.getServerName())
                );

        DeviceType type =
                new UDADeviceType(filesysConfig.getServerName(), 1);

        DeviceDetails details =
                new DeviceDetails(
                        "Friendly Binary Light",
                        new ManufacturerDetails("ACME"),
                        new ModelDetails(
                                "BinLight2000",
                                "A demo light with on/off switch.",
                                "v1"
                        )
                );

        Icon icon =
                new Icon(
                        "image/png", 32, 32, 8,
                        getClass().getResource("/res/images/log/AlfrescoLogo32.png")
                );

        LocalService<SwitchPower> switchPowerService =
                new AnnotationLocalServiceBinder().read(SwitchPower.class);

        switchPowerService.setManager(
                new DefaultServiceManager(switchPowerService, SwitchPower.class)
        );

        return new LocalDevice(identity, type, details, icon, switchPowerService);

    /* Several services can be bound to the same device:
    return new LocalDevice(
            identity, type, details, icon,
            new LocalService[] {switchPowerService, myOtherService}
    );
    */

    }

    


    public void stopServer() {

    }


    public void setFilesysConfig(ServerConfiguration filesysConfig) {
        this.filesysConfig = filesysConfig;
    }
}
