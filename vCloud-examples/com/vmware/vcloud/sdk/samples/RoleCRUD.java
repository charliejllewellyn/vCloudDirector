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
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.RightReferencesType;
import com.vmware.vcloud.api.rest.schema.RoleType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.Role;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Adding, Updating and Deleting Role
 *
 * @author Administrator
 *
 */
public class RoleCRUD {

	private static VcloudClient client;

	private static VcloudAdmin admin;

	/**
	 * Getting all the rights
	 *
	 * @return
	 */
	private static RightReferencesType getRights() {
		RightReferencesType rightReferences = new RightReferencesType();
		List<ReferenceType> rights = rightReferences.getRightReference();
		for (ReferenceType rightRef : admin.getRightRefs())
			rights.add(rightRef);
		return rightReferences;
	}

	/**
	 * Constructing a new role with all the rights
	 *
	 * @return
	 */
	private static RoleType getRole() {
		RoleType role = new RoleType();
		role.setName("eerole");
		role.setDescription("ee description");
		role.setRightReferences(getRights());
		return role;
	}

	/**
	 * Check for tasks if any
	 *
	 * @param role
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(Role role) throws VCloudException {
		TasksInProgressType tasksInProgress = role.getResource().getTasks();
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
				.println("java RoleCRUD VcloudUrl Username@vcloud-oragnization Password");
		System.out
				.println("java RoleCRUD https://vcloud user@Organization password");
		System.exit(0);
	}

	/**
	 * Main method, which does Adding, Updating and Deleting Role
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
		if (args.length < 3)
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

		System.out.println("Add Role");
		Role role = admin.createRole(getRole());
		Task task = returnTask(role);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + role.getResource().getName() + "\n");

		System.out.println("Update Role");
		Role updatedRole = role.updateRole(role.getResource());
		task = returnTask(updatedRole);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + updatedRole.getResource().getName() + "\n");

		System.out.println("Delete Role");
		updatedRole.delete();
		System.out.println("	Deleted");

	}
}
