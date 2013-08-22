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

import com.vmware.vcloud.api.rest.schema.extension.ImportVmAsVAppTemplateParamsType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.admin.extensions.VMWVimServer;

/**
 * This sample imports the given vm from vsphere into the cloud as a
 * vapptemplate.
 *
 * @author Ecosystem Engineering
 *
 */

public class ImportVmAsvAppTemplate {

	static VcloudClient client;

	/**
	 * Building the import vapp template params.
	 *
	 * @param vmName
	 * @param org
	 * @param vdc
	 * @param vimServer
	 * @return
	 * @throws VCloudException
	 */
	private static ImportVmAsVAppTemplateParamsType buildImportVappTemplateParams(
			String vmName, Organization org, Vdc vdc, VMWVimServer vimServer)
			throws VCloudException {

		ImportVmAsVAppTemplateParamsType importVmAsVAppTemplateParamsType = new ImportVmAsVAppTemplateParamsType();
		importVmAsVAppTemplateParamsType.setName("imported vapp template");
		importVmAsVAppTemplateParamsType.setSourceMove(false);
		importVmAsVAppTemplateParamsType.setVmMoRef(vimServer.getVms().get(
				vmName).getMoRef());
		importVmAsVAppTemplateParamsType.setVdc(vdc.getReference());
		return importVmAsVAppTemplateParamsType;

	}

	public static void main(String args[]) throws VCloudException,
			KeyManagementException, NoSuchAlgorithmException, IOException,
			InterruptedException, TimeoutException, UnrecoverableKeyException,
			KeyStoreException {

		if (args.length < 7) {
			System.out
					.println("java ImportVmAsvAppTemplate vCloudURL user@organization password orgname vdcname vspherename vmname");
			System.out
					.println("java ImportVmAsvAppTemplate https://vcloud user@Organizaiton password orgname vdcname vspherename vmname");
			System.exit(0);
		}

		// api login
		VcloudClient.setLogLevel(Level.OFF);
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);

		// getting to the extension api. getting org, vdc, vimserver etc
		VcloudAdminExtension extension = client.getVcloudAdminExtension();

		Organization org = Organization.getOrganizationByReference(client,
				client.getOrgRefsByName().get(args[3]));
		System.out.println("Organization: " + org.getResource().getName());
		System.out.println("--------------------------------");

		Vdc vdc = Vdc.getVdcByReference(client, org.getVdcRefByName(args[4]));
		System.out.println("Vdc: " + vdc.getResource().getName());
		System.out.println("--------------------------------");

		VMWVimServer vimServer = VMWVimServer.getVMWVimServerByReference(
				client, extension.getVMWVimServerRefsByName().get(args[5]));
		System.out.println("vSphere: " + vimServer.getResource().getName());
		System.out.println("--------------------------------");
		System.out.println("VM Name: " + args[6]);
		System.out.println("--------------------------------");

		// build the import vapptemplate params
		ImportVmAsVAppTemplateParamsType vmAsvAppTemplate = buildImportVappTemplateParams(
				args[6], org, vdc, vimServer);

		// actual import vapptemplate
		System.out.println("Importing the VM As VappTemplate");
		VappTemplate vappTemplate = vimServer
				.importVmAsVAppTemplate(vmAsvAppTemplate);
		List<Task> tasks = vappTemplate.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);
		System.out.println("	Created VappTemplate: "
				+ vappTemplate.getResource().getName());

		// Since the task is complete refresh the client object to reflect the
		// server object state.
		vappTemplate = VappTemplate.getVappTemplateByReference(client,
				vappTemplate.getReference());

	}
}
