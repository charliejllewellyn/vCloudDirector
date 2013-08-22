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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.extension.ResourcePoolType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefType;
import com.vmware.vcloud.api.rest.schema.extension.VmObjectRefType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.VMWVimServer;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Listing all the VIMServer's (Attached vSphere's) VM's, ResourcePools,
 * Datastores.
 *
 * @author Ecosystem Engineering
 *
 */

public class ListVimServerDetails {

	private static VcloudAdminExtension extension;
	private static VcloudClient vcloudClient;

	/**
	 * Listing all the datastore details
	 *
	 * @throws VCloudException
	 */
	private static void listAllDatastores() throws VCloudException {
		// Traversing tree and printing all the Datastores
		System.out.println("Listing All Datastore's");
		System.out.println("-----------------------");
		Map<String, ReferenceType> vimServerRefsByName = extension
				.getVMWVimServerRefsByName();
		for (String vimServerName : vimServerRefsByName.keySet()) {
			System.out.println("	" + vimServerName);
			VMWVimServer vimServer = VMWVimServer.getVMWVimServerByReference(
					vcloudClient, vimServerRefsByName.get(vimServerName));
			HashMap<String, ResourcePoolType> resourcePoolsByName = vimServer
					.getResourcePools();
			for (String resourcePoolName : resourcePoolsByName.keySet()) {
				System.out.println("		" + resourcePoolName);
				List<VimObjectRefType> datastoreVimObjectRefs = resourcePoolsByName
						.get(resourcePoolName).getDataStoreRefs()
						.getVimObjectRef();
				for (VimObjectRefType datastoreVimObjectRef : datastoreVimObjectRefs) {
					System.out
							.println("			" + datastoreVimObjectRef.getMoRef());
				}
				if (datastoreVimObjectRefs.size() == 0)
					System.out.println("		No Datastores");
			}
			if (resourcePoolsByName.size() == 0)
				System.out.println("		No ResourcePools");
		}
	}

	/**
	 * Listing all the resource pool details
	 *
	 * @throws VCloudException
	 */
	private static void listAllResourcePools() throws VCloudException {
		// Traversing tree and printing all the ResourcePools
		System.out.println("Listing All ResourcePool's");
		System.out.println("--------------------------");
		Map<String, ReferenceType> vimServerRefsByName = extension
				.getVMWVimServerRefsByName();
		for (String vimServerName : vimServerRefsByName.keySet()) {
			System.out.println("	" + vimServerName);
			VMWVimServer vimServer = VMWVimServer.getVMWVimServerByReference(
					vcloudClient, vimServerRefsByName.get(vimServerName));
			HashMap<String, ResourcePoolType> resourcePoolsByName = vimServer
					.getResourcePools();
			for (String resourcePoolName : resourcePoolsByName.keySet()) {
				System.out.println("		" + resourcePoolName + " ("
						+ resourcePoolsByName.get(resourcePoolName).getMoRef()
						+ ")");
			}
			if (resourcePoolsByName.size() == 0)
				System.out.println("		No ResourcePools");
		}
	}

	/**
	 * Listing all the VM's details
	 *
	 * @throws VCloudException
	 */
	public static void listAllVMs() throws VCloudException {
		// Traversing tree and printing all the VMS
		System.out.println("Listing All VM's");
		System.out.println("----------------");
		Map<String, ReferenceType> vimServerRefsByName = extension
				.getVMWVimServerRefsByName();
		for (String vimServerName : vimServerRefsByName.keySet()) {
			System.out.println("	" + vimServerName);
			VMWVimServer vimServer = VMWVimServer.getVMWVimServerByReference(
					vcloudClient, vimServerRefsByName.get(vimServerName));
			HashMap<String, VmObjectRefType> vmsByName = vimServer.getVms();
			for (String vmName : vmsByName.keySet()) {
				System.out.println("		" + vmName + " ("
						+ vmsByName.get(vmName).getMoRef() + ")");
			}
			if (vmsByName.size() == 0)
				System.out.println("		No Vms");
		}
	}

	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, UnrecoverableKeyException,
			KeyStoreException {

		if (args.length < 3) {
			System.out
					.println("java ListVimServerDetails vCloudURL user@organization password");
			System.out
					.println("java ListVimServerDetails https://vcloud user@System password");
			System.exit(0);
		}

		// Client login
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(args[0], Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(args[1], args[2]);
		extension = vcloudClient.getVcloudAdminExtension();

		listAllVMs();

		listAllResourcePools();

		listAllDatastores();

	}

}
