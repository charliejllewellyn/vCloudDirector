/*
 * *******************************************************
 * Copyright VMware, Inc. 2010-2012.  All Rights Reserved.
 * *******************************************************
 *
 * DISCLAIMER. THIS PROGRAM IS PROVIDED TO YOU "AS IS" WITHOUT
 * WARRANTIES OR CONDITIONS # OF ANY KIND, WHETHER ORAL OR WRITTEN,
 * EXPRESS OR IMPLIED. THE AUTHOR SPECIFICALLY # DISCLAIMS ANY IMPLIED
 * WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY # QUALITY,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.vmware.vcloud.sdk.samples;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.extension.AmqpSettingsType;
import com.vmware.vcloud.sdk.OrgNetwork;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.constants.BlockingTaskOperationType;
import com.vmware.vcloud.sdk.constants.EntityType;
import com.vmware.vcloud.sdk.constants.EventType;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.maas.Notification;

public class VcloudNotification {

	// AMQP variables.
	private Connection connection;
	private Channel channel;
	private QueueingConsumer consumer;
	private static final String systemQueue = "systemQueue";
	private static final String systemExchange = "systemExchange";
	private static final String username = "guest";
	private static final String password = "guest";
	private static final int port = 5672;

	/**
	 * Bind a queue to an exchange.
	 *
	 * @throws IOException
	 */
	private void bind(String defaultExchange, String defaultQueue)
			throws IOException {

		try {
			Channel channel = connection.createChannel();
			channel.exchangeDeclarePassive(defaultExchange);
			channel.close();
		} catch (IOException e) {
			Channel channel = connection.createChannel();
			channel.exchangeDeclare(defaultExchange, "topic", true);
			channel.close();
		}
		try {
			Channel channel = connection.createChannel();
			channel.queueDeclarePassive(defaultQueue);
			channel.close();
		} catch (IOException e) {
			Channel channel = connection.createChannel();
			channel.queueDeclare(defaultQueue, true, false, false, null);
			channel.close();
		}
		Channel channel = connection.createChannel();
		channel.queueBind(defaultQueue, defaultExchange, "#");
		channel.close();

	}

	/**
	 * Connecting to the Rabbit MQ AMQP Server.
	 *
	 * @param userName
	 * @param pwd
	 * @throws IOException
	 */
	private void connect(String amqpHost, int port, String userName, String pwd)
			throws IOException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(amqpHost);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);
		try {
			connection = factory.newConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Disconnect from the Rabbit MQ AMQP Server.
	 */
	private void disconnect() {
		try {
			if (connection != null) {
				connection.close(1000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			connection = null;
		}
	}

	/**
	 * Blocking method with timeout if time < 0 then wait till mesg is available
	 * or interrupted.
	 *
	 */
	private Notification getNotification(String queue) {
		try {
			if (channel == null) {
				channel = connection.createChannel();
				channel.queueDeclarePassive(queue);
			}
			if (consumer == null) {
				consumer = new QueueingConsumer(channel);
				channel.basicConsume(queue, false, consumer);
			}
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			return Notification.getNotification(new String(delivery.getBody(),
					"UTF-8"), delivery.getProperties().getHeaders());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Purge the contents of the queue.
	 *
	 * @throws IOException
	 */
	private void purgeQueue(String queue) throws IOException {
		Channel channel = connection.createChannel();
		channel.queuePurge(queue);
		channel.close();
	}

	// vCloud SDK variables.
	private VcloudClient vcloudClient;

	/**
	 * Connect and login to the vCD.
	 *
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 * @throws VCloudException
	 */
	public void loginvCD(String vcloudName, String username, String password)
			throws KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, VCloudException {
		System.out.println("Connecting to the vCloud");
		VcloudClient.setLogLevel(Level.INFO);
		vcloudClient = new VcloudClient(vcloudName, Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(username, password);
		System.out.println("	Successfully connected to - " + vcloudName);

	}

	/**
	 * Update the AMQP settings in vCloud.
	 *
	 * @throws VCloudException
	 */
	public void updatevCloudAmqpSettings(String amqpHostName)
			throws VCloudException {
		System.out.println("Updating the AMQP Settings to the vCloud");
		AmqpSettingsType amqpSettings = new AmqpSettingsType();
		amqpSettings.setAmqpExchange(systemExchange);
		amqpSettings.setAmqpHost(amqpHostName);
		amqpSettings.setAmqpPassword(password);
		amqpSettings.setAmqpUsername(username);
		amqpSettings.setAmqpPort(port);
		amqpSettings.setAmqpVHost("/");

		vcloudClient.getVcloudAdminExtension()
				.getVcloudAdminExtensionSettings().updateAmqpSettings(
						amqpSettings);
		System.out.println("	Successfully mapped AMQP to vCloud");
	}

	/**
	 * Enable the operations which needs notifications.
	 *
	 * @throws VCloudException
	 *
	 */

	public void enableNotifications() throws VCloudException {

		System.out
				.println("Enabling vApp Compose Operation for Receiving Notifications");
		List<BlockingTaskOperationType> operations = vcloudClient
				.getVcloudAdminExtension().getVcloudAdminExtensionSettings()
				.getEnabledBlockingTaskOperations();
		if (operations.contains(BlockingTaskOperationType.VDC_COMPOSE_VAPP)) {
			operations.remove(BlockingTaskOperationType.VDC_COMPOSE_VAPP);
			vcloudClient.getVcloudAdminExtension()
					.getVcloudAdminExtensionSettings()
					.updateEnabledBlockingTaskOperations(operations);
		}
		vcloudClient.getVcloudAdminExtension()
				.getVcloudAdminExtensionSettings().setEnableNotifications(true);
		System.out.println("	Successfully enabled");

	}

	/**
	 * Connecting to the AMQP server/broker.
	 *
	 * @param amqpHostName
	 * @throws IOException
	 */
	public void connectAmqp(String amqpHostName) throws IOException {
		// Creating the AMQP client.
		System.out.println("Connecting to AMQP broker - " + amqpHostName);
		// Connecting to it with credentials.
		connect(amqpHostName, port, username, password);
		// Binding the queue to the exchange.rr
		bind(systemExchange, systemQueue);
		purgeQueue(systemQueue);
		System.out.println("	Successfully connected");

	}

	/**
	 * Receive the vapp creation notification from the AMQP broker/server.
	 *
	 *
	 * @param orgName
	 *            - Organization name filter for the notification.
	 *
	 * @return {@link String} - Newly created vmID(urn:vcloud:vm:uuid)
	 * @throws IOException
	 * @throws VCloudException
	 */
	public String receiveVmNotification(String orgName) throws IOException,
			VCloudException {
		System.out.print("Receiving Notifications For New VM Creation");
		purgeQueue(systemQueue);
		try {
			while (true) {
				Notification notification = getNotification(systemQueue);
				System.out.print(".");
				if (notification.getNotificationEventType().equals(
						EventType.VM_CREATE)
						&& notification.getOrgLink().getName().equals(orgName)
						&& notification.getEntityLinkType().equals(
								EntityType.VM)) {
					return notification.getEntityLink().getId();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			purgeQueue(systemQueue);
			// disconnect();
		}
		throw new VCloudException("VM creation notification not received");
	}

	/**
	 * This method waits until the vm with vmId is completely
	 * composed/recomposed.
	 *
	 * @param vmId
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	public void waitForVmCreation(String vmId) throws VCloudException,
			TimeoutException {
		VM vm = VM.getVMById(vcloudClient, vmId);
		VM vapp = VM
				.getVMByReference(vcloudClient, vm.getParentVappReference());
		for (Task task : vapp.getTasks()) {
			if (task.getResource().getOperationName().equals(
					BlockingTaskOperationType.VDC_COMPOSE_VAPP.value())
					|| task.getResource().getOperationName().equals(
							BlockingTaskOperationType.VDC_RECOMPOSE_VAPP
									.value())) {
				task.waitForTask(0);
			}
		}
	}

	/**
	 * Returns a valid mac address for the vm with vmId. Returns a mac address
	 * only if the vm contains a network which is directly connected to a direct
	 * org network.
	 *
	 * @param vmId
	 * @return
	 * @throws VCloudException
	 */
	public String getMacAddress(String vmId) throws VCloudException {
		VM vm = VM.getVMById(vcloudClient, vmId);
		Vapp vapp = Vapp.getVappByReference(vcloudClient, vm
				.getParentVappReference());
		List<String> vappNetworksBridgedToDirectOrgNetworks = new ArrayList<String>();
		for (VAppNetworkConfigurationType vappNetwork : vapp
				.getNetworkConfigSection().getNetworkConfig()) {
			if (vappNetwork.getConfiguration() != null
					&& vappNetwork.getConfiguration().getFenceMode().equals(
							FenceModeValuesType.BRIDGED.value())
					&& vappNetwork.getConfiguration().getParentNetwork() != null) {
				OrgNetwork orgNetwork = OrgNetwork.getOrgNetworkByReference(
						vcloudClient, vappNetwork.getConfiguration()
								.getParentNetwork());
				if (orgNetwork.getResource().getConfiguration().getFenceMode()
						.equals(FenceModeValuesType.BRIDGED.value())) {
					vappNetworksBridgedToDirectOrgNetworks.add(vappNetwork
							.getNetworkName());
				}
			}
		}
		for (NetworkConnectionType vmNetworkConnection : vm
				.getNetworkConnections()) {
			if (vappNetworksBridgedToDirectOrgNetworks
					.contains(vmNetworkConnection.getNetwork())) {
				return vmNetworkConnection.getMACAddress();
			}
		}
		throw new VCloudException(
				"The vm does not contain any MAC Address which is connected to a Directo Org Network");
	}

	/**
	 * Reconfiguring the vApp's VM IP.
	 *
	 * @param vmId
	 *            - VM id for configuring the new ip.
	 * @param macAddress
	 *            - The mac address which needs to be configured.
	 * @param newIp
	 *            - new Ip for the vm.
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	public void reconfigurevAppsVm(String vmId, String macAddress, String newIp)
			throws VCloudException, TimeoutException {
		System.out.println("Reconfiguring VM with New IP");
		VM vm = VM.getVMById(vcloudClient, vmId);

		Collection<NetworkConnectionType> connections = vm
				.getNetworkConnections();
		for (NetworkConnectionType connection : connections) {
			if (connection.getMACAddress().equals(macAddress)) {
				connection.setIpAddress(newIp);
			}
		}
		vm.updateSection(vm.getNetworkConnectionSection()).waitForTask(0);

		System.out.println("	Successfully Reconfigured");
	}

	/**
	 * Starts here.
	 *
	 * @param args
	 * @throws HttpException
	 * @throws VCloudException
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, UnrecoverableKeyException,
			KeyStoreException, TimeoutException, InterruptedException {
		if (args.length < 6) {
			System.err
					.println("java VcloudNotification https://vcloud username@organization password amqpHostName vmIp orgNameFilter");
			System.err
					.println("java VcloudNotification https://vcloud vadmin@System password 10.20.30.40 1.1.1.1 orgNameFilter");
			System.exit(0);
		}

		String vCDHost = args[0]; // https://cloud-main-be.eng.vmware.com
		String vCDUsername = args[1]; // vadmin@System
		String vCDPassword = args[2]; // akimbi
		String amqpHost = args[3]; // 10.20.140.185
		String newVmIp = args[4]; // 192.168.222.110
		String orgFilter = args[5];

		VcloudNotification vCloudCallout = new VcloudNotification();

		// 1. Connect and login to vCD.
		vCloudCallout.loginvCD(vCDHost, vCDUsername, vCDPassword);

		// 2. Update/Map the AMQP settings in vCloud.
		vCloudCallout.updatevCloudAmqpSettings(amqpHost);

		// 3. Enable the operations which needs notifications
		vCloudCallout.enableNotifications();

		// 4. Connect to AMQP server/broker.
		vCloudCallout.connectAmqp(amqpHost);

		// 5. Receive the vm creation notification message from the AMQP
		// broker/server
		String newVmId = vCloudCallout.receiveVmNotification(orgFilter);

		// 6. Waiting for the vm to get created completely.
		vCloudCallout.waitForVmCreation(newVmId);

		// 7. Get the mac address of the vm and send it to ipam.
		String macAddress = vCloudCallout.getMacAddress(newVmId);

		// 8. Reconfigure the VM IP
		vCloudCallout.reconfigurevAppsVm(newVmId, macAddress, newVmIp);

		vCloudCallout.disconnect();

	}
}
