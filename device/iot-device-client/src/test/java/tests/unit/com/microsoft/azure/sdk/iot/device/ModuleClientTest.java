/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package tests.unit.com.microsoft.azure.sdk.iot.device;

import com.microsoft.azure.sdk.iot.device.*;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import org.junit.Test;

import java.net.URISyntaxException;

/**
 * Unit tests for ModuleClient.java
 * Methods:
 * Lines:
 */
public class ModuleClientTest
{
    @Mocked
    DeviceClientConfig mockedDeviceClientConfig;

    @Mocked
    IotHubConnectionString mockedIotHubConnectionString;

    @Mocked
    Message mockedMessage;

    @Mocked
    IotHubEventCallback mockedIotHubEventCallback;

    @Mocked
    DeviceIO mockedDeviceIO;

    //@Mocked
    //InternalClient mockedInternalClient;

    private void baseExpectations() throws URISyntaxException
    {
        new NonStrictExpectations()
        {
            {
                new IotHubConnectionString(anyString);
                result = mockedIotHubConnectionString;

                mockedIotHubConnectionString.getModuleId();
                result = "someModuleId";

                mockedDeviceClientConfig.getModuleId();
                result = "someModuleId";
            }
        };
    }


    //Codes_SRS_MODULECLIENT_34_004: [If the provided connection string does not contain a module id, this function shall throw an UnsupportedOperationException.]
    //Codes_SRS_MODULECLIENT_34_005: [If the provided connection string uses x509, this function shall throw an UnsupportedOperationException.]
    //Codes_SRS_MODULECLIENT_34_006: [This function shall invoke the super constructor.]


    //Codes_SRS_MODULECLIENT_34_001: [If the provided outputName is null or empty, this function shall throw an IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void sendEventAsyncWithOutputThrowsForEmptyOutputName() throws URISyntaxException
    {
        //arrange
        baseExpectations();
        ModuleClient client = new ModuleClient("some connection string", IotHubClientProtocol.MQTT);

        //act
        client.sendEventAsync("", mockedMessage, mockedIotHubEventCallback, new Object());
    }

    //Codes_SRS_MODULECLIENT_34_001: [If the provided outputName is null or empty, this function shall throw an IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void sendEventAsyncWithOutputThrowsForNullOutputName() throws URISyntaxException
    {
        //arrange
        baseExpectations();
        ModuleClient client = new ModuleClient("some connection string", IotHubClientProtocol.MQTT);

        //act
        client.sendEventAsync(null, mockedMessage, mockedIotHubEventCallback, new Object());
    }


    //Codes_SRS_MODULECLIENT_34_002: [This function shall set the provided message with the provided outputName, device id, and module id properties.]
    //Codes_SRS_MODULECLIENT_34_003: [This function shall invoke super.sendEventAsync(message, callback, callbackContext).]
    @Test
    public void sendEventAsyncSuccess() throws URISyntaxException
    {
        //arrange
        baseExpectations();
        ModuleClient client = new ModuleClient("some connection string", IotHubClientProtocol.MQTT);
        final String expectedOutputName = "some output name";
        final String expectedDeviceId = "1234";
        final String expectedModuleId = "5678";
        Deencapsulation.setField(client, "config", mockedDeviceClientConfig);
        new NonStrictExpectations()
        {
            {
                mockedDeviceClientConfig.getDeviceId();
                result = expectedDeviceId;

                mockedDeviceClientConfig.getModuleId();
                result = expectedModuleId;
            }
        };

        //act
        client.sendEventAsync(expectedOutputName, mockedMessage, mockedIotHubEventCallback, new Object());

        //assert
        new Verifications()
        {
            {
                mockedMessage.setOutputName(expectedOutputName);
                times = 1;

                mockedMessage.setConnectionDeviceId(expectedDeviceId);
                times = 1;

                mockedMessage.setConnectionModuleId(expectedModuleId);
                times = 1;

                mockedDeviceIO.sendEventAsync(mockedMessage, mockedIotHubEventCallback, any, mockedIotHubConnectionString);
                times = 1;
            }
        };
    }
}
