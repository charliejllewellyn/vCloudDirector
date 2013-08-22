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

import javax.xml.bind.JAXBElement;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.GuestCustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.Version;

/*
 * Sample flow
 *
 * 1. Instantiates the given vapptemplate
 * 2. Assuming that the vapp contains only 1 vm.
 * 3. Change the guest customization params of that VM (for simplicity we have disabled guest customization, you can enable it and set the required guest customization parameters)
 * 4. Deploy the vapp
 * 5. Undeploy and delete it.
 */

public class GuestCustomization {

	public static VcloudClient vcloudClient;

	/**
	 * Search the vapp template reference. Since the vapptemplate is not unique
	 * under a vdc. This method returns the first occurance of the vapptemplate
	 * in that vdc.
	 *
	 * @return
	 * @throws VCloudException
	 */
	public static ReferenceType findVappTemplateRef(String orgName,
			String vdcName, String vappTemplateName) throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);
		for (ReferenceType vappTemplateRef : vdc.getVappTemplateRefs())
			if (vappTemplateRef.getName().equals(vappTemplateName))
				return vappTemplateRef;
		return null;
	}

	/**
	 * Finding a vdc
	 *
	 * @param vdcName
	 * @param orgName
	 * @return {@link Vdc}
	 * @throws VCloudException
	 */
	public static Vdc findVdc(String orgName, String vdcName)
			throws VCloudException {
		System.out.println("Org Name: " + orgName);
		System.out.println("--------------------");
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		System.out.println("Vdc Name: " + vdcName);
		System.out.println("--------------------");
		return Vdc.getVdcByReference(vcloudClient, vdcRef);
	}

	/**
	 * Instantiating the vAppTemplate
	 *
	 * @param vAppTemplateReference
	 * @param vdc
	 * @throws VCloudException
	 */
	public static Vapp newvAppFromTemplate(ReferenceType vAppTemplateReference,
			Vdc vdc) throws VCloudException {
		System.out.println("Instantiating VappTemplate: "
				+ vAppTemplateReference.getName());
		System.out.println("-----------------------------------");

		NetworkConfigurationType networkConfigurationType = new NetworkConfigurationType();
		if (vdc.getAvailableNetworkRefs().size() == 0) {
			System.out.println("No Networks in vdc to instantiate the vapp");
			System.exit(0);
		}

		networkConfigurationType.setParentNetwork(vdc.getAvailableNetworkRefs()
				.iterator().next());
		networkConfigurationType.setFenceMode(FenceModeValuesType.BRIDGED.value());

		VAppNetworkConfigurationType vAppNetworkConfigurationType = new VAppNetworkConfigurationType();
		vAppNetworkConfigurationType.setConfiguration(networkConfigurationType);
		vAppNetworkConfigurationType.setNetworkName(vdc
				.getAvailableNetworkRefs().iterator().next().getName());

		NetworkConfigSectionType networkConfigSectionType = new NetworkConfigSectionType();
		MsgType networkInfo = new MsgType();
		networkConfigSectionType.setInfo(networkInfo);
		List<VAppNetworkConfigurationType> vAppNetworkConfigs = networkConfigSectionType
				.getNetworkConfig();
		vAppNetworkConfigs.add(vAppNetworkConfigurationType);

		InstantiationParamsType instantiationParamsType = new InstantiationParamsType();
		List<JAXBElement<? extends SectionType>> sections = instantiationParamsType
				.getSection();
		sections.add(new ObjectFactory()
				.createNetworkConfigSection(networkConfigSectionType));

		InstantiateVAppTemplateParamsType instVappTemplParamsType = new InstantiateVAppTemplateParamsType();
		instVappTemplParamsType.setName("HellovCloudvAppp");
		instVappTemplParamsType.setSource(vAppTemplateReference);
		instVappTemplParamsType.setInstantiationParams(instantiationParamsType);

		Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParamsType);
		System.out.println("Vapp Name: " + vapp.getResource().getName());
		System.out.println("--------------------");
		return vapp;

	}

	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, UnrecoverableKeyException,
			KeyStoreException, TimeoutException {

		if (args.length < 6) {
			System.out
					.println("java GuestCustomization vCloudURL user@organization password orgname vdcname vapptemplatename");
			System.out
					.println("java GuestCustomization https://vcloud user@Organization password orgname vdcname vapptemplatename");
			System.exit(0);
		}

		// Client login
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(args[0], Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(args[1], args[2]);

		// fetch the vdc
		Vdc vdc = findVdc(args[3], args[4]);

		// fetch the vapp tempalte
		ReferenceType vappTemplateRef = findVappTemplateRef(args[3], args[4],
				args[5]);

		// instantiate the vapptemplate - vapp
		Vapp vapp = newvAppFromTemplate(vappTemplateRef, vdc);
		List<Task> tasks = vapp.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);

		// fetch the instantiated vapp
		vapp = Vapp.getVappByReference(vcloudClient, vapp.getReference());
		VM vm1 = vapp.getChildrenVms().get(0);

		// change the guest customization settings of the vm inside the vapp.
		// for simplicity purposes guest customization is disabled. you can
		// enable it and set the parameters accordingly.
		System.out
				.println("Setting the guest customization settings of the vm");
		System.out
				.println("--------------------------------------------------");
		GuestCustomizationSectionType guestCustomizationSection = vm1
				.getGuestCustomizationSection();
		guestCustomizationSection.setEnabled(false);
		vm1.updateSection(guestCustomizationSection).waitForTask(0);

		// deploy the vapp
		System.out.println("Deploying the " + vapp.getResource().getName());
		System.out.println("--------------------");
		vapp.deploy(false, 1000000, false).waitForTask(0);

		// undeploy and delete
		System.out.println("Undeploy and Delete Vapp");
		System.out.println("-------------------------");
		vapp.undeploy(UndeployPowerActionType.FORCE).waitForTask(0);
		vapp.delete().waitForTask(0);
	}
}
