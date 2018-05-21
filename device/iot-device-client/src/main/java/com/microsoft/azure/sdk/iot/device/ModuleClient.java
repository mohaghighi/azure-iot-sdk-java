/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.device;

import com.microsoft.azure.sdk.iot.device.DeviceTwin.*;
import com.microsoft.azure.sdk.iot.device.exceptions.ModuleClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * TODO is this still accurate?
 * Public API for communicating with Edge Modules. A ModuleClient can be used to send messages to an edge device's modules,
 * update twin properties on that module, and receive methods from IotHub.
 */
public class ModuleClient extends InternalClient
{
    private static long SEND_PERIOD_MILLIS = 10;

    private static long RECEIVE_PERIOD_MILLIS_AMQPS = 10;
    private static long RECEIVE_PERIOD_MILLIS_MQTT = 10;
    private static long RECEIVE_PERIOD_MILLIS_HTTPS = 25 * 60 * 1000; /*25 minutes*/

    private static final String IotEdgedUriVariableName = "IOTEDGE_IOTEDGEDURI";
    private static final String IotEdgedApiVersionVariableName = "IOTEDGE_IOTEDGEDVERSION";
    private static final String IotHubHostnameVariableName = "IOTEDGE_IOTHUBHOSTNAME";
    private static final String GatewayHostnameVariableName = "IOTEDGE_GATEWAYHOSTNAME";
    private static final String DeviceIdVariableName = "IOTEDGE_DEVICEID";
    private static final String ModuleIdVariableName = "IOTEDGE_MODULEID";
    private static final String AuthSchemeVariableName = "IOTEDGE_AUTHSCHEME";
    private static final String SasTokenAuthScheme = "SasToken";
    private static final String EdgehubConnectionstringVariableName = "EdgeHubConnectionString";
    private static final String IothubConnectionstringVariableName = "IotHubConnectionString";
    
    /**
     * Constructor for a ModuleClient instance.
     * @param connectionString The connection string for the edge module to connect to. Must be in format
     *                         HostName=xxxx;DeviceId=xxxx;SharedAccessKey=
     *                         xxxx;ModuleId=xxxx;
     *
     *                         or
     *
     *                         HostName=xxxx;DeviceId=xxxx;SharedAccessKey=
     *                         xxxx;ModuleId=xxxx;HostNameGateway=xxxx
     * @param protocol The protocol to use when communicating with the module
     * @throws ModuleClientException if an exception is encountered when parsing the connection string
     * @throws UnsupportedOperationException if using any protocol besides MQTT, if the connection string is missing
     * the "ModuleId" field, or if the connection string uses x509
     * @throws IllegalArgumentException if the provided connection string is null or empty, or if the provided protocol is null
     */
    public ModuleClient(String connectionString, IotHubClientProtocol protocol) throws URISyntaxException, IllegalArgumentException, UnsupportedOperationException
    {
        //Codes_SRS_MODULECLIENT_34_006: [This function shall invoke the super constructor.]
        super(new IotHubConnectionString(connectionString), protocol, SEND_PERIOD_MILLIS, getReceivePeriod(protocol));

        commonConstructorVerifications(protocol, this.getConfig());

        if (protocol != IotHubClientProtocol.MQTT && protocol != IotHubClientProtocol.AMQPS)
        {
            //throw new UnsupportedOperationException("Only MQTT and AMQPS are supported for ModuleClient.");
        }

        if (this.getConfig().getModuleId() == null || this.getConfig().getModuleId().isEmpty())
        {
            //Codes_SRS_MODULECLIENT_34_004: [If the provided connection string does not contain a module id, this function shall throw an UnsupportedOperationException.]
            throw new UnsupportedOperationException("Connection string must contain field for ModuleId");
        }

        if (this.getConfig().getAuthenticationType() == DeviceClientConfig.AuthType.X509_CERTIFICATE)
        {
            //Codes_SRS_MODULECLIENT_34_005: [If the provided connection string uses x509, this function shall throw an UnsupportedOperationException.]
            throw new UnsupportedOperationException("Connection string for this constructor must use SAS authentication");
        }
    }

    /**
     * Create a module client instance that uses x509 authentication.
     *
     * <p>Note! Communication from a module to another EdgeHub using x509 authentication is not currently supported and
     * will always return "UNAUTHORIZED"</p>
     *
     * <p>Communication from a module directly to the IotHub does support x509 authentication, though.</p>
     * @param connectionString The connection string for the edge module to connect to. Must be in format
     *                         HostName=xxxx;DeviceId=xxxx;SharedAccessKey=
     *                         xxxx;ModuleId=xxxx;
     *
     *                         or
     *
     *                         HostName=xxxx;DeviceId=xxxx;SharedAccessKey=
     *                         xxxx;ModuleId=xxxx;HostNameGateway=xxxx
     * @param protocol The protocol to communicate with
     * @param publicKeyCertificate The PEM formatted string for the public key certificate or the system path to the file containing the PEM.
     * @param isCertificatePath 'false' if the publicKeyCertificate argument is a path to the PEM, and 'true' if it is the PEM string itself,
     * @param privateKey The PEM formatted string for the private key or the system path to the file containing the PEM.
     * @param isPrivateKeyPath 'false' if the privateKey argument is a path to the PEM, and 'true' if it is the PEM string itself,
     * @throws URISyntaxException If the connString cannot be parsed
     */
    public ModuleClient(String connectionString, IotHubClientProtocol protocol, String publicKeyCertificate, boolean isCertificatePath, String privateKey, boolean isPrivateKeyPath) throws URISyntaxException
    {
        super(new IotHubConnectionString(connectionString), protocol, publicKeyCertificate, isCertificatePath, privateKey, isPrivateKeyPath, SEND_PERIOD_MILLIS, getReceivePeriod(protocol));

        if (this.config.getAuthenticationType() != DeviceClientConfig.AuthType.X509_CERTIFICATE)
        {
            throw new UnsupportedOperationException("Connection string for this constructor must contain field 'x509=true;'");
        }

        commonConstructorVerifications(protocol, this.getConfig());

        //TODO check for moduleid
        //Jasmine is talking with Edge folks about how SDKs should handle the fact that x509 auth works from module to iot hub, but not module to edge hub
        //either throw unsupported operation if using x509 and a gatewayhostname is present, or have Jasmine document on service client side that x509 module -> edge hub doesn't work.
        //confer with other SDKs
    }

    public static ModuleClient createInternalClientFromEnvironment() throws ModuleClientException
    {
        Map<String, String> envVariables = System.getenv();

        String connectionString = envVariables.get(EdgehubConnectionstringVariableName);

        if (connectionString == null)
        {
            connectionString = envVariables.get(IothubConnectionstringVariableName);
        }

        // First try to create from connection string and if env variable for connection string is not found try to create from edgedUri
        if (connectionString != null)
        {
            try
            {
                return new ModuleClient(connectionString, IotHubClientProtocol.AMQPS);
            }
            catch (URISyntaxException e)
            {
                throw new ModuleClientException(e);
            }
        }
        else
        {
            //throw new UnsupportedOperationException("Cannot create from environment without iothub connection string saved in environment variables");

            //TODO look into how C# does HSM work here?
            String edgedUri = envVariables.get(IotEdgedUriVariableName);
            String deviceId = envVariables.get(DeviceIdVariableName);
            String moduleId = envVariables.get(ModuleIdVariableName);
            String hostname = envVariables.get(IotHubHostnameVariableName);
            String authScheme = envVariables.get(AuthSchemeVariableName);
            String gateway = envVariables.get(GatewayHostnameVariableName);
            String apiVersion = envVariables.get(IotEdgedApiVersionVariableName);

            if (edgedUri == null)
            {
                throw new ModuleClientException("Environment variable" + IotEdgedUriVariableName + " is required.");
            }

            if (deviceId == null)
            {
                throw new ModuleClientException("Environment variable" + DeviceIdVariableName + " is required.");
            }

            if (moduleId == null)
            {
                throw new ModuleClientException("Environment variable" + ModuleIdVariableName + " is required.");
            }
            if (hostname == null)
            {
                throw new ModuleClientException("Environment variable" + IotHubHostnameVariableName + " is required.");
            }

            if (authScheme == null)
            {
                throw new ModuleClientException("Environment variable" + AuthSchemeVariableName + " is required.");
            }


            if (!authScheme.equals(SasTokenAuthScheme))
            {
                throw new ModuleClientException("Unsupported authentication scheme. Supported scheme is " + SasTokenAuthScheme + ".");
            }

            //TODO
            /*
            ISignatureProvider signatureProvider = string.IsNullOrWhiteSpace(apiVersion)
                    ? new HttpHsmSignatureProvider(edgedUri)
                    : new HttpHsmSignatureProvider(edgedUri, apiVersion);
            var authMethod = new ModuleAuthenticationWithHsm(signatureProvider, deviceId, moduleId);

            return this.CreateInternalClientFromAuthenticationMethod(hostname, gateway, authMethod);
            */

            return null;
        }
    }

    /**
     * Sends a message to a particular outputName asynchronously
     *
     * @param outputName the outputName to route the message to
     * @param message the message to send
     * @param callback the callback to be fired when the message is acknowledged by the service
     * @param callbackContext the context to be included in the callback when fired
     * @throws IllegalArgumentException if the provided outputName is null or empty
     */
    public void sendEventAsync(String outputName, Message message, IotHubEventCallback callback, Object callbackContext) throws IllegalArgumentException
    {
        if (outputName == null || outputName.isEmpty())
        {
            //Codes_SRS_MODULECLIENT_34_001: [If the provided outputName is null or empty, this function shall throw an IllegalArgumentException.]
            throw new IllegalArgumentException("outputName cannot be null or empty");
        }

        //Codes_SRS_MODULECLIENT_34_002: [This function shall set the provided message with the provided outputName, device id, and module id properties.]
        this.setModuleProperties(message);
        message.setOutputName(outputName);

        //Codes_SRS_MODULECLIENT_34_003: [This function shall invoke super.sendEventAsync(message, callback, callbackContext).]
        super.sendEventAsync(message, callback, callbackContext);
    }

    @Override
    public InternalClient setMessageCallback(MessageCallback callback, Object context)
    {
        //TODO right?
        throw new UnsupportedOperationException();
    }

    @Override
    public void uploadToBlobAsync(String destinationBlobName, InputStream inputStream, long streamLength,
                                  IotHubEventCallback callback, Object callbackContext) throws IllegalArgumentException, IOException
    {
        //right?
        throw new UnsupportedOperationException();
    }


    private void setModuleProperties(Message message)
    {
        String deviceId = this.getConfig().getDeviceId();
        String moduleId = this.getConfig().getModuleId();

        message.setUserId(deviceId + "/" + moduleId);
        message.setConnectionModuleId(moduleId);
        message.setConnectionDeviceId(deviceId);
    }

    private static long getReceivePeriod(IotHubClientProtocol protocol)
    {
        switch (protocol)
        {
            case HTTPS:
                return RECEIVE_PERIOD_MILLIS_HTTPS;
            case AMQPS:
            case AMQPS_WS:
                return RECEIVE_PERIOD_MILLIS_AMQPS;
            case MQTT:
            case MQTT_WS:
                return RECEIVE_PERIOD_MILLIS_MQTT;
            default:
                // should never happen.
                throw new IllegalStateException(
                        "Invalid client protocol specified.");
        }
    }

    private static void commonConstructorVerifications(IotHubClientProtocol protocol, DeviceClientConfig config)
    {
        if (protocol != IotHubClientProtocol.MQTT && protocol != IotHubClientProtocol.AMQPS)
        {
            throw new UnsupportedOperationException("Only MQTT and AMQPS are supported for ModuleClient.");
        }

        if (config.getModuleId() == null || config.getModuleId().isEmpty())
        {
            //Codes_SRS_MODULECLIENT_34_004: [If the provided connection string does not contain a module id, this function shall throw an UnsupportedOperationException.]
            throw new UnsupportedOperationException("Connection string must contain field for ModuleId");
        }
    }
}
