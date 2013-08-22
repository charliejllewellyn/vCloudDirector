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
import java.math.BigInteger;
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
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.constants.BusSubType;
import com.vmware.vcloud.sdk.constants.BusType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * This sample demonstrates disk addition and deletion.
 *
 * @author Ecosystem Engineering
 *
 */

public class DiskCRUD {

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
				.println("java DiskCRUD VcloudUrl Username@vcloud-oragnization Password OrganizationName vAppName vmName");
		System.out
				.println("java DiskCRUD https://vcloud username@Organization password orgName vappName vmName");
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

		if (args.length < 6)
			usage();

		String deleteDiskName = "Hard disk 2";

		VcloudClient.setLogLevel(Level.OFF);
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);

		VM vm = findVM(args[3], args[4], args[5]);

		VirtualDisk lsiLogicSCSIDisk = new VirtualDisk(
				BigInteger.valueOf(2048), BusType.SCSI, BusSubType.LSI_LOGIC);

		VirtualDisk paravirtualSCSIDisk = new VirtualDisk(BigInteger
				.valueOf(2048), BusType.SCSI, BusSubType.PARA_VIRTUAL, 3, 10);

		List<VirtualDisk> disks = vm.getDisks();
		disks.add(lsiLogicSCSIDisk);
		disks.add(paravirtualSCSIDisk);
		try {
			vm.updateDisks(vm.getDisks()).waitForTask(0);
		} catch (VCloudException e) {
			System.out
					.println("	Adding disks failed" + e.getLocalizedMessage());
			System.exit(0);
		}

		System.out.println("	New Disks Added");

		disks = VM.getDisks(client, vm.getReference());
		for (int i = 0; i < disks.size(); i++) {
			if (disks.get(i).getItemResource().getElementName().getValue()
					.equals(deleteDiskName))
				disks.remove(i);
		}
		try {
			vm.updateDisks(disks).waitForTask(0);
		} catch (VCloudException e) {
			System.out.println("	Deleting disk failed"
					+ e.getLocalizedMessage());
			System.exit(0);
		}
		System.out.println("	Deleted Disk - " + deleteDiskName);

	}
}
