// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package samples.com.microsoft.azure.sdk.iot;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.ModuleClientException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


/** Sends a number of event messages to an IoT Hub from a module. */
public class SendModuleEvent
{
    private  static final int D2C_MESSAGE_TIMEOUT = 5000; // 5 seconds
    private  static List failedMessageListOnClose = new ArrayList(); // List of messages that failed on close

    protected static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            Message msg = (Message) context;

            System.out.println("IoT Hub responded to message "+ msg.getMessageId()  + " with status " + status.name());

            if (status==IotHubStatusCode.MESSAGE_CANCELLED_ONCLOSE)
            {
                failedMessageListOnClose.add(msg.getMessageId());
            }
        }
    }

    private static String SAMPLE_USAGE = "Expected 2 or 3 arguments but received: %d.\n"
            + "The program should be called with the following args: \n"
            + "1. [Device connection string] - String containing Hostname, Device Id, Module Id & Device Key in one of the following formats: HostName=<iothub_host_name>;DeviceId=<device_id>;SharedAccessKey=<device_key>;ModuleId=<module_id>\n"
            + "2. [number of requests to send]\n"
            + "3. (mqtt | amqps | amqps_ws | mqtt_ws)\n"
            + "4. (optional) path to certificate to enable one-way authentication over ssl for amqps \n";

    /**
     * Sends a number of messages to an IoT Hub. Default protocol is to 
     * use MQTT transport.
     *
     * @param args
     * args[0] = IoT Hub connection string
     * args[1] = number of requests to send
     * args[2] = protocol (optional, one of 'mqtt' or 'amqps' or 'https' or 'amqps_ws')
     * args[3] = path to certificate to enable one-way authentication over ssl for amqps (optional, default shall be used if unspecified).
     */
    public static void main(String[] args) throws IOException, URISyntaxException, ModuleClientException
    {
        System.out.println("Starting...");
        System.out.println("Beginning setup.");

        if (args.length <= 1 || args.length >= 5)
        {
            System.out.format(
                    SAMPLE_USAGE,
                    args.length);
            return;
        }

        String connString = args[0];
        int numRequests;
        String pathToCertificate = null;
        try
        {
            numRequests = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            System.out.format(
                    "Could not parse the number of requests to send. "
                            + "Expected an int but received:\n%s.\n", args[1]);
            return;
        }
        IotHubClientProtocol protocol;
        if (args.length == 2)
        {
            protocol = IotHubClientProtocol.MQTT;
        }
        else
        {
            String protocolStr = args[2];
            if (protocolStr.equals("https"))
            {
                throw new UnsupportedOperationException("ModuleClient does not support HTTPS");
            }
            else if (protocolStr.equals("amqps"))
            {
                protocol = IotHubClientProtocol.AMQPS;
            }
            else if (protocolStr.equals("mqtt"))
            {
                protocol = IotHubClientProtocol.MQTT;
            }
            else if (protocolStr.equals("amqps_ws"))
            {
                protocol = IotHubClientProtocol.AMQPS_WS;
            }
            else if (protocolStr.equals("mqtt_ws"))
            {
                protocol = IotHubClientProtocol.MQTT_WS;
            }
            else
            {
                System.out.format(
                        "Expected argument 2 to be one of 'mqtt', 'https', 'amqps' or 'amqps_ws' but received %s\n"
                                + "The program should be called with the following args: \n"
                                + "1. [Device connection string] - String containing Hostname, Device Id & Device Key in one of the following formats: HostName=<iothub_host_name>;DeviceId=<device_id>;SharedAccessKey=<device_key>\n"
                                + "2. [number of requests to send]\n"
                                + "3. (mqtt | https | amqps | amqps_ws | mqtt_ws)\n"
                                + "4. (optional) path to certificate to enable one-way authentication over ssl for amqps \n",
                        protocolStr);
                return;
            }

            if (args.length == 3)
            {
                pathToCertificate = null;
            }
            else
            {
                pathToCertificate = args[3];
            }
        }


        System.out.println("Successfully read input parameters.");
        System.out.format("Using communication protocol %s.\n", protocol.name());

        ModuleClient client = new ModuleClient(connString, protocol);
        if (pathToCertificate != null )
        {
            client.setOption("SetCertificatePath", pathToCertificate);
        }

        System.out.println("Successfully created an IoT Hub client.");

        // Set your token expiry time limit here
        long time = 2400;
        client.setOption("SetSASTokenExpiryTime", time);
        System.out.println("Updated token expiry time to " + time);

        client.open();

        System.out.println("Opened connection to IoT Hub.");
        System.out.println("Sending the following event messages:");

        String deviceId = "MyJavaDevice";
        double temperature = 0.0;
        double humidity = 0.0;

        for (int i = 0; i < numRequests; ++i)
        {
            temperature = 20 + Math.random() * 10;
            humidity = 30 + Math.random() * 20;

            String msgStr = "{\"deviceId\":\"" + deviceId +"\",\"messageId\":" + i + ",\"temperature\":"+ temperature +",\"humidity\":"+ humidity +"}";

            try
            {
                Message msg = new Message(msgStr);
                msg.setProperty("temperatureAlert", temperature > 28 ? "true" : "false");
                msg.setMessageId(java.util.UUID.randomUUID().toString());
                msg.setExpiryTime(D2C_MESSAGE_TIMEOUT);
                System.out.println(msgStr);

                EventCallback callback = new EventCallback();
                client.sendEventAsync(msg, callback, msg);
            }
            catch (Exception e)
            {
                e.printStackTrace(); // Trace the exception
            }
        }

        System.out.println("Wait for " + D2C_MESSAGE_TIMEOUT / 1000 + " second(s) for response from the IoT Hub...");

        // Wait for IoT Hub to respond.
        try
        {
            Thread.sleep(D2C_MESSAGE_TIMEOUT);
        }

        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // close the connection
        System.out.println("Closing");
        client.closeNow();

        if (!failedMessageListOnClose.isEmpty())
        {
            System.out.println("List of messages that were cancelled on close:" + failedMessageListOnClose.toString());
        }

        System.out.println("Shutting down...");
    }
}
