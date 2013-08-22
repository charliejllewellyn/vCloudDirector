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
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualNetworkCard;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * This sample demonstrates nic addition and deletion.
 *
 * @author Ecosystem Engineering
 *
 */

public class NicCRUD {

	public static VcloudClient client;

	/**
	 * Finding the vm using the org name and the vapp name
	 *
	 * @param orgName
	 * @param vAppName
	 * @param vmName
	 * @return
	 * @throws VCloudException
	 */
	private static VM findVM(String orgName, String vAppName, String vmName)
			throws VCloudException {
		Organization org = Organization.getOrganizationByReference(client,
				client.getOrgRefsByName().get(orgName));

		for (ReferenceType vdcRef : org.getVdcRefs()) {
			Vdc vdc = Vdc.getVdcByReference(client, vdcRef);
			if (vdc.getVappRefsByName().containsKey(vAppName)) {
				Vapp vapp = Vapp.getVappByReference(client, vdc
						.getVappRefByName(vAppName));
				for (VM vm : vapp.getChildrenVms()) {
					if (vm.getResource().getName().equals(vmName)) {
						System.out.println("VM Found: " + vmName + " - "
								+ vm.getReference().getHref());
						return vm;
					}
				}
			}
		}
		System.out.println("VM " + vmName + " not found");
		System.exit(0);
		return null;
	}

	/**
	 * Sample usage
	 */
	private static void usage() {
		System.out
				.println("java NicCRUD VcloudUrl Username@vcloud-oragnization Password OrganizationName vAppName vmName networkName");
		System.out
				.println("java NicCRUD https://vcloud username@Organization password orgName vappName vmName networkName");
		System.exit(0);
	}

	/**
	 * Starts here
	 *
	 * @param args
	 * @throws VCloudException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws VCloudException,
			KeyManagementException, NoSuchAlgorithmException, IOException,
			UnrecoverableKeyException, KeyStoreException, TimeoutException {

		if (args.length < 7)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);

		VM vm = findVM(args[3], args[4], args[5]);

		// getting the already existing nics.
		List<VirtualNetworkCard> nics = vm.getNetworkCards();

		// assuming that the vm does not contain any nics.
		// adding 3 more nics with nic index 0, 1, 2.
		boolean onlyOnePrimaryNetwork = true;
		for (int i = 0; i < 3; i++) {
			VirtualNetworkCard nic = new VirtualNetworkCard(i, true, args[6],
					onlyOnePrimaryNetwork, IpAddressAllocationModeType.POOL,
					"192.168.1." + i);
			nics.add(nic);
			onlyOnePrimaryNetwork = false;
		}
		try {
			vm.updateNetworkCards(nics).waitForTask(0);
		} catch (VCloudException e) {
			System.out.println("	Adding nics failed" + e.getLocalizedMessage());
			System.exit(0);
		}

		System.out.println("	New nics Added");

		nics = VM.getNetworkCards(client, vm.getReference());
		// deleting nic with ip 192.168.1.2
		for (int i = 0; i < nics.size(); i++) {
			if (nics.get(i).getIpAddress().equals("192.168.1.2"))
				nics.remove(i);
		}
		try {
			vm.updateNetworkCards(nics).waitForTask(0);
		} catch (VCloudException e) {
			System.out.println("	Deleting nic failed "
					+ e.getLocalizedMessage());
			System.exit(0);
		}
		System.out.println("	Deleted nic with ip - 192.168.1.2");

	}
}
