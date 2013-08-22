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

import com.vmware.vcloud.api.rest.schema.AdminOrgType;
import com.vmware.vcloud.api.rest.schema.OrgEmailSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgGeneralSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgLeaseSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgSettingsType;
import com.vmware.vcloud.api.rest.schema.SmtpServerSettingsType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Creating, Updating and Deleting Organization
 *
 * @author Administrator
 *
 */
public class OrganizationCRUD {

	private static VcloudClient client;

	private static VcloudAdmin admin;

	/**
	 * Creates a new admin org type object
	 *
	 */
	private static AdminOrgType createNewAdminOrgType() {

		SmtpServerSettingsType smtpServerSettings = new SmtpServerSettingsType();
		smtpServerSettings.setHost("custom");
		smtpServerSettings.setIsUseAuthentication(true);
		smtpServerSettings.setPassword("custom");
		smtpServerSettings.setUsername("custom");

		OrgEmailSettingsType orgEmailSettings = new OrgEmailSettingsType();
		orgEmailSettings.setIsDefaultOrgEmail(true);
		orgEmailSettings.setIsDefaultSmtpServer(true);
		orgEmailSettings.setFromEmailAddress("custom@custom.com");
		orgEmailSettings.setDefaultSubjectPrefix("");
		orgEmailSettings.setSmtpServerSettings(smtpServerSettings);

		OrgLeaseSettingsType orgLeaseSettings = new OrgLeaseSettingsType();
		orgLeaseSettings.setDeleteOnStorageLeaseExpiration(false);
		orgLeaseSettings.setDeploymentLeaseSeconds(1000000);
		orgLeaseSettings.setStorageLeaseSeconds(1000000);

		OrgSettingsType orgSettings = new OrgSettingsType();

		OrgGeneralSettingsType orgGeneralSettings = new OrgGeneralSettingsType();
		orgGeneralSettings.setStoredVmQuota(0);
		orgGeneralSettings.setDeployedVMQuota(0);
		orgGeneralSettings.setCanPublishCatalogs(true);

		orgSettings.setOrgGeneralSettings(orgGeneralSettings);
		orgSettings.setVAppLeaseSettings(orgLeaseSettings);
		orgSettings.setOrgEmailSettings(orgEmailSettings);

		AdminOrgType adminOrg = new AdminOrgType();
		adminOrg.setName("CustomAdminOrg");
		adminOrg.setDescription("Custom Admin Org Desc");
		adminOrg.setFullName("Custom Admin Org Full Name");
		adminOrg.setSettings(orgSettings);
		adminOrg.setIsEnabled(true);
		return adminOrg;
	}

	/**
	 * Check for tasks if any
	 *
	 * @param adminOrg
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(AdminOrganization adminOrg)
			throws VCloudException {
		TasksInProgressType tasksInProgress = adminOrg.getResource().getTasks();
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
				.println("java OrganizationCRUD VcloudUrl Username@vcloud-organization Password");
		System.out
				.println("java OrganizationCRUD https://vcloud user@Organization password");
		System.exit(0);
	}

	/**
	 * Main method, which does Adding, Updating and Deleting Vdc
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

		System.out.println("Add New Organization");
		AdminOrganization adminOrg = admin
				.createAdminOrg(createNewAdminOrgType());
		Task task = returnTask(adminOrg);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + adminOrg.getResource().getName());
		System.out.println("	" + adminOrg.getResource().getHref() + "\n");

		System.out.println("Update Organization to Disabled");
		adminOrg.getResource().setIsEnabled(false);
		adminOrg.updateAdminOrg(adminOrg.getResource());
		task = returnTask(adminOrg);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	Updated\n");

		System.out.println("Delete Organization");
		adminOrg.delete();
		System.out.println("	Deleted\n");
	}
}
