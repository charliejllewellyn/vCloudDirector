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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.UserType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.User;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Adding, Updating and Deleting User
 *
 * @author Administrator
 *
 */
public class UserCRUD {

	private static VcloudClient client;

	private static VcloudAdmin admin;

	/**
	 * Getting any Role reference Type
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static ReferenceType getRole() throws VCloudException {
		for (ReferenceType roleRef : admin.getRoleRefs())
			return roleRef;
		return null;
	}

	/**
	 * Constructing a New User
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static UserType getUser() throws VCloudException {
		UserType user = new UserType();
		user.setName("eeuser");
		user.setRole(getRole());
		user.setPassword("ee user");
		user.setEmailAddress("ee@ee.com");
		return user;
	}

	/**
	 * Search the admin organization
	 *
	 * @param orgName
	 * @return
	 * @throws VCloudException
	 */
	private static AdminOrganization searchAdminOrg(String orgName)
			throws VCloudException {
		ReferenceType orgRef = admin.getAdminOrgRefsByName().get(orgName);
		return AdminOrganization.getAdminOrgByReference(client, orgRef);
	}

	/**
	 * Check for tasks if any
	 *
	 * @param user
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(User user) throws VCloudException {
		TasksInProgressType tasksInProgress = user.getResource().getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Sample Usage
	 */
	public static void usage() {
		System.out
				.println("java UserCRUD VcloudUrl user@organization password OrgName");
		System.out
				.println("java UserCRUD https://vcloud user@organization password orgName");
		System.exit(0);
	}

	/**
	 * Main method, which does Adding, Updating and Deleting User
	 *
	 * @param args
	 * @throws HttpException
	 * @throws SecurityException
	 * @throws FileNotFoundException
	 * @throws VCloudException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws HttpException,
			SecurityException, FileNotFoundException, VCloudException,
			IOException, KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException, TimeoutException {

		if (args.length < 4)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud Login");
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);
		System.out.println("	Login Success\n");

		System.out.println("Get Vcloud Admin");
		admin = client.getVcloudAdmin();
		System.out.println("	" + admin.getResource().getHref() + "\n");

		System.out.println("Get Admin Organization");
		AdminOrganization adminOrg = searchAdminOrg(args[3]);
		System.out.println("	" + adminOrg.getResource().getHref() + "\n");

		System.out.println("Add User");
		User user = adminOrg.createUser(getUser());
		Task task = returnTask(user);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + user.getResource().getHref() + "\n");

		System.out.println("Update User");
		User updatedUser = user.updateUser(user.getResource());
		task = returnTask(updatedUser);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + updatedUser.getResource().getHref() + "\n");

		System.out.println("Delete User");
		updatedUser.delete();
		System.out.println("	Deleted");

	}
}
