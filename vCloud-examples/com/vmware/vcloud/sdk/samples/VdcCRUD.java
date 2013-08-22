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

import com.vmware.vcloud.api.rest.schema.CapacityWithUsageType;
import com.vmware.vcloud.api.rest.schema.ComputeCapacityType;
import com.vmware.vcloud.api.rest.schema.CreateVdcParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.VdcStorageProfileParamsType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.admin.ProviderVdc;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.AllocationModelType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Creating, Updating and Deleting AdminVdc
 *
 * @author Administrator
 *
 */
public class VdcCRUD {

	private static VcloudClient client;

	private static VcloudAdmin admin;

	/***
	 * Returns any valid provider vdc ref
	 *
	 * @throws VCloudException
	 */
	private static ReferenceType getProviderVdcRef() throws VCloudException {
		for (ReferenceType providerVdcRef : admin.getProviderVdcRefs()) {
			if (ProviderVdc.getProviderVdcByReference(client, providerVdcRef)
					.getResource().getStatus() == 1)
				return providerVdcRef;
		}
		return null;
	}

	/**
	 * Get the capacity
	 *
	 * @param allocated
	 * @param limit
	 * @param unit
	 * @return
	 */
	private static CapacityWithUsageType getCapacity(String allocated,
			String limit, String unit) {
		CapacityWithUsageType capacityWithUsage = new CapacityWithUsageType();
		capacityWithUsage.setAllocated(Long.parseLong(allocated));
		capacityWithUsage.setLimit(Long.parseLong(limit));
		capacityWithUsage.setUnits(unit);
		return capacityWithUsage;

	}

	/**
	 * Get the CPU and Memory compute capacity
	 *
	 * @return
	 */
	private static ComputeCapacityType getComputeCapacity() {
		ComputeCapacityType computeCapacity = new ComputeCapacityType();
		computeCapacity.setCpu(getCapacity("200", "200", "MHz"));
		computeCapacity.setMemory(getCapacity("200", "200", "MB"));
		return computeCapacity;

	}

	/**
	 * Constructing a New Admin Vdc
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static CreateVdcParamsType getAdminVdc() throws VCloudException {
		CreateVdcParamsType adminVdc = new CreateVdcParamsType();
		adminVdc.setName("ee vdc");
		adminVdc.setDescription("ee vdc description");
		adminVdc.setNetworkQuota(10);
		adminVdc.setNicQuota(0);
		adminVdc.setAllocationModel(AllocationModelType.ALLOCATIONPOOL.value());
		adminVdc.setComputeCapacity(getComputeCapacity());
		adminVdc.setIsEnabled(true);
		adminVdc.setResourceGuaranteedCpu(1.0);
		adminVdc.setResourceGuaranteedMemory(1.0);
		adminVdc.setIsThinProvision(true);
		ReferenceType providerVdcRef = getProviderVdcRef();
		adminVdc.setProviderVdcReference(providerVdcRef);
		ProviderVdc providerVdc = ProviderVdc.getProviderVdcByReference(client,
				providerVdcRef);
		VdcStorageProfileParamsType vdcStorageProfile = new VdcStorageProfileParamsType();
		vdcStorageProfile.setDefault(true);
		vdcStorageProfile.setEnabled(true);
		vdcStorageProfile.setLimit(10);
		vdcStorageProfile.setUnits("MB");
				ReferenceType providerVdcStorageProfileRef = providerVdc
				.getProviderVdcStorageProfileRefs().iterator().next();
				vdcStorageProfile
				.setProviderVdcStorageProfile(providerVdcStorageProfileRef);
		adminVdc.getVdcStorageProfile().add(vdcStorageProfile);
		return adminVdc;
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
	 * @param adminVdc
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(AdminVdc adminVdc) throws VCloudException {
		TasksInProgressType tasksInProgress = adminVdc.getResource().getTasks();
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
				.println("java VdcCRUD Vcloud user@organization password OrgName");
		System.out
				.println("java VdcCRUD https://vcloud username@Organization password orgname");
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

		System.out.println("Add Vdc");
		AdminVdc adminVdc = adminOrg.createAdminVdc(getAdminVdc());
		Task task = returnTask(adminVdc);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + adminVdc.getResource().getHref() + "\n");

		System.out.println("Update Vdc");
		adminVdc.getResource().setIsEnabled(false);
		adminVdc.getResource().setStorageCapacity(getCapacity("5", "5", "MB"));
		task = adminVdc.updateAdminVdc(adminVdc.getResource());
		if (task != null)
			task.waitForTask(0);
		AdminVdc updatedAdminVdc = AdminVdc.getAdminVdcByReference(client,
				adminVdc.getReference());
		System.out
				.println("	" + updatedAdminVdc.getResource().getHref() + "\n");

		System.out.println("Delete Vdc");
		updatedAdminVdc.delete();
		System.out.println("	Deleted");

	}
}
