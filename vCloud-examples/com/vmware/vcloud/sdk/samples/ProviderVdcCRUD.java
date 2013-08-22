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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.NetworkPoolReferencesType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.extension.ResourcePoolType;
import com.vmware.vcloud.api.rest.schema.extension.VMWProviderVdcParamsType;
import com.vmware.vcloud.api.rest.schema.extension.VMWProviderVdcType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefsType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.VMWProviderVdc;
import com.vmware.vcloud.sdk.admin.extensions.VMWVimServer;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.constants.VimObjectTypeEnum;

/**
 * Creating, Getting, Updating and Deleting ProviderVdc
 * 
 * Should be system administrator.
 * Should contain atleast a vCenter with resource pools.
 * 
 * @author Ecosystem Engineering
 */

public class ProviderVdcCRUD {

	private static VcloudClient client;

	/**
	 * ProviderVdcCRUD Usage
	 */
	public static void usage() {
		System.out
				.println("java ProviderVdcCRUD Vcloud user@organization password VimServerName ResourcePoolName StorageProfileName ProviderVdcName");
		System.out
				.println("java ProviderVdcCRUD https://vcloud username@Organization password vimservername resourcepoolname storageprofilename providervdcname");
		System.exit(0);
	}

	/**
	 * Creates ProviderVdcParam
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param resourcePool
	 *            {@link ResourcePoolType}
	 * @param storageProfile
	 *            {@link String}
	 * 
	 * @return {@link VMWProviderVdcParamsType}
	 * 
	 * @throws VCloudException
	 */
	private static VMWProviderVdcParamsType createProviderVdcParam(
			ReferenceType vimServerRef, ResourcePoolType resourcePool,
			String storageProfile, String providerVdcName)
			throws VCloudException {
		VMWProviderVdcParamsType vmwProviderVdcParams = new VMWProviderVdcParamsType();
		vmwProviderVdcParams.setName(providerVdcName);
		vmwProviderVdcParams.setDescription("ProviderVdc description");
		vmwProviderVdcParams.getVimServer().add(vimServerRef);
		vmwProviderVdcParams.setIsEnabled(true);
		vmwProviderVdcParams.getStorageProfile().add(storageProfile);
		VimObjectRefsType resRefs = new VimObjectRefsType();
		VimObjectRefType resRef = new VimObjectRefType();
		resRef.setMoRef(resourcePool.getMoRef());
		resRef.setVimObjectType(VimObjectTypeEnum.RESOURCE_POOL.value());
		resRef.setVimServerRef(vimServerRef);
		resRefs.getVimObjectRef().add(resRef);
		vmwProviderVdcParams.setResourcePoolRefs(resRefs);
		return vmwProviderVdcParams;
	}

	/**
	 * Creates params for updating ProviderVdc
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param resourcePool
	 *            {@link ResourcePoolType}
	 * 
	 * @return VMWProviderVdcType {@link VMWProviderVdcType}
	 * 
	 * @throws VCloudException
	 */
	private static VMWProviderVdcType updateProviderVdcParam(
			ReferenceType vimServerRef, ResourcePoolType resourcePool,
			String providerVdcName) throws VCloudException {
		VMWProviderVdcType vMWProviderVdc = new VMWProviderVdcType();
		vMWProviderVdc.setName(providerVdcName + "_Updated");
		vMWProviderVdc.setDescription("New description");
		vMWProviderVdc.setIsEnabled(true);
		vMWProviderVdc.setHighestSupportedHardwareVersion("vmx-07");
		vMWProviderVdc.getVimServer().add(vimServerRef);
		VimObjectRefsType resRefs = new VimObjectRefsType();
		VimObjectRefType resRef = new VimObjectRefType();
		resRef.setMoRef(resourcePool.getMoRef());
		resRef.setVimObjectType(VimObjectTypeEnum.RESOURCE_POOL.value());
		resRef.setVimServerRef(vimServerRef);
		resRefs.getVimObjectRef().add(resRef);
		vMWProviderVdc.setResourcePoolRefs(resRefs);
		NetworkPoolReferencesType networkPoolReferences = new NetworkPoolReferencesType();
		vMWProviderVdc.setNetworkPoolReferences(networkPoolReferences);
		vMWProviderVdc.setDataStoreRefs(resRefs);

		return vMWProviderVdc;
	}

	/**
	 * Check for tasks if any
	 * 
	 * @param vMWProviderVdc
	 * @return {@link Task}
	 * 
	 * @throws VCloudException
	 */
	public static Task returnTask(VMWProviderVdc vMWProviderVdc)
			throws VCloudException {
		TasksInProgressType tasksInProgress = vMWProviderVdc.getResource()
				.getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Get VimServer Reference by vimServerName
	 * 
	 * @param vimServerName
	 *            {@link String}
	 * @return {@link ReferenceType}
	 * 
	 * @throws VCloudException
	 */
	private static ReferenceType getVimServerRef(String vimServerName)
			throws VCloudException {
		return client.getVcloudAdminExtension().getVMWVimServerRefsByName()
				.get(vimServerName);
	}

	/**
	 * Get Resource Pool
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param resourcePoolName
	 *            {@link String}
	 * @return {@link ResourcePoolType}
	 * @throws VCloudException
	 */
	private static ResourcePoolType getResourcePool(ReferenceType vimServerRef,
			String resourcePoolName) throws VCloudException {
		VMWVimServer server = VMWVimServer.getVMWVimServerByReference(client,
				vimServerRef);
		for (ResourcePoolType pool : server.getResourcePools().values()) {
			if (pool.getName().equalsIgnoreCase(resourcePoolName)) {
				return pool;
			}
		}
		System.out.println("Unable to find the ResourcePool: "
				+ resourcePoolName);
		return null;
	}

	/**
	 * Main method, which does Creating, Getting, Updating and Deleting
	 * ProviderVdc
	 * 
	 * @param args
	 * 
	 * @throws VCloudException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws VCloudException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, TimeoutException {

		if (args.length < 7)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud Login");
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);

		System.out.println("	Login Success\n");

		ReferenceType vimServerRef = getVimServerRef(args[3]);
		ResourcePoolType resourcePool = getResourcePool(vimServerRef, args[4]);
		VMWProviderVdc vMWProviderVdc;
		if (resourcePool != null) {
			VMWProviderVdcParamsType vmwProviderVdcParams = createProviderVdcParam(
					vimServerRef, resourcePool, args[5], args[6]);
			System.out.println("Add VMWProviderVdc");
			vMWProviderVdc = client.getVcloudAdminExtension()
					.createVMWProviderVdc(vmwProviderVdcParams);
			Task task = returnTask(vMWProviderVdc);
			if (task != null)
				task.waitForTask(0);
			System.out.println("VMWProviderVdc added");
			System.out.println("	VMWProviderVdc:	"
					+ vMWProviderVdc.getResource().getName());

			System.out.println("Update VMWProviderVdc");
			VMWProviderVdcType vmwProviderVdc = updateProviderVdcParam(
					vimServerRef, resourcePool, args[6]);
			vMWProviderVdc = vMWProviderVdc.updateVMWProviderVdc(vmwProviderVdc);
			if(vMWProviderVdc.getTasks().size()>0){ 
				vMWProviderVdc.getTasks().get(0).waitForTask(0);
			}
			System.out.println("VMWProviderVdc Updated");
			System.out.println("	Updated VMWProviderVdc:	"
					+ vMWProviderVdc.getResource().getName());

			System.out.println("Get VMWProviderVdc");
			System.out.println("	"
					+ VMWProviderVdc.getVMWProviderVdcByReference(client,
							vMWProviderVdc.getReference()).getResource()
							.getName());

			System.out.println("Delete VMWProviderVdc");
			vMWProviderVdc.disable();
			vMWProviderVdc.delete();
			System.out.println("VMWProviderVdc deleted");
		}
	}
}
