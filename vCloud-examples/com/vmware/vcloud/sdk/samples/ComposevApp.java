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

import com.vmware.vcloud.api.rest.schema.ComposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NetworkAssignmentType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.Version;

/*
 * Composing a vApp from a vAppTemplate.
 * The vAppTemplate vm is added 3 times to the composition with different names.
 * The resulting vApp would contain 3 vms.
 *
 */

public class ComposevApp {

	public static VcloudClient vcloudClient;

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
			if (vappTemplateRef.getName().equals(vappTemplateName)) {
				System.out.println("Vapp Template: " + vappTemplateName);
				return vappTemplateRef;
			}
		return null;
	}

	/**
	 * Create the compose vapp params. Creating compose vapp params containing
	 * the vapp templates vms. The same vm is added 3 times with different
	 * names.
	 *
	 * @param vappTemplateRef
	 * @param vdc
	 * @return
	 * @throws VCloudException
	 */
	public static ComposeVAppParamsType createComposeParams(
			ReferenceType vappTemplateRef, Vdc vdc) throws VCloudException {

		// name of the vapp network. this vapp network gets mapped to the
		// organization network.
		String newvAppNetwork = "vApp Network Name";

		// This InstantiationParams element that applies to the composed vApp
		// itself and any vApp templates referenced in Item elements
		NetworkConfigurationType networkConfigurationType = new NetworkConfigurationType();
		if (vdc.getAvailableNetworkRefs().size() == 0) {
			System.out.println("No Networks in vdc to compose vapp");
			System.exit(0);
		}

		networkConfigurationType.setParentNetwork(vdc.getAvailableNetworkRefs()
				.iterator().next());
		networkConfigurationType.setFenceMode(FenceModeValuesType.NATROUTED
				.value());

		VAppNetworkConfigurationType vAppNetworkConfigurationType = new VAppNetworkConfigurationType();
		vAppNetworkConfigurationType.setConfiguration(networkConfigurationType);
		vAppNetworkConfigurationType.setNetworkName(newvAppNetwork);

		NetworkConfigSectionType networkConfigSectionType = new NetworkConfigSectionType();
		MsgType networkInfo = new MsgType();
		networkConfigSectionType.setInfo(networkInfo);
		List<VAppNetworkConfigurationType> vAppNetworkConfigs = networkConfigSectionType
				.getNetworkConfig();
		vAppNetworkConfigs.add(vAppNetworkConfigurationType);

		InstantiationParamsType vappOrvAppTemplateInstantiationParamsType = new InstantiationParamsType();
		List<JAXBElement<? extends SectionType>> vappSections = vappOrvAppTemplateInstantiationParamsType
				.getSection();
		vappSections.add(new ObjectFactory()
				.createNetworkConfigSection(networkConfigSectionType));

		ComposeVAppParamsType composeVAppParamsType = new ComposeVAppParamsType();
		composeVAppParamsType.setDeploy(false);
		composeVAppParamsType
				.setInstantiationParams(vappOrvAppTemplateInstantiationParamsType);
		composeVAppParamsType.setName("ComposedVapp-EE");
		List<SourcedCompositionItemParamType> items = composeVAppParamsType
				.getSourcedItem();

		// getting the vapptemplates first vm.
		VappTemplate vappTemplate = VappTemplate.getVappTemplateByReference(
				vcloudClient, vappTemplateRef);
		VappTemplate vm = vappTemplate.getChildren().get(0);
		String vmHref = vm.getReference().getHref();

		// adding 3 vm items with different names.
		for (int i = 0; i < 3; i++) {
			SourcedCompositionItemParamType vappTemplateItem = new SourcedCompositionItemParamType();
			ReferenceType vappTemplateVMRef = new ReferenceType();
			vappTemplateVMRef.setHref(vmHref);
			vappTemplateVMRef.setName(i + "-" + vappTemplateRef.getName());
			vappTemplateItem.setSource(vappTemplateVMRef);

			// When a vApp includes Vm elements that connect to networks with
			// different names, you can use a NetworkAssignment element to
			// assign the network connection for each Vm to a specific network
			// in the parent vApp
			if (vm.getNetworkConnectionSection().getNetworkConnection().size() > 0) {
				for (NetworkConnectionType networkConnection : vm
						.getNetworkConnectionSection().getNetworkConnection()) {
					if (networkConnection.getNetworkConnectionIndex() == vm
							.getNetworkConnectionSection()
							.getPrimaryNetworkConnectionIndex()) {
						NetworkAssignmentType networkAssignment = new NetworkAssignmentType();
						networkAssignment.setInnerNetwork(networkConnection
								.getNetwork());
						networkAssignment.setContainerNetwork(newvAppNetwork);
						List<NetworkAssignmentType> networkAssignments = vappTemplateItem
								.getNetworkAssignment();
						networkAssignments.add(networkAssignment);
					}
				}
			}
			// If the vApp's Vm elements does not contain any network
			// connections. The network connection settings can be edited and
			// updated with the network on which you want the Vm's to connect
			// to.
			else {

				NetworkConnectionSectionType networkConnectionSectionType = new NetworkConnectionSectionType();
				networkConnectionSectionType.setInfo(networkInfo);

				NetworkConnectionType networkConnectionType = new NetworkConnectionType();
				networkConnectionType.setNetwork(newvAppNetwork);
				networkConnectionType
						.setIpAddressAllocationMode(IpAddressAllocationModeType.DHCP
								.value());
				networkConnectionSectionType.getNetworkConnection().add(
						networkConnectionType);

				InstantiationParamsType vmInstantiationParamsType = new InstantiationParamsType();
				List<JAXBElement<? extends SectionType>> vmSections = vmInstantiationParamsType
						.getSection();
				vmSections
						.add(new ObjectFactory()
								.createNetworkConnectionSection(networkConnectionSectionType));
				vappTemplateItem
						.setInstantiationParams(vmInstantiationParamsType);
			}

			items.add(vappTemplateItem);
		}

		return composeVAppParamsType;

	}

	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, TimeoutException,
			UnrecoverableKeyException, KeyStoreException {

		if (args.length < 6) {
			System.out
					.println("java ComposevApp vCloudURL user@organization password orgname vdcname vapptemplatename");
			System.out
					.println("java ComposevApp https://vcloud user@Organization password orgname vdcname vapptemplatename");
			System.exit(0);
		}

		// Client login
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(args[0], Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(args[1], args[2]);

		// find the vdc ref
		Vdc vdc = findVdc(args[3], args[4]);

		// find the vapp template ref
		ReferenceType vappTemplateRef = findVappTemplateRef(args[3], args[4],
				args[5]);

		// composed vapp. the composed vapp contains 3 vms from the same
		// vapptemplate.
		Vapp vapp = vdc.composeVapp(createComposeParams(vappTemplateRef, vdc));
		System.out.println("Composing vApp : " + vapp.getResource().getName());
		List<Task> tasks = vapp.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);

		// refresh the vapp
		vapp = Vapp.getVappByReference(vcloudClient, vapp.getReference());

	}
}
