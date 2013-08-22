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

import com.vmware.vcloud.api.rest.schema.AdminCatalogType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminCatalog;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Create, Update & Delete a Catalog
 *
 * @author Ecosystem Engineering
 *
 */
public class CatalogCRUD {

	private static VcloudClient client;

	private static VcloudAdmin admin;

	/**
	 * Constructing a New Catalog
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static AdminCatalogType getCatalog() throws VCloudException {
		AdminCatalogType catalog = new AdminCatalogType();
		catalog.setName("ee catalog");
		return catalog;
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
	 * @param adminCatalog
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(AdminCatalog adminCatalog)
			throws VCloudException {
		TasksInProgressType tasksInProgress = adminCatalog.getResource()
				.getTasks();
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
				.println("java CatalogCRUD VcloudUrl Username@vcloud-organization Password OrgName");
		System.out
				.println("java CatalogCRUD https://vcloud user@Organization password orgname");
		System.exit(0);
	}

	/**
	 * Main method, which does Adding, Updating and Deleting Catalog
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

		System.out.println("Add AdminCatalog");
		AdminCatalog adminCatalog = adminOrg.createCatalog(getCatalog());
		Task task = returnTask(adminCatalog);
		if (task != null)
			task.waitForTask(0);
		adminCatalog = AdminCatalog.getCatalogByReference(client, adminCatalog
				.getReference());
		System.out.println("	" + adminCatalog.getResource().getHref() + "\n");

		System.out.println("Update AdminCatalog");
		adminCatalog.getResource().setDescription("udpated Description");
		adminCatalog = adminCatalog.updateAdminCatalog(adminCatalog
				.getResource());
		task = returnTask(adminCatalog);
		if (task != null)
			task.waitForTask(0);
		adminCatalog = AdminCatalog.getCatalogByReference(client, adminCatalog
				.getReference());
		System.out.println("	" + adminCatalog.getResource().getHref() + "\n");

		System.out.println("Delete AdminCatalog");
		adminCatalog.delete();
		System.out.println("	Deleted");

	}
}
