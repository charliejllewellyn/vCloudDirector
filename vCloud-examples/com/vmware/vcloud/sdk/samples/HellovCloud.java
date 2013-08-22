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
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.xml.bind.JAXBElement;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.CatalogItemType;
import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Catalog;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.exception.DuplicateNameException;

/**
 *
 * Hello vCloud: A Structured REST Workflow Example, This sample uploads an ovf
 * package into vcloud as a vapptemplate.
 *
 * @author Ecosystem Engineering
 */

public class HellovCloud {

	public static VcloudClient vcloudClient;

	/**
	 * Logging in and Getting the Organization List
	 *
	 * @param vCloudURL
	 * @param username
	 * @param password
	 * @throws VCloudException
	 * @throws IOException
	 * @throws HttpException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	public static void login(String vCloudURL, String username, String password)
			throws VCloudException, HttpException, IOException,
			KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException {
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(vCloudURL, Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(username, password);
		HashMap<String, ReferenceType> organizationsMap = vcloudClient
				.getOrgRefsByName();
		if (!organizationsMap.isEmpty()) {
			System.out.println("Organizations:");
			System.out.println("-------------------------------");
			for (String organizationName : organizationsMap.keySet())
				System.out.println("	" + organizationName);
		} else {
			System.out.println("	Invalid login for user " + username);
			System.exit(0);
		}

	}

	/**
	 *To instantiate a vApp template and operate the resulting vApp, you need
	 * the object references (href values) for the catalog in which the vApp
	 * template will be entered and the vDC in which the vApp will be deployed.
	 * The Organization class implements several methods that return references
	 * to vDCs and catalogs. findVdc gets the href of the vDC named on the
	 * command line.
	 *
	 * @param vdcName
	 * @param orgName
	 * @return {@link Vdc}
	 * @throws VCloudException
	 */
	public static Vdc findVdc(String orgName, String vdcName)
			throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		System.out.println("VDC - " + vdcRef.getName());
		System.out.println("-------------------------------");
		return Vdc.getVdcByReference(vcloudClient, vdcRef);
	}

	/**
	 * findCatalogRef gets the href of the Catalog named on the command line.
	 *
	 * @param catalogName
	 * @return {@link Vdc}
	 * @throws VCloudException
	 */
	public static ReferenceType findCatalogRef(String orgName,
			String catalogName) throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType catalogRef = null;
		for (ReferenceType ref : org.getCatalogRefs()) {
			if (ref.getName().equals(catalogName))
				catalogRef = ref;
		}
		return catalogRef;
	}

	/**
	 * Instantiating the vAppTemplate. Obtain a vAppTemplateReference from the
	 * catalog, construct an InstantiateVAppTemplateParams request, and POST the
	 * request to the action/instantiateVAppTemplate link of the vDC
	 *
	 * @param vAppTemplateReference
	 * @param vdc
	 * @throws VCloudException
	 */
	public static Vapp newvAppFromTemplate(ReferenceType vAppTemplateReference,
			Vdc vdc) throws VCloudException {
		System.out.println("Instantiating " + vAppTemplateReference.getName());
		System.out.println("-------------------------------");
		//
		// get the href of the OrgNetwork to which we can connect the vApp
		// network
		//
		NetworkConfigurationType networkConfiguration = new NetworkConfigurationType();
		if (vdc.getAvailableNetworkRefs().size() == 0) {
			System.out.println("No Networks in vdc to instantiate the vapp");
			System.exit(0);
		}

		//
		// specify the NetworkConfiguration for the vApp network
		//
		networkConfiguration.setParentNetwork(vdc.getAvailableNetworkRefs()
				.iterator().next());
		networkConfiguration.setFenceMode(FenceModeValuesType.BRIDGED
				.value());

		VAppNetworkConfigurationType vAppNetworkConfiguration = new VAppNetworkConfigurationType();
		vAppNetworkConfiguration.setConfiguration(networkConfiguration);
		vAppNetworkConfiguration.setNetworkName(vdc
				.getAvailableNetworkRefs().iterator().next().getName());

		//
		// fill in the NetworkConfigSection
		//
		NetworkConfigSectionType networkConfigSection = new NetworkConfigSectionType();
		MsgType networkInfo = new MsgType();
		networkConfigSection.setInfo(networkInfo);
		List<VAppNetworkConfigurationType> vAppNetworkConfigs = networkConfigSection
				.getNetworkConfig();
		vAppNetworkConfigs.add(vAppNetworkConfiguration);

		//
		// fill in remaining InstantititonParams (name, Source)
		//
		InstantiationParamsType instantiationParams = new InstantiationParamsType();
		List<JAXBElement<? extends SectionType>> sections = instantiationParams
				.getSection();
		sections.add(new ObjectFactory()
				.createNetworkConfigSection(networkConfigSection));

		//
		// create the request body (InstantiateVAppTemplateParams)
		//
		InstantiateVAppTemplateParamsType instVappTemplParams = new InstantiateVAppTemplateParamsType();
		instVappTemplParams.setName("HellovCloudvApp");
		instVappTemplParams.setSource(vAppTemplateReference);
		instVappTemplParams.setInstantiationParams(instantiationParams);

		//
		// make the request, and get an href to the vApp in return
		//
		Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParams);
		return vapp;

	}

	/**
	 * Configuring all the vms in the vapp to static ip addressing mode.
	 *
	 * @param vappRef
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	public static void configureVMsIPAddressingMode(ReferenceType vappRef,
			Vdc vdc) throws VCloudException, TimeoutException {
		System.out.println("	Configuring VM Ip Addressing Mode");
		Vapp vapp = Vapp.getVappByReference(vcloudClient, vappRef);
		List<VM> childVms = vapp.getChildrenVms();
		for (VM childVm : childVms) {
			NetworkConnectionSectionType networkConnectionSection = childVm
					.getNetworkConnectionSection();
			List<NetworkConnectionType> networkConnections = networkConnectionSection
					.getNetworkConnection();
			for (NetworkConnectionType networkConnection : networkConnections) {
				networkConnection
						.setIpAddressAllocationMode(IpAddressAllocationModeType.POOL
								.value());
				networkConnection.setNetwork(vdc.getAvailableNetworkRefs()
						.iterator().next().getName());
			}
			childVm.updateSection(networkConnectionSection).waitForTask(0);
			for (String ip : VM.getVMByReference(vcloudClient,
					childVm.getReference()).getIpAddressesById().values()) {
				System.out.println("		" + ip);
			}
		}

	}

	/**
	 * Create a new catalog item type with the specified vapp template reference
	 *
	 * @param vAppTemplatereference
	 *            {@link ReferenceType}
	 * @return {@link CatalogItemType}
	 */
	public static CatalogItemType createNewCatalogItem(
			ReferenceType vAppTemplatereference) {
		CatalogItemType catalogItem = new CatalogItemType();
		catalogItem.setName("HellovCloud-VappTemplate");
		catalogItem.setName("HellovCloud-VappTemplate");
		catalogItem.setEntity(vAppTemplatereference);
		return catalogItem;

	}

	/**
	 * HellovCloud Program Usage
	 */
	public static void getUsage() {
		System.out
				.println("java HellovCloud vCloudURL user@vcloud-organization password orgName vdcName ovfFileLocation catalogName");
		System.out
				.println("java HellovCloud https://vcloud user@vcloud-organization password orgName vdcName ovfFileLocation catalogName");
		System.exit(0);
	}

	/**
	 * Hello vCloud: A Structured REST Workflow Example
	 *
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws HttpException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws TimeoutException
	 * @throws DuplicateNameException 
	 */

	public static void main(String args[]) throws VCloudException,
			InterruptedException, HttpException, IOException,
			KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException, TimeoutException, DuplicateNameException {

		// Sample usage
		if (args.length < 7)
			HellovCloud.getUsage();

		// Logging in and Getting the Organization List
		HellovCloud.login(args[0], args[1], args[2]);

		// Finding a vdc
		Vdc vdc = HellovCloud.findVdc(args[3], args[4]);

		// fetch catalog
		Catalog catalog = Catalog.getCatalogByReference(vcloudClient,
				findCatalogRef(args[3], args[6]));
		
		// Upload the ovf package as vapptemplate.
		VappTemplate hellovCloudTemplate = vdc.uploadVappTemplate(
				"HellovCloud-VappTemplate", "HellovCloud-VappTemplate",
				args[5], false, vdc.getVdcStorageProfileRefs().iterator().next()
				,catalog.getReference());
		System.out.println("Uploaded vAppTemplate - "
				+ hellovCloudTemplate.getReference().getName());
		System.out.println("-------------------------------");

		// Instantiating the vAppTemplate
		Vapp vapp = HellovCloud.newvAppFromTemplate(hellovCloudTemplate
				.getReference(), vdc);
		List<Task> tasks = vapp.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);

		// chaning all the vms ip addressing mode to static ip pool
		configureVMsIPAddressingMode(vapp.getReference(), vdc);

		String vappName = vapp.getReference().getName();

		// Deploying the Instantiated vApp
		System.out.println("Deploying the " + vappName);
		System.out.println("-------------------------------");
		vapp.deploy(false, 1000000, false).waitForTask(0);

		// Powering on the vApp
		System.out.println("PowerOn the " + vappName);
		System.out.println("-------------------------------");
		vapp.powerOn().waitForTask(0);

		// Suspend the vApp
		System.out.println("Suspend the " + vappName);
		System.out.println("-------------------------------");
		vapp.suspend().waitForTask(0);

		// Powering on the vApp
		System.out.println("PowerOn the " + vappName);
		System.out.println("-------------------------------");
		vapp.powerOn().waitForTask(0);

		// Powering off the vApp
		System.out.println("PowerOff the " + vappName);
		System.out.println("-------------------------------");
		vapp.powerOff().waitForTask(0);

		// Undeploy vApp
		System.out.println("Undeploy the " + vappName);
		System.out.println("-------------------------------");
		vapp.undeploy(UndeployPowerActionType.FORCE).waitForTask(0);

		// Deleting vApp
		System.out.println("Delete the " + vappName);
		System.out.println("-------------------------------");
		vapp.delete().waitForTask(0);

	}
}
