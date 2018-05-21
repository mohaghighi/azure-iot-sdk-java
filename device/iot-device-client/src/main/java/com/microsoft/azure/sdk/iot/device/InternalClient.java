/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.device;

import com.microsoft.azure.sdk.iot.deps.serializer.ParserUtility;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.*;
import com.microsoft.azure.sdk.iot.device.fileupload.FileUpload;
import com.microsoft.azure.sdk.iot.device.transport.RetryPolicy;
import com.microsoft.azure.sdk.iot.device.transport.amqps.IoTHubConnectionType;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static com.microsoft.azure.sdk.iot.device.IotHubClientProtocol.AMQPS;
import static com.microsoft.azure.sdk.iot.device.IotHubClientProtocol.AMQPS_WS;

public class InternalClient
{
    /**
     * @deprecated as of release 1.2.27 this value is deprecated and will not be replaced.
     * The hostname attribute name in a connection string.
     */
    @Deprecated
    public static final String HOSTNAME_ATTRIBUTE = "HostName=";
    /**
     * @deprecated as of release 1.2.27 this value is deprecated and will not be replaced.
     * The device ID attribute name in a connection string.
     */
    @Deprecated
    public static final String DEVICE_ID_ATTRIBUTE = "DeviceId=";
    /**
     * @deprecated as of release 1.2.27 this value is deprecated and will not be replaced.
     * The shared access key attribute name in a connection string.
     */
    @Deprecated
    public static final String SHARED_ACCESS_KEY_ATTRIBUTE = "SharedAccessKey=";
    /**
     * @deprecated as of release 1.2.27 this value is deprecated and will not be replaced.
     * The shared access signature attribute name in a connection string.
     */
    @Deprecated
    public static final String SHARED_ACCESS_TOKEN_ATTRIBUTE = "SharedAccessSignature=";

    /**
     * @deprecated as of release 1.2.27 this value is deprecated and will not be replaced.
     * The charset used for URL-encoding the device ID in the connection
     * string.
     */
    @Deprecated
    public static final Charset CONNECTION_STRING_CHARSET = StandardCharsets.UTF_8;

    static final String SET_MINIMUM_POLLING_INTERVAL = "SetMinimumPollingInterval";
    static final String SET_SEND_INTERVAL = "SetSendInterval";
    static final String SET_CERTIFICATE_PATH = "SetCertificatePath";
    static final String SET_SAS_TOKEN_EXPIRY_TIME = "SetSASTokenExpiryTime";

    DeviceClientConfig config;
    DeviceIO deviceIO;

    private DeviceTwin deviceTwin;
    private DeviceMethod deviceMethod;
    private FileUpload fileUpload;

    InternalClient(IotHubConnectionString iotHubConnectionString, IotHubClientProtocol protocol, long sendPeriodMillis, long receivePeriodMillis)
    {
        /* Codes_SRS_INTERNALCLIENT_21_004: [If the connection string is null or empty, the function shall throw an IllegalArgumentException.] */
        commonConstructorVerification(iotHubConnectionString, protocol);

        this.config = new DeviceClientConfig(iotHubConnectionString, DeviceClientConfig.AuthType.SAS_TOKEN);
        this.config.setProtocol(protocol);

        this.deviceIO = new DeviceIO(this.config, sendPeriodMillis, receivePeriodMillis);
    }

    InternalClient(IotHubConnectionString iotHubConnectionString, IotHubClientProtocol protocol, String publicKeyCertificate, boolean isCertificatePath, String privateKey, boolean isPrivateKeyPath, long sendPeriodMillis, long receivePeriodMillis) throws URISyntaxException
    {
        // Codes_SRS_INTERNALCLIENT_34_078: [If the connection string or protocol is null, this function shall throw an IllegalArgumentException.]
        commonConstructorVerification(iotHubConnectionString, protocol);

        // Codes_SRS_INTERNALCLIENT_34_079: [This function shall save a new config using the provided connection string, and x509 certificate information.]
        this.config = new DeviceClientConfig(iotHubConnectionString, publicKeyCertificate, isCertificatePath, privateKey, isPrivateKeyPath);
        this.config.setProtocol(protocol);

        // Codes_SRS_INTERNALCLIENT_34_080: [This function shall save a new DeviceIO instance using the created config and the provided send/receive periods.]
        this.deviceIO = new DeviceIO(this.config, sendPeriodMillis, receivePeriodMillis);
    }

    InternalClient(String uri, String deviceId, SecurityProvider securityProvider, IotHubClientProtocol protocol, long sendPeriodMillis, long receivePeriodMillis) throws URISyntaxException, IOException
    {
        if (protocol == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_072: [If the provided protocol is null, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("The transport protocol cannot be null");
        }

        if (securityProvider == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_073: [If the provided securityProvider is null, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("securityProvider cannot be null");
        }

        if (uri == null || uri.isEmpty())
        {
            //Codes_SRS_INTERNALCLIENT_34_074: [If the provided uri is null, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("URI cannot be null or empty");
        }

        if (deviceId == null || deviceId.isEmpty())
        {
            //Codes_SRS_INTERNALCLIENT_34_075: [If the provided deviceId is null, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("deviceId cannot be null or empty");
        }

        //Codes_SRS_INTERNALCLIENT_34_065: [The provided uri and device id will be used to create an iotHubConnectionString that will be saved in config.]
        IotHubConnectionString connectionString = new IotHubConnectionString(uri, deviceId, null, null);

        //Codes_SRS_INTERNALCLIENT_34_066: [The provided security provider will be saved in config.]
        this.config = new DeviceClientConfig(connectionString, securityProvider);
        this.config.setProtocol(protocol);

        //Codes_SRS_INTERNALCLIENT_34_067: [The constructor shall initialize the IoT Hub transport for the protocol specified, creating a instance of the deviceIO.]
        this.deviceIO = new DeviceIO(this.config, sendPeriodMillis, receivePeriodMillis);
    }

    //unused
    InternalClient()
    {
        // Codes_SRS_INTERNALCLIENT_12_028: [The constructor shall shall set the config, deviceIO and tranportClient to null.]
        this.config = null;
        this.deviceIO = null;
    }

    public void open() throws IOException
    {
        if (this.config.getAuthenticationType() == DeviceClientConfig.AuthType.SAS_TOKEN && this.config.getSasTokenAuthentication().isRenewalNecessary())
        {
            //Codes_SRS_INTERNALCLIENT_34_044: [If the SAS token has expired before this call, throw a Security Exception]
            throw new SecurityException("Your SasToken is expired");
        }

        //Codes_SRS_INTERNALCLIENT_21_006: [The open shall open the deviceIO connection.]
        this.deviceIO.open();
    }

    public void close() throws IOException
    {
        while (!this.deviceIO.isEmpty())
        {
            // Don't do anything, can be infinite.
        }

        //Codes_SRS_INTERNALCLIENT_21_042: [The closeNow shall closeNow the deviceIO connection.]
        this.deviceIO.close();
    }

    public void closeNow() throws IOException
    {
        //Codes_SRS_INTERNALCLIENT_21_008: [The closeNow shall closeNow the deviceIO connection.]
        this.deviceIO.close();

        //Codes_SRS_INTERNALCLIENT_21_054: [If the fileUpload is not null, the closeNow shall call closeNow on fileUpload.]
        closeFileUpload();
    }

    void closeFileUpload() throws IOException
    {
        if (this.fileUpload != null)
        {
            this.fileUpload.closeNow();
        }
    }

    /**
     * Asynchronously sends an event message to the IoT Hub.
     *
     * @param message the message to be sent.
     * @param callback the callback to be invoked when a response is received.
     * Can be {@code null}.
     * @param callbackContext a context to be passed to the callback. Can be
     * {@code null} if no callback is provided.
     *
     * @throws IllegalArgumentException if the message provided is {@code null}.
     * @throws IllegalStateException if the client has not been opened yet or is
     * already closed.
     */
    public void sendEventAsync(Message message, IotHubEventCallback callback, Object callbackContext)
    {
        //Codes_SRS_INTERNALCLIENT_21_010: [The sendEventAsync shall asynchronously send the message using the deviceIO connection.]
        deviceIO.sendEventAsync(message, callback, callbackContext, this.config.getIotHubConnectionString());
    }

    /**
     * Starts the device twin.
     *
     * @param deviceTwinStatusCallback the IotHubEventCallback callback for providing the status of Device Twin operations. Cannot be {@code null}.
     * @param deviceTwinStatusCallbackContext the context to be passed to the status callback. Can be {@code null}.
     * @param genericPropertyCallBack the PropertyCallBack callback for providing any changes in desired properties. Cannot be {@code null}.
     * @param genericPropertyCallBackContext the context to be passed to the property callback. Can be {@code null}.     *
     *
     * @throws IllegalArgumentException if the callback is {@code null}
     * @throws UnsupportedOperationException if called more than once on the same device
     * @throws IOException if called when client is not opened
     */
    public void startDeviceTwin(IotHubEventCallback deviceTwinStatusCallback, Object deviceTwinStatusCallbackContext,
                                PropertyCallBack genericPropertyCallBack, Object genericPropertyCallBackContext)
            throws IOException
    {
        if (!this.deviceIO.isOpen())
        {
            throw new IOException("Open the client connection before using it.");
        }

        if (deviceTwinStatusCallback == null || genericPropertyCallBack == null)
        {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        if (this.deviceTwin == null)
        {
            deviceTwin = new DeviceTwin(
                    this.deviceIO,
                    this.config,
                    deviceTwinStatusCallback,
                    deviceTwinStatusCallbackContext,
                    genericPropertyCallBack,
                    genericPropertyCallBackContext);

            deviceTwin.getDeviceTwin();
        }
        else
        {
            throw new UnsupportedOperationException("You have already initialised twin");
        }
    }

    /**
     * Starts the device twin.
     *
     * @param deviceTwinStatusCallback the IotHubEventCallback callback for providing the status of Device Twin operations. Cannot be {@code null}.
     * @param deviceTwinStatusCallbackContext the context to be passed to the status callback. Can be {@code null}.
     * @param genericPropertyCallBack the TwinPropertyCallBack callback for providing any changes in desired properties. Cannot be {@code null}.
     * @param genericPropertyCallBackContext the context to be passed to the property callback. Can be {@code null}.     *
     *
     * @throws IllegalArgumentException if the callback is {@code null}
     * @throws UnsupportedOperationException if called more than once on the same device
     * @throws IOException if called when client is not opened
     */
    public void startDeviceTwin(IotHubEventCallback deviceTwinStatusCallback, Object deviceTwinStatusCallbackContext,
                                TwinPropertyCallBack genericPropertyCallBack, Object genericPropertyCallBackContext)
            throws IOException
    {
        if (!this.deviceIO.isOpen())
        {
            //Codes_SRS_INTERNALCLIENT_34_081: [If device io has not been opened yet, this function shall throw an IOException.]
            throw new IOException("Open the client connection before using it.");
        }

        if (deviceTwinStatusCallback == null || genericPropertyCallBack == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_082: [If either callback is null, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("Callback cannot be null");
        }
        if (this.deviceTwin == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_084: [This function shall initialize a DeviceTwin object and invoke getDeviceTwin on it.]
            deviceTwin = new DeviceTwin(this.deviceIO, this.config, deviceTwinStatusCallback, deviceTwinStatusCallbackContext,
                    genericPropertyCallBack, genericPropertyCallBackContext);
            deviceTwin.getDeviceTwin();
        }
        else
        {
            //Codes_SRS_INTERNALCLIENT_34_083: [If either callback is null, this function shall throw an IllegalArgumentException.]
            throw new UnsupportedOperationException("You have already initialised twin");
        }
    }

    public void getDeviceTwin() throws IOException
    {
        if (this.deviceTwin == null)
        {
            //Codes_SRS_INTERNALCLIENT_21_040: [If the client has not started twin before calling this method, the function shall throw an IOException.]
            throw new IOException("Start twin before using it");
        }

        if (!this.deviceIO.isOpen())
        {

            //Codes_SRS_INTERNALCLIENT_21_041: [If the client has not been open, the function shall throw an IOException.]
            throw new IOException("Open the client connection before using it.");
        }

        //Codes_SRS_INTERNALCLIENT_21_042: [The function shall get all desired properties by calling getDeviceTwin.]
        this.deviceTwin.getDeviceTwin();
    }

    /**
     * Sets the message callback.
     *
     * @param callback the message callback. Can be {@code null}.
     * @param context the context to be passed to the callback. Can be {@code null}.
     *
     * @return itself, for fluent setting.
     *
     * @throws IllegalArgumentException if the callback is {@code null} but a context is
     * passed in.
     * @throws IllegalStateException if the callback is set after the client is
     * closed.
     */
    public InternalClient setMessageCallback(MessageCallback callback, Object context)
    {
        if (callback == null && context != null)
        {
            /* Codes_SRS_INTERNALCLIENT_11_014: [If the callback is null but the context is non-null, the function shall throw an IllegalArgumentException.] */
            throw new IllegalArgumentException("Cannot give non-null context for a null callback.");
        }

        /* Codes_SRS_INTERNALCLIENT_11_013: [The function shall set the message callback, with its associated context.] */
        this.config.setMessageCallback(callback, context);
        return this;
    }

    /**
     * Subscribes to desired properties
     *
     * @param onDesiredPropertyChange the Map for desired properties and their corresponding callback and context. Can be {@code null}.
     *
     * @throws IOException if called when client is not opened or called before starting twin.
     */
    public void subscribeToDesiredProperties(Map<Property, Pair<PropertyCallBack<String, Object>, Object>> onDesiredPropertyChange) throws IOException
    {
        if (this.deviceTwin == null)
        {
            //Codes_SRS_INTERNALCLIENT_25_029: [If the client has not started twin before calling this method, the function shall throw an IOException.]
            throw new IOException("Start twin before using it");
        }

        if (!this.deviceIO.isOpen())
        {
            //Codes_SRS_INTERNALCLIENT_25_030: [If the client has not been open, the function shall throw an IOException.]
            throw new IOException("Open the client connection before using it.");
        }

        //Codes_SRS_INTERNALCLIENT_25_031: [This method shall subscribe to desired properties by calling subscribeDesiredPropertiesNotification on the twin object.]
        this.deviceTwin.subscribeDesiredPropertiesNotification(onDesiredPropertyChange);
    }

    /**
     * Subscribes to desired properties
     *
     * @param onDesiredPropertyChange the Map for desired properties and their corresponding callback and context. Can be {@code null}.
     *
     * @throws IOException if called when client is not opened or called before starting twin.
     */
    public void subscribeToTwinDesiredProperties(Map<Property, Pair<TwinPropertyCallBack, Object>> onDesiredPropertyChange) throws IOException
    {
        if (this.deviceTwin == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_087: [If the client has not started twin before calling this method, the function shall throw an IOException.]
            throw new IOException("Start twin before using it");
        }

        if (!this.deviceIO.isOpen())
        {
            //Codes_SRS_INTERNALCLIENT_34_086: [If the client has not been open, the function shall throw an IOException.]
            throw new IOException("Open the client connection before using it.");
        }

        //Codes_SRS_INTERNALCLIENT_34_085: [This method shall subscribe to desired properties by calling subscribeDesiredPropertiesNotification on the twin object.]
        this.deviceTwin.subscribeDesiredPropertiesTwinPropertyNotification(onDesiredPropertyChange);
    }

    /**
     * Sends reported properties
     *
     * @param reportedProperties the Set for desired properties and their corresponding callback and context. Cannot be {@code null}.
     *
     * @throws IOException if called when client is not opened or called before starting twin.
     * @throws IllegalArgumentException if reportedProperties is null or empty.
     */
    public void sendReportedProperties(Set<Property> reportedProperties) throws IOException
    {
        if (this.deviceTwin == null)
        {
            throw new IOException("Start twin before using it");
        }

        if (!this.deviceIO.isOpen())
        {
            throw new IOException("Open the client connection before using it.");
        }

        if (reportedProperties == null || reportedProperties.isEmpty())
        {
            throw new IllegalArgumentException("Reported properties set cannot be null or empty.");
        }

        this.deviceTwin.updateReportedProperties(reportedProperties);
    }

    /**
     * Sends reported properties
     *
     * @param reportedProperties the Set for desired properties and their corresponding callback and context. Cannot be {@code null}.
     * @param version the Reported property version. Cannot be negative.
     *
     * @throws IOException if called when client is not opened or called before starting twin.
     * @throws IllegalArgumentException if reportedProperties is null or empty.
     */
    public void sendReportedProperties(Set<Property> reportedProperties, int version) throws IOException
    {
        if (this.deviceTwin == null)
        {
            throw new IOException("Start twin before using it");
        }

        if (!this.deviceIO.isOpen())
        {
            throw new IOException("Open the client connection before using it.");
        }

        if (reportedProperties == null || reportedProperties.isEmpty())
        {
            throw new IllegalArgumentException("Reported properties set cannot be null or empty.");
        }

        if(version < 0)
        {
            throw new IllegalArgumentException("Version cannot be null.");
        }

        this.deviceTwin.updateReportedProperties(reportedProperties, version);
    }

    /**
     * Subscribes to device methods
     *
     * @param deviceMethodCallback Callback on which device methods shall be invoked. Cannot be {@code null}.
     * @param deviceMethodCallbackContext Context for device method callback. Can be {@code null}.
     * @param deviceMethodStatusCallback Callback for providing IotHub status for device methods. Cannot be {@code null}.
     * @param deviceMethodStatusCallbackContext Context for device method status callback. Can be {@code null}.
     *
     * @throws IOException if called when client is not opened.
     * @throws IllegalArgumentException if either callback are null.
     */
    public void subscribeToDeviceMethod(DeviceMethodCallback deviceMethodCallback, Object deviceMethodCallbackContext,
                                        IotHubEventCallback deviceMethodStatusCallback, Object deviceMethodStatusCallbackContext)
            throws IOException
    {
        if (!this.deviceIO.isOpen())
        {
            throw new IOException("Open the client connection before using it.");
        }

        if (deviceMethodCallback == null || deviceMethodStatusCallback == null)
        {
            throw new IllegalArgumentException("Callback cannot be null");
        }

        if (this.deviceMethod == null)
        {
            this.deviceMethod = new DeviceMethod(this.deviceIO, this.config, deviceMethodStatusCallback, deviceMethodStatusCallbackContext);
        }

        this.deviceMethod.subscribeToDeviceMethod(deviceMethodCallback, deviceMethodCallbackContext);
    }

    /**
     * Asynchronously upload a stream to the IoT Hub.
     *
     * @param destinationBlobName is a string with the name of the file in the storage.
     * @param inputStream is a InputStream with the stream to upload in the blob.
     * @param streamLength is a long with the number of bytes in the stream to upload.
     * @param callback the callback to be invoked when a file is uploaded.
     * @param callbackContext a context to be passed to the callback. Can be {@code null}.
     *
     * @throws IllegalArgumentException if the provided blob name, or the file path is {@code null},
     *          empty or not valid, or if the callback is {@code null}.
     * @throws IOException if the client cannot create a instance of the FileUpload or the transport.
     * @throws UnsupportedOperationException if this method is called when using x509 authentication
     */
    public void uploadToBlobAsync(String destinationBlobName, InputStream inputStream, long streamLength,
                                  IotHubEventCallback callback, Object callbackContext) throws IllegalArgumentException, IOException
    {
        if (callback == null)
        {
            throw new IllegalArgumentException("Callback is null");
        }

        if (inputStream == null)
        {
            throw new IllegalArgumentException("The input stream cannot be null.");
        }

        if (streamLength < 0)
        {
            throw new IllegalArgumentException("Invalid stream size.");
        }

        ParserUtility.validateBlobName(destinationBlobName);

        if (this.config.getAuthenticationType() == DeviceClientConfig.AuthType.X509_CERTIFICATE)
        {
            throw new UnsupportedOperationException("File Upload does not support x509 authentication");
        }

        if (this.fileUpload == null)
        {
            this.fileUpload = new FileUpload(this.config);
        }

        this.fileUpload.uploadToBlobAsync(destinationBlobName, inputStream, streamLength, callback, callbackContext);
    }

    /**
     * Registers a callback to be executed whenever the connection to the device is lost or established.
     * @deprecated as of release 1.10.0 by {@link #registerConnectionStatusChangeCallback(IotHubConnectionStatusChangeCallback callback, Object callbackContext)}
     * @param callback the callback to be called.
     * @param callbackContext a context to be passed to the callback. Can be
     * {@code null} if no callback is provided.
     */
    @Deprecated
    public void registerConnectionStateCallback(IotHubConnectionStateCallback callback, Object callbackContext)
    {
        if (null == callback)
        {
            throw new IllegalArgumentException("Callback object cannot be null");
        }

        this.deviceIO.registerConnectionStateCallback(callback, callbackContext);
    }

    /**
     * Registers a callback to be executed when the connection status of the device changes. The callback will be fired
     * with a status and a reason why the device's status changed. When the callback is fired, the provided context will
     * be provided alongside the status and reason.
     *
     * @param callback The callback to be fired when the connection status of the device changes
     * @param callbackContext a context to be passed to the callback. Can be
     * {@code null} if no callback is provided.
     * @throws IllegalArgumentException if provided callback is null
     */
    public void registerConnectionStatusChangeCallback(IotHubConnectionStatusChangeCallback callback, Object callbackContext) throws IllegalArgumentException
    {
        if (callback == null)
        {
            //Codes_SRS_INTERNALCLIENT_34_068: [If the callback is null the method shall throw an IllegalArgument exception.]
            throw new IllegalArgumentException("Callback cannot be null");
        }

        //Codes_SRS_INTERNALCLIENT_34_069: [This function shall register the provided callback and context with its device IO instance.]
        this.deviceIO.registerConnectionStatusChangeCallback(callback, callbackContext);
    }

    /**
     * Sets the given retry policy on the underlying transport
     * <a href="https://github.com/Azure/azure-iot-sdk-java/blob/master/device/iot-device-client/devdoc/requirement_docs/com/microsoft/azure/iothub/retryPolicy.md">
     *     See more details about the default retry policy and about using custom retry policies here</a>
     * @param retryPolicy the new interval in milliseconds
     */
    public void setRetryPolicy(RetryPolicy retryPolicy)
    {
        //Codes_SRS_INTERNALCLIENT_28_001: [The function shall set the device config's RetryPolicy .]
        this.config.setRetryPolicy(retryPolicy);
    }

    /**
     * Set the length of time, in milliseconds, that any given operation will expire in. These operations include
     * reconnecting upon a connection drop and sending a message.
     * @param timeout the length in time, in milliseconds, until a given operation shall expire
     * @throws IllegalArgumentException if the provided timeout is 0 or negative
     */
    public void setOperationTimeout(long timeout) throws IllegalArgumentException
    {
        // Codes_SRS_INTERNALCLIENT_34_070: [The function shall set the device config's operation timeout .]
        this.config.setOperationTimeout(timeout);
    }

    public ProductInfo getProductInfo()
    {
        // Codes_SRS_INTERNALCLIENT_34_071: [This function shall return the product info saved in config.]
        return this.config.getProductInfo();
    }

    /**
     * Getter for the device client config.
     *
     * @return the value of the config.
     */
    public DeviceClientConfig getConfig()
    {
        return this.config;
    }

    public void setOption(String optionName, Object value)
    {
        if (optionName == null)
        {
            // Codes_SRS_DEVICECLIENT_02_015: [If optionName is null or not an option handled by the client, then
            // it shall throw IllegalArgumentException.]
            throw new IllegalArgumentException("optionName is null");
        }
        else if (value == null)
        {
            // Codes_SRS_DEVICECLIENT_12_026: [The function shall trow IllegalArgumentException if the value is null.]
            throw new IllegalArgumentException("optionName is null");
        }
        else
        {
            switch (optionName)
            {
                case SET_MINIMUM_POLLING_INTERVAL:
                {
                    if (this.deviceIO.isOpen())
                    {
                        throw new IllegalStateException("setOption " + SET_MINIMUM_POLLING_INTERVAL +
                                "only works when the transport is closed");
                    }
                    else
                    {
                        if (this.deviceIO.getProtocol() == IotHubClientProtocol.HTTPS)
                        {
                            setOption_SetMinimumPollingInterval(value);
                        }
                        else
                        {
                            throw new IllegalArgumentException("optionName is unknown = " + optionName + " for " + this.deviceIO.getProtocol().toString());
                        }
                    }

                    break;
                }
                case SET_SEND_INTERVAL:
                {
                    setOption_SetSendInterval(value);
                    break;
                }
                case SET_CERTIFICATE_PATH:
                {
                    if ((this.deviceIO != null) && (this.deviceIO.isOpen()))
                    {
                        throw new IllegalStateException("setOption " + SET_CERTIFICATE_PATH + " only works when the transport is closed");
                    }
                    else
                    {
                        if ((this.deviceIO.getProtocol() == AMQPS) || (this.deviceIO.getProtocol() == AMQPS_WS))
                        {
                            setOption_SetCertificatePath(value);
                        }
                        else
                        {
                            throw new IllegalArgumentException("optionName is unknown = " + optionName + " for " + this.deviceIO.getProtocol().toString());
                        }
                    }

                    break;
                }
                case SET_SAS_TOKEN_EXPIRY_TIME:
                {
                    setOption_SetSASTokenExpiryTime(value);
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException("optionName is unknown = " + optionName);
                }
            }
        }
    }

    /**
     * Getter for the underlying DeviceIO for multiplexing scenarios.
     *
     * @return the value of the underlying DeviceIO.
     */
    DeviceIO getDeviceIO()
    {
        return this.deviceIO;
    }

    /**
     * Setter for the underlying DeviceIO for multiplexing scenarios.
     *
     * @param deviceIO is the DeviceIO to set.
     */
    void setDeviceIO(DeviceIO deviceIO)
    {
        this.deviceIO = deviceIO;
    }

    void setOption_SetCertificatePath(Object value)
    {
        if (value != null)
        {
            if (this.config.getAuthenticationType() == DeviceClientConfig.AuthType.SAS_TOKEN)
            {
                this.config.getSasTokenAuthentication().setPathToIotHubTrustedCert((String) value);
            }
            else if (this.config.getAuthenticationType() == DeviceClientConfig.AuthType.X509_CERTIFICATE)
            {
                this.config.getX509Authentication().setPathToIotHubTrustedCert((String) value);
            }
        }
    }

    void setOption_SetSendInterval(Object value)
    {
        if (value != null)
        {
            // Codes_SRS_DEVICECLIENT_21_041: ["SetSendInterval" needs to have value type long.]
            if (value instanceof Long)
            {
                try
                {
                    this.deviceIO.setSendPeriodInMilliseconds((long) value);
                }
                catch (IOException e)
                {
                    throw new IOError(e);
                }
            }
            else
            {
                throw new IllegalArgumentException("value is not long = " + value);
            }
        }
    }

    void setOption_SetMinimumPollingInterval(Object value)
    {
        if (value != null)
        {
            // Codes_SRS_DEVICECLIENT_02_018: ["SetMinimumPollingInterval" needs to have type long].
            if (value instanceof Long)
            {
                try
                {
                    this.deviceIO.setReceivePeriodInMilliseconds((long) value);
                }
                catch (IOException e)
                {
                    throw new IOError(e);
                }
            }
            else
            {
                throw new IllegalArgumentException("value is not long = " + value);
            }
        }
    }

    void setOption_SetSASTokenExpiryTime(Object value)
    {
        if (this.config.getAuthenticationType() != DeviceClientConfig.AuthType.SAS_TOKEN)
        {
            throw new IllegalStateException("Cannot set sas token validity time when not using sas token authentication");
        }

        if (value != null)
        {
            long validTimeInSeconds;

            if (value instanceof Long)
            {
                validTimeInSeconds = (long) value;
            }
            else
            {
                throw new IllegalArgumentException("value is not long = " + value);
            }

            this.config.getSasTokenAuthentication().setTokenValidSecs(validTimeInSeconds);

            if (this.deviceIO != null)
            {
                if (this.deviceIO.isOpen())
                {
                    try
                    {
                        /* Codes_SRS_DEVICECLIENT_25_024: [**"SetSASTokenExpiryTime" shall restart the transport
                         *                                  1. If the device currently uses device key and
                         *                                  2. If transport is already open
                         *                                 after updating expiry time
                         */
                        if (this.config.getIotHubConnectionString().getSharedAccessKey() != null)
                        {
                            this.deviceIO.close();
                            this.deviceIO.open();
                        }
                    }
                    catch (IOException e)
                    {
                        // Codes_SRS_DEVICECLIENT_12_027: [The function shall throw IOError if either the deviceIO or the tranportClient's open() or closeNow() throws.]
                        throw new IOError(e);
                    }
                }
            }
        }
    }

    private void commonConstructorVerification(IotHubConnectionString connectionString, IotHubClientProtocol protocol)
    {
        if (connectionString == null)
        {
            throw new IllegalArgumentException("Connection string cannot be null");
        }

        if (protocol == null)
        {
            throw new IllegalArgumentException("Protocol cannot be null.");
        }
    }
}
