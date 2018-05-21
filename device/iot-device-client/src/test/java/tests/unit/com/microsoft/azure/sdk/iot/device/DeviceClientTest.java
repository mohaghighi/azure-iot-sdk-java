// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package tests.unit.com.microsoft.azure.sdk.iot.device;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.auth.IotHubSasTokenAuthenticationProvider;
import com.microsoft.azure.sdk.iot.device.auth.IotHubX509AuthenticationProvider;
import com.microsoft.azure.sdk.iot.device.fileupload.FileUpload;
import com.microsoft.azure.sdk.iot.device.transport.amqps.IoTHubConnectionType;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;
import com.microsoft.azure.sdk.iot.provisioning.security.exceptions.SecurityProviderException;
import mockit.*;
import org.junit.Test;

import java.io.IOError;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for DeviceClient.
 * Methods: 100%
 * Lines: 98%
 */
public class DeviceClientTest
{
    //@Mocked
    //InternalClient mockedInternalClient;

    @Mocked
    DeviceClientConfig mockConfig;

    @Mocked
    IotHubConnectionString mockIotHubConnectionString;

    @Mocked
    DeviceIO mockDeviceIO;

    @Mocked
    TransportClient mockTransportClient;

    @Mocked
    FileUpload mockFileUpload;

    @Mocked
    IotHubSasTokenAuthenticationProvider mockIotHubSasTokenAuthenticationProvider;

    @Mocked
    IotHubX509AuthenticationProvider mockIotHubX509AuthenticationProvider;

    @Mocked
    SecurityProvider mockSecurityProvider;

    @Mocked
    IotHubConnectionStatusChangeCallback mockedIotHubConnectionStatusChangeCallback;

    @Mocked
    ProductInfo mockedProductInfo;

    private static long SEND_PERIOD_MILLIS = 10L;
    private static long RECEIVE_PERIOD_MILLIS_AMQPS = 10L;
    private static long RECEIVE_PERIOD_MILLIS_HTTPS = 25*60*1000; /*25 minutes*/

    // Tests_SRS_DEVICECLIENT_12_009: [The constructor shall interpret the connection string as a set of key-value pairs delimited by ';', using the object IotHubConnectionString.]
    // Tests_SRS_DEVICECLIENT_12_010: [The constructor shall set the connection type to USE_TRANSPORTCLIENT.]
    // Tests_SRS_DEVICECLIENT_12_011: [The constructor shall set the deviceIO to null.]
    // Tests_SRS_DEVICECLIENT_12_016: [The constructor shall save the transportClient parameter.]
    // Tests_SRS_DEVICECLIENT_12_017: [The constructor shall register the device client with the transport client.]
    @Test
    public void constructorTransportClientSuccess() throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";

        // act
        final DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // assert
        new Verifications()
        {
            {
                IotHubConnectionString iotHubConnectionString = Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                times = 1;
                Deencapsulation.newInstance(DeviceClientConfig.class, iotHubConnectionString, DeviceClientConfig.AuthType.SAS_TOKEN);
                times = 1;

                IoTHubConnectionType ioTHubConnectionType = Deencapsulation.getField(client, "ioTHubConnectionType");
                assertEquals(IoTHubConnectionType.USE_TRANSPORTCLIENT, ioTHubConnectionType);

                DeviceIO deviceIO = Deencapsulation.getField(client, "deviceIO");
                assertNull(deviceIO);

                TransportClient actualTransportClient = Deencapsulation.getField(client, "transportClient");
                assertEquals(mockTransportClient, actualTransportClient);

                Deencapsulation.invoke(mockTransportClient, "registerDeviceClient", client);
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_018: [If the tranportClient is null, the function shall throw an IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void constructorTransportClientTransportClientNullThrows() throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";

        // act
        new DeviceClient(connString, (TransportClient) null);
    }

    // Tests_SRS_DEVICECLIENT_21_001: [The constructor shall interpret the connection string as a set of key-value pairs delimited by ';', using the object IotHubConnectionString.]
    // Tests_SRS_DEVICECLIENT_12_012: [The constructor shall set the connection type to SINGLE_CLIENT.]
    // Tests_SRS_DEVICECLIENT_12_015: [The constructor shall set the transportClient to null.]
    @Test
    public void constructorSuccess() throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        // act
        final DeviceClient client = new DeviceClient(connString, protocol);

        // assert
        new Verifications()
        {
            {
                IotHubConnectionString iotHubConnectionString = Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                times = 1;

                IoTHubConnectionType ioTHubConnectionType = Deencapsulation.getField(client, "ioTHubConnectionType");
                assertEquals(IoTHubConnectionType.SINGLE_CLIENT, ioTHubConnectionType);

                TransportClient transportClient = Deencapsulation.getField(client, "transportClient");
                assertNull(transportClient);
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_34_058: [The constructor shall interpret the connection string as a set of key-value pairs delimited by ';', using the object IotHubConnectionString.]
    // Tests_SRS_DEVICECLIENT_12_013: [The constructor shall set the connection type to SINGLE_CLIENT.]
    // Tests_SRS_DEVICECLIENT_12_014: [The constructor shall set the transportClient to null.]
    @Test
    public void constructorSuccessX509() throws URISyntaxException
    {
        // arrange
        final String publicKeyCert = "someCert";
        final String privateKey = "someKey";

        final String connString =
                "HostName=iothub.device.com;DeviceId=testdevice;x509=true";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS_WS;

        new NonStrictExpectations()
        {
            {
                mockIotHubConnectionString.isUsingX509();
                result = true;
            }
        };

        // act
        final DeviceClient client = new DeviceClient(connString, protocol, publicKeyCert, false, privateKey, false);

        // assert
        new Verifications()
        {
            {
                Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                times = 1;

                IoTHubConnectionType ioTHubConnectionType = Deencapsulation.getField(client, "ioTHubConnectionType");
                assertEquals(IoTHubConnectionType.SINGLE_CLIENT, ioTHubConnectionType);

                TransportClient transportClient = Deencapsulation.getField(client, "transportClient");
                assertNull(transportClient);
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_34_065: [The provided uri and device id will be used to create an iotHubConnectionString that will be saved in config.]
    //Tests_SRS_DEVICECLIENT_34_066: [The provided security provider will be saved in config.]
    //Tests_SRS_DEVICECLIENT_34_067: [The constructor shall initialize the IoT Hub transport for the protocol specified, creating a instance of the deviceIO.]
    @Test
    public void createFromSecurityProviderUsesUriAndDeviceIdAndSavesSecurityProviderAndCreatesDeviceIO() throws URISyntaxException, IOException
    {
        //arrange
        final String expectedUri = "some uri";
        final String expectedDeviceId = "some device id";
        final IotHubClientProtocol expectedProtocol = IotHubClientProtocol.HTTPS;

        //act
        DeviceClient.createFromSecurityProvider(expectedUri, expectedDeviceId, mockSecurityProvider, expectedProtocol);

        //assert
        new Verifications()
        {
            {
                //TODO add check for super() call
            }
        };
    }

    /* Tests_SRS_DEVICECLIENT_21_001: [The constructor shall interpret the connection string as a set of key-value pairs delimited by ';', using the object IotHubConnectionString.] */
    @Test
    public void constructorBadConnectionStringThrows() throws URISyntaxException, IOException
    {
        // arrange
        final String connString =
                "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
        new NonStrictExpectations()
        {
            {
                Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                result = new IllegalArgumentException();
            }
        };

        // act
        try
        {
            new DeviceClient(connString, protocol);
        }
        catch (IllegalArgumentException expected)
        {
            // Don't do anything, throw expected.
        }

        // assert
        new Verifications()
        {
            {
                IotHubConnectionString iotHubConnectionString = Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                times = 1;
                Deencapsulation.newInstance(DeviceClientConfig.class, iotHubConnectionString, DeviceClientConfig.AuthType.SAS_TOKEN);
                times = 0;
                Deencapsulation.newInstance("com.microsoft.azure.sdk.iot.device.DeviceIO",
                        new Class[] {DeviceClientConfig.class, long.class, long.class},
                        any, SEND_PERIOD_MILLIS, RECEIVE_PERIOD_MILLIS_AMQPS);
                times = 0;
            }
        };
    }

    /* Tests_SRS_DEVICECLIENT_21_002: [The constructor shall initialize the IoT Hub transport for the protocol specified, creating a instance of the deviceIO.] */
    @Test (expected = IllegalArgumentException.class)
    public void constructorBadDeviceIOThrows() throws URISyntaxException, IOException
    {
        // arrange
        final String connString =
                "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        // act
        new DeviceClient(connString, protocol);

        // assert
        new Verifications()
        {
            {
                Deencapsulation.newInstance(IotHubConnectionString.class, connString);
                times = 1;
                Deencapsulation.newInstance(DeviceClientConfig.class, (IotHubConnectionString)any, DeviceClientConfig.AuthType.SAS_TOKEN);
                times = 1;
                Deencapsulation.newInstance("com.microsoft.azure.sdk.iot.device.DeviceIO",
                        new Class[] {DeviceClientConfig.class, IotHubClientProtocol.class, long.class, long.class},
                        (DeviceClientConfig)any, protocol, SEND_PERIOD_MILLIS, RECEIVE_PERIOD_MILLIS_AMQPS);
                times = 1;
            }
        };
    }

    /* Tests_SRS_DEVICECLIENT_21_003: [The constructor shall save the connection configuration using the object DeviceClientConfig.] */
    @Test (expected = IllegalArgumentException.class)
    public void constructorBadDeviceClientConfigThrows() throws URISyntaxException, IOException
    {
        // arrange
        final String connString =
                "HostName=iothub.device.com;CredentialType=SharedAccessKey;CredentialScope=Device;DeviceId=testdevice;SharedAccessKey=adjkl234j52=;";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        // act
        new DeviceClient(connString, protocol);

        // assert
        new Verifications()
        {
            {
                Deencapsulation.newInstance(DeviceClientConfig.class, (IotHubConnectionString)any, DeviceClientConfig.AuthType.SAS_TOKEN);
                times = 1;
                Deencapsulation.newInstance("com.microsoft.azure.sdk.iot.device.DeviceIO",
                        new Class[] {DeviceClientConfig.class, IotHubClientProtocol.class, long.class, long.class},
                        (DeviceClientConfig)any, protocol, SEND_PERIOD_MILLIS, RECEIVE_PERIOD_MILLIS_AMQPS);
                times = 0;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_21_006: [The open shall invoke super.open().]
    @Test
    public void openOpensDeviceIOSuccess(final @Mocked InternalClient mockedInternalClient) throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.open();

        // assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(mockedInternalClient, "open");
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_007: [If the client has been initialized to use TransportClient and the TransportClient is not opened yet the function shall throw an IOException.]
    @Test (expected = IOException.class)
    public void openUseTransportClientAndCalledBeforeTransportClientOpenedThrows() throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.CLOSED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // act
        client.open();
    }

    // Tests_SRS_DEVICECLIENT_12_019: [If the client has been initialized to use TransportClient and the TransportClient is already opened the function shall do nothing.]
    @Test
    public void openUseTransportClientAndCalledAfterTransportClientOpenedDoNothing(final @Mocked InternalClient mockedInternalClient) throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);

        // act
        client.open();

        new Verifications()
        {
            {
                Deencapsulation.invoke(mockDeviceIO, "open");
                times = 0;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_34_040: [If this object is not using a transport client, it shall invoke super.close().]
    @Test
    public void closeClosesTransportSuccess(final @Mocked InternalClient mockedInternalClient) throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.close();

        // assert
        new Verifications()
        {
            {
                mockedInternalClient.close();
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_006: [If the client has been initialized to use TransportClient and the TransportClient is already opened the function shall throw an IOException.]
    @Test (expected = IOException.class)
    public void closeUseTransportClientAndCalledAfterTransportClientOpenedThrows() throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // act
        client.close();
    }

    // Tests_SRS_DEVICECLIENT_12_020: [If the client has been initialized to use TransportClient and the TransportClient is not opened yet the function shall do nothing.]
    @Test
    public void closeUseTransportClientAndCalledBeforeTransportClientOpenedDoNothing() throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.CLOSED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);

        // act
        client.close();

        new Verifications()
        {
            {
                mockDeviceIO.isEmpty();
                times = 0;
                mockDeviceIO.close();
                times = 0;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_005: [If the client has been initialized to use TransportClient and the TransportClient is already opened the function shall throw an IOException.]
    @Test (expected = IOException.class)
    public void closeNowUseTransportClientAndCalledAfterTransportClientOpenedThrows() throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // act
        client.closeNow();
    }

    // Tests_SRS_DEVICECLIENT_12_021: [If the client has been initialized to use TransportClient and the TransportClient is not opened yet the function shall do nothing.]
    @Test
    public void closeNowUseTransportClientAndCalledBeforeTransportClientOpenedDoNothing() throws URISyntaxException, IOException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.CLOSED;
            }
        };

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        Deencapsulation.setField(client, "fileUpload", mockFileUpload);

        // act
        client.closeNow();

        new Verifications()
        {
            {
                mockFileUpload.closeNow();
                times = 0;
                mockDeviceIO.close();
                times = 0;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_34_041: [If this object is not using a transport client, it shall invoke super.closeNow().]
    @Test
    public void closeNowClosesTransportSuccess(final @Mocked InternalClient mockedInternalClient) throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.closeNow();

        // assert
        new Verifications()
        {
            {
                mockedInternalClient.closeNow();
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_02_015: [If optionName is null or not an option handled by the client, then it shall throw IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void setOptionWithNullOptionNameThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        long someMilliseconds = 4;

        // act
        client.setOption(null, someMilliseconds);
    }

    // Tests_SRS_DEVICECLIENT_02_015: [If optionName is null or not an option handled by the client, then it shall throw IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void setOptionWithUnknownOptionNameThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        long someMilliseconds = 4;

        // act
        client.setOption("thisIsNotAHandledOption", someMilliseconds);
    }

    //Tests_SRS_DEVICECLIENT_02_017: [Available only for HTTP.]
    @Test (expected = IllegalArgumentException.class)
    public void setOptionMinimumPollingIntervalWithAMQPfails()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
        long someMilliseconds = 4;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.AMQPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.setOption("SetMinimumPollingInterval", someMilliseconds);
    }

    //Tests_SRS_DEVICECLIENT_02_018: [Value needs to have type long].
    @Test (expected = IllegalArgumentException.class)
    public void setOptionMinimumPollingIntervalWithStringInsteadOfLongFails()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.setOption("SetMinimumPollingInterval", "thisIsNotALong");
    }

    //Tests_SRS_DEVICECLIENT_02_005: [Setting the option can only be done before open call.]
    @Test (expected = IllegalStateException.class)
    public void setOptionMinimumPollingIntervalAfterOpenFails()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);
        client.open();
        long value = 3;

        // act
        client.setOption("SetMinimumPollingInterval", value);
    }

    //Tests_SRS_DEVICECLIENT_02_016: ["SetMinimumPollingInterval" - time in milliseconds between 2 consecutive polls.]
    @Test
    public void setOptionMinimumPollingIntervalSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);
        final long value = 3L;

        // act
        client.setOption("SetMinimumPollingInterval", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.setReceivePeriodInMilliseconds(value);
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_21_040: ["SetSendInterval" - time in milliseconds between 2 consecutive message sends.]
    @Test
    public void setOptionSendIntervalSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);
        final long value = 3L;

        // act
        client.setOption("SetSendInterval", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.setSendPeriodInMilliseconds(value);
            }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void setOptionSendIntervalWithStringInsteadOfLongFails()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.setOption("SetSendInterval", "thisIsNotALong");
    }

    @Test (expected = IllegalArgumentException.class)
    public void setOptionValueNullThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.setOption("", null);
    }

    //Tests_SRS_DEVICECLIENT_25_022: [**"SetSASTokenExpiryTime" should have value type long.]
    @Test (expected = IllegalArgumentException.class)
    public void setOptionSASTokenExpiryTimeWithStringInsteadOfLongFails()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);

        // act
        client.setOption("SetSASTokenExpiryTime", "thisIsNotALong");
    }

    //Tests_SRS_DEVICECLIENT_25_021: ["SetSASTokenExpiryTime" - time in seconds after which SAS Token expires.]
    @Test
    public void setOptionSASTokenExpiryTimeHTTPSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_25_021: ["SetSASTokenExpiryTime" - time in seconds after which SAS Token expires.]
    //Tests_SRS_DEVICECLIENT_25_024: ["SetSASTokenExpiryTime" shall restart the transport if transport is already open after updating expiry time.]
    @Test
    public void setOptionSASTokenExpiryTimeAfterClientOpenHTTPSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        client.open();
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.close();
                times = 1;
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
                Deencapsulation.invoke(mockDeviceIO, "open");
                times = 2;
            }
        };
    }

    /*Tests_SRS_DEVICECLIENT_25_024: ["SetSASTokenExpiryTime" shall restart the transport
                                    1. If the device currently uses device key and
                                    2. If transport is already open
                                    after updating expiry time.]
    */
    @Test
    public void setOptionSASTokenExpiryTimeAfterClientOpenTransportWithSasTokenSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockConfig.getIotHubConnectionString();
                result = mockIotHubConnectionString;
                mockIotHubConnectionString.getSharedAccessKey();
                result = "some key";
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessSignature=SharedAccessSignature sr=sample-iothub-hostname.net%2fdevices%2fsample-device-ID&sig=S3%2flPidfBF48B7%2fOFAxMOYH8rpOneq68nu61D%2fBP6fo%3d&se=1469813873";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        client.open();
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.close();
                times = 1;
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
                Deencapsulation.invoke(mockDeviceIO, "open");
                times = 2;
            }
        };
    }
    //Tests_SRS_DEVICECLIENT_25_021: ["SetSASTokenExpiryTime" - Time in secs to specify SAS Token Expiry time.]
    @Test
    public void setOptionSASTokenExpiryTimeAMQPSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };

        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_25_021: ["SetSASTokenExpiryTime" - time in seconds after which SAS Token expires.]
    //Tests_SRS_DEVICECLIENT_25_024: ["SetSASTokenExpiryTime" shall restart the transport if transport is already open after updating expiry time.]
    @Test
    public void setOptionSASTokenExpiryTimeAfterClientOpenAMQPSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        client.open();
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.close();
                times = 1;
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
                Deencapsulation.invoke(mockDeviceIO, "open");
                times = 2;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_25_021: [**"SetSASTokenExpiryTime" - Time in secs to specify SAS Token Expiry time.]
    @Test
    public void setOptionSASTokenExpiryTimeMQTTSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations() {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
            }
        };
    }

    //Tests_SRS_DEVICECLIENT_25_021: ["SetSASTokenExpiryTime" - time in seconds after which SAS Token expires.]
    //Tests_SRS_DEVICECLIENT_25_024: ["SetSASTokenExpiryTime" shall restart the transport if transport is already open after updating expiry time.]
    @Test
    public void setOptionSASTokenExpiryTimeAfterClientOpenMQTTSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);
        client.open();
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockDeviceIO.close();
                times = 1;
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
                Deencapsulation.invoke(mockDeviceIO, "open");
                times = 2;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_025: [If the client configured to use TransportClient the function shall use transport client closeNow() and open() for restart.]
    @Test
    public void setOptionWithTransportClientSASTokenExpiryTimeAfterClientOpenAMQPSucceeds()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);

        // assert
        new Verifications()
        {
            {
                mockTransportClient.closeNow();
                times = 1;
                mockConfig.getSasTokenAuthentication().setTokenValidSecs(value);
                times = 1;
                mockTransportClient.open();
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_027: [The function shall throw IOError if either the deviceIO or the tranportClient's open() or closeNow() throws.]
    @Test (expected = IOError.class)
    public void setOptionWithTransportClientSASTokenExpiryTimeAfterClientOpenAMQPThrowsTransportClientClose()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockTransportClient.closeNow();
                result =  new IOException();
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);
    }

    // Tests_SRS_DEVICECLIENT_12_027: [The function shall throw IOError if either the deviceIO or the tranportClient's open() or closeNow() throws.]
    @Test (expected = IOError.class)
    public void setOptionWithTransportClientSASTokenExpiryTimeAfterClientOpenAMQPThrowsTransportClientOpen()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockTransportClient.open();
                result =  new IOException();
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);
    }

    // Tests_SRS_DEVICECLIENT_12_029: [*SetCertificatePath" shall throw if the transportClient or deviceIO already opene.]
    @Test (expected = IllegalStateException.class)
    public void setOptionWithTransportClientSetCertificatePathTransportOpenedThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
//        Deencapsulation.setField(client, "config", mockConfig);
//        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final String value = "certificatePath";

        // act
        client.setOption("SetCertificatePath", value);
    }

    // Tests_SRS_DEVICECLIENT_12_030: [*SetCertificatePath" shall udate the config on transportClient if tranportClient used.]
    @Test
    public void setOptionWithTransportClientSetCertificatePathSuccess()
            throws IOException, URISyntaxException
    {
        // arrange
        final String value = "certificatePath";

        new NonStrictExpectations()
        {
            {
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.CLOSED;

                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockIotHubSasTokenAuthenticationProvider.setPathToIotHubTrustedCert(value);
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";

        DeviceClient client = new DeviceClient(connString, mockTransportClient);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);

        // act
        client.setOption("SetCertificatePath", value);

        new Verifications()
        {
            {
                mockConfig.getAuthenticationType();
                times = 1;
                mockConfig.getSasTokenAuthentication();
                times = 1;
                mockIotHubSasTokenAuthenticationProvider.setPathToIotHubTrustedCert(value);
                times = 1;
            }
        };
    }
    // Tests_SRS_DEVICECLIENT_12_029: [*SetCertificatePath" shall throw if the transportClient or deviceIO already open, otherwise set the path on the config.]
    @Test (expected = IllegalStateException.class)
    public void setOptionSetCertificatePathDeviceIOOpenedThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final String value = "certificatePath";

        // act
        client.setOption("SetCertificatePath", value);
    }

    // Tests_SRS_DEVICECLIENT_12_030: [*SetCertificatePath" shall udate the config on transportClient if tranportClient used.]
    @Test (expected = IllegalArgumentException.class)
    public void setOptionSetCertificatePathWrongProtocolThrows()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                Deencapsulation.invoke(mockTransportClient, "getTransportClientState");
                result = TransportClient.TransportClientState.OPENED;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final String value = "certificatePath";

        // act
        client.setOption("SetCertificatePath", value);
    }

    // Tests_SRS_DEVICECLIENT_12_029: [*SetCertificatePath" shall throw if the transportClient or deviceIO already open, otherwise set the path on the config.]
    @Test
    public void setOptionSetCertificatePathSASSuccess()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.AMQPS_WS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS_WS;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final String value = "certificatePath";

        // act
        client.setOption("SetCertificatePath", value);

        new Verifications()
        {
            {
                mockConfig.getSasTokenAuthentication();
                times = 1;
                mockIotHubSasTokenAuthenticationProvider.setPathToIotHubTrustedCert(value);
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_029: [*SetCertificatePath" shall throw if the transportClient or deviceIO already open, otherwise set the path on the config.]
    @Test
    public void setOptionSetCertificatePathX509Success()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = false;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.AMQPS_WS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.X509_CERTIFICATE;
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS_WS;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final String value = "certificatePath";

        // act
        client.setOption("SetCertificatePath", value);

        new Verifications()
        {
            {
                mockConfig.getAuthenticationType();
                times = 2;
                mockConfig.getX509Authentication();
                times = 1;
                mockIotHubX509AuthenticationProvider.setPathToIotHubTrustedCert(value);
                times = 1;
            }
        };
    }

    // Tests_SRS_DEVICECLIENT_12_027: [The function shall throw IOError if either the deviceIO or the tranportClient's open() or closeNow() throws.]
    @Test (expected = IOError.class)
    public void setOptionClientSASTokenExpiryTimeAfterClientOpenAMQPThrowsDeviceIOClose()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                mockDeviceIO.close();
                result =  new IOException();
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);
    }

    // Tests_SRS_DEVICECLIENT_12_027: [The function shall throw IOError if either the deviceIO or the tranportClient's open() or closeNow() throws.]
    @Test (expected = IOError.class)
    public void setOptionClientSASTokenExpiryTimeAfterClientOpenAMQPThrowsTransportDeviceIOOpen()
            throws IOException, URISyntaxException
    {
        // arrange
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getIotHubConnectionString().getSharedAccessKey();
                result = anyString;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.SAS_TOKEN;
                Deencapsulation.invoke(mockDeviceIO, "open");
                result =  new IOException();
            }
        };
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;

        DeviceClient client = new DeviceClient(connString, protocol);
        Deencapsulation.setField(client, "config", mockConfig);
        Deencapsulation.setField(client, "deviceIO", mockDeviceIO);
        final long value = 60;

        // act
        client.setOption("SetSASTokenExpiryTime", value);
    }

    // Tests_SRS_DEVICECLIENT_12_022: [If the client configured to use TransportClient the SetSendInterval shall throw IOException.]
    @Test (expected = IllegalStateException.class)
    public void setOptionWithTransportClientThrowsSetSendInterval()
            throws URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // act
        client.setOption("SetSendInterval", "thisIsNotALong");
    }

    // Tests_SRS_DEVICECLIENT_12_023: [If the client configured to use TransportClient the SetMinimumPollingInterval shall throw IOException.]
    @Test (expected = IllegalStateException.class)
    public void setOptionWithTransportClientThrowsSetMinimumPollingInterval()
            throws URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        DeviceClient client = new DeviceClient(connString, mockTransportClient);

        // act
        client.setOption("SetMinimumPollingInterval", "thisIsNotALong");
    }

    //Tests_SRS_DEVICECLIENT_34_065: [""SetSASTokenExpiryTime" if this option is called when not using sas token authentication, an IllegalStateException shall be thrown.*]
    @Test (expected = IllegalStateException.class)
    public void setOptionSASTokenExpiryTimeWhenNotUsingSasTokenAuthThrows() throws URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.X509_CERTIFICATE;
            }
        };
        DeviceClient client = new DeviceClient(connString, protocol, "someCert", false, "someKey", false);

        // act
        client.setOption("SetSASTokenExpiryTime", 25L);
    }

    // Tests_SRS_DEVICECLIENT_34_074: [If the provided connection string contains a module id field, this function shall throw an UnsupportedOperationException.]
    @Test (expected = UnsupportedOperationException.class)
    public void x509ConstructorThrowsIfConnStringContainsModuleIdField() throws URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.X509_CERTIFICATE;
                mockConfig.getModuleId();
                result = "any module id";
            }
        };

        //act
        new DeviceClient(connString, protocol, "someCert", false, "someKey", false);
    }

    // Tests_SRS_DEVICECLIENT_34_075: [If the provided connection string contains a module id field, this function shall throw an UnsupportedOperationException.]
    @Test (expected = UnsupportedOperationException.class)
    public void ConstructorThrowsIfConnStringContainsModuleIdField() throws URISyntaxException
    {
        // arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        final IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.X509_CERTIFICATE;
                mockConfig.getModuleId();
                result = "any module id";
            }
        };

        //act
        new DeviceClient(connString, protocol);
    }

    // Tests_SRS_DEVICECLIENT_34_073: [If this constructor is called with a connection string that contains a moduleId, this function shall throw an UnsupportedOperationException.]
    @Test (expected = UnsupportedOperationException.class)
    public void transportClientConstructorThrowsIfConnectionStringHasModuleId() throws URISyntaxException
    {
        //arrange
        final String connString = "HostName=iothub.device.com;CredentialType=SharedAccessKey;DeviceId=testdevice;"
                + "SharedAccessKey=adjkl234j52=";
        new NonStrictExpectations()
        {
            {
                mockDeviceIO.isOpen();
                result = true;
                mockDeviceIO.getProtocol();
                result = IotHubClientProtocol.HTTPS;
                mockConfig.getAuthenticationType();
                result = DeviceClientConfig.AuthType.X509_CERTIFICATE;
                mockConfig.getModuleId();
                result = "some module id";
            }
        };

        //act
        new DeviceClient(connString, mockTransportClient);
    }

    // Tests_SRS_DEVICECLIENT_12_028: [The constructor shall shall set the config, deviceIO and tranportClient to null.]
    @Test
    public void unusedConstructor()
    {
        // act
        DeviceClient client = Deencapsulation.newInstance(DeviceClient.class);

        // assert
        assertNull(Deencapsulation.getField(client, "config"));
        assertNull(Deencapsulation.getField(client, "deviceIO"));
    }
}
