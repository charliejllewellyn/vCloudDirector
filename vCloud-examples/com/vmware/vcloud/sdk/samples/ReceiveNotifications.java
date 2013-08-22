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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.maas.Notification;

/**
 * This sample illustrates receiving notifications from the AMQP server.
 *
 * @author Ecosystem Engineering
 */
public class ReceiveNotifications {

	private static Connection connection;
	private static Channel channel;
	private static QueueingConsumer consumer;

	/**
	 * Bind a queue to an exchange.
	 *
	 * @throws IOException
	 */
	private static void bind(String exchange, String queue) throws IOException {
		try {
			Channel channel = connection.createChannel();
			channel.exchangeDeclarePassive(exchange);
			channel.close();
		} catch (IOException e) {
			Channel channel = connection.createChannel();
			channel.exchangeDeclare(exchange, "fanout");
			channel.close();
		}
		try {
			Channel channel = connection.createChannel();
			channel.queueDeclarePassive(queue);
			channel.close();
		} catch (IOException e) {
			Channel channel = connection.createChannel();
			channel.queueDeclare(queue, false, false, true, null);
			channel.close();
		}
		Channel channel = connection.createChannel();
		channel.queueBind(queue, exchange, "");
		channel.close();
	}

	/**
	 * Connecting to the Rabbit MQ AMQP Server.
	 *
	 * @param userName
	 * @param pwd
	 * @throws VCloudException
	 */
	private static void connect(String amqpHost, int port, String virtualHost,
			String userName, String pwd) {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(amqpHost);
		factory.setPort(port);
		factory.setUsername(userName);
		factory.setPassword(pwd);
		factory.setVirtualHost(virtualHost);
		try {
			connection = factory.newConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connection.addShutdownListener(new ShutdownListener() {
			@Override
			public void shutdownCompleted(ShutdownSignalException cause) {
				connection = null;
			}
		});
	}

	/**
	 * Disconnect from the Rabbit MQ AMQP Server.
	 */
	private static void disconnect() {
		try {
			if (connection != null) {
				channel.close();
				connection.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			channel = null;
			connection = null;
		}
	}

	/**
	 * Blocking method with timeout if time < 0 then wait till mesg is available
	 * or interrupted.
	 *
	 * @throws VCloudException
	 *
	 */
	private static Notification getNotification(String queue, long timeout)
			throws VCloudException {
		try {
			if (channel == null) {
				channel = connection.createChannel();
				channel.queueDeclarePassive(queue);
			}
			if (consumer == null) {
				consumer = new QueueingConsumer(channel);
				channel.basicConsume(queue, false, consumer);
			}
			QueueingConsumer.Delivery delivery;
			if (timeout < 0) {
				delivery = consumer.nextDelivery();
			} else {
				delivery = consumer.nextDelivery(timeout);
			}
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
	private static void purgeQueue(String queue) throws IOException {
		Channel channel = connection.createChannel();
		channel.queuePurge(queue);
		channel.close();
	}

	/**
	 * Delete the queue.
	 *
	 * @throws IOException
	 */
	private static void deleteQueue(String queue) throws IOException {
		Channel channel = connection.createChannel();
		channel.queueDelete(queue);
		channel.close();
	}

	/**
	 * Delete the Exchange.
	 *
	 * @throws IOException
	 */
	private static void deleteExchange(String exchange) throws IOException {
		Channel channel = connection.createChannel();
		channel.exchangeDelete(exchange);
		channel.close();
	}

	/**
	 * Printing the Notification Header and its Properties.
	 *
	 * @param notification
	 *            {@link Notification}
	 * @throws VCloudException
	 */
	private static void interpretNotification(Notification notification)
			throws VCloudException {
		System.out
				.println("Notification - "
						+ (notification.isBlockingTask() ? "Blocking"
								: "Non Blocking"));
		System.out.println("	Headers:");
		for (String headerName : notification.getNotificationHeaders().keySet()) {
			System.out.println("		" + headerName + " - "
					+ notification.getNotificationHeaders().get(headerName));
		}
		System.out.println("	Properties:");
		System.out.println("		Event Id - "
				+ notification.getResource().getEventId());
		System.out.println("		Timestamp - "
				+ notification.getResource().getTimestamp());
		System.out.println("		Is Blocking - " + notification.isBlockingTask());
		if (notification.isBlockingTask()) {
			System.out.println("		Blocking Task Name - "
					+ notification.getBlockingTaskLink().getName());
			System.out.println("		Blocking Task Id - "
					+ notification.getBlockingTaskLink().getId());

		} else {
			System.out.println("		Entity Type - "
					+ notification.getEntityLink().getType());
			System.out.println("		Entity Name - "
					+ notification.getEntityLink().getName());
			System.out.println("		Entity Id - "
					+ notification.getEntityLink().getId());
		}
		System.out.println("		Organization Name - "
				+ notification.getOrgLink().getName());
		System.out.println("		Organization Id - "
				+ notification.getOrgLink().getId());
		System.out.println("		User Name - "
				+ notification.getUserLink().getName());
		System.out.println("		User Id - " + notification.getUserLink().getId());

	}

	public static void main(String args[]) throws IOException {

		if (args.length < 6) {
			System.out
					.println("java ReceiveNotifications AmqpHost AmqpPort Exchange Queue VirtualHost UserName Password");
			System.out
					.println("java ReceiveNotifications hostName/Ip 5672 systemExchange systemQueue / guest guest");
			System.exit(0);
		}

		try {
			// Creating the AMQP client.
			// Connecting to it with credentials.
			connect(args[0], Integer.parseInt(args[1]), args[4], args[5],
					args[6]);
			// Binding the queue to the exchange.
			bind(args[2], args[3]);
			purgeQueue(args[3]);
			// this method receives notifications and processes it.
			while (true) {
				Notification notification = getNotification(args[3], -1);
				interpretNotification(notification);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Purge the contents of the queue.
			purgeQueue(args[3]);
			// Delete the queue.
			deleteQueue(args[3]);
			// Delete the exchange.
			deleteExchange(args[2]);
			disconnect();
		}
	}
}
