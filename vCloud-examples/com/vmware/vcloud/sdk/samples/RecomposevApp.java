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

import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NetworkAssignmentType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.Version;

/*
 * Recompose vApp. This samples recomposes a vApp by adding a new VM to it.
 *
 */

public class RecomposevApp {

	public static VcloudClient vcloudClient;

	/**
	 * Search the vapp reference.
	 *
	 * @return {@link ReferenceType}
	 * @throws VCloudException
	 */
	public static ReferenceType findVappRef(String orgName, String vdcName,
			String vappName) throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);
		for (ReferenceType vappRef : vdc.getVappRefs())
			if (vappRef.getName().equals(vappName)) {
				System.out.println("To Be Recomposed Vapp : " + vappName);
				return vappRef;
			}
		return null;
	}

	/**
	 * Search the vm reference.
	 *
	 * @return {@link ReferenceType}
	 * @throws VCloudException
	 */
	public static ReferenceType findVmRef(String orgName, String vdcName,
			String vappName, String vmName) throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);
		for (ReferenceType vappRef : vdc.getVappRefs())
			if (vappRef.getName().equals(vappName)) {
				System.out.println("Vapp Containing, To Be Added VM : "
						+ vappName);
				for (VM vm : Vapp.getVappByReference(vcloudClient, vappRef)
						.getChildrenVms()) {
					if (vm.getResource().getName().equals(vmName)) {
						System.out.println("	To Be Added VM : " + vmName);
						return vm.getReference();
					}
				}
			}
		return null;
	}

	/**
	 * Create the recompose vapp params.
	 *
	 * @param vappTemplateRef
	 * @param vdc
	 * @return
	 * @throws VCloudException
	 */
	public static RecomposeVAppParamsType createRecomposeParams(
			ReferenceType vappRef, ReferenceType vmRef) throws VCloudException {

		// get the to be recomposed vapp reference.
		Vapp vapp = Vapp.getVappByReference(vcloudClient, vappRef);
		// find out if the vapp contains any configured network. if any, takes
		// the first configured network. the to be added vm's network could be
		// mapped to this network
		String vappNetworkName = null;
		if (vapp.getNetworkConfigSection().getNetworkConfig().size() > 0) {
			vappNetworkName = vapp.getNetworkConfigSection().getNetworkConfig()
					.get(0).getNetworkName();
		}

		// get the to be added vm reference.
		VM vm = VM.getVMByReference(vcloudClient, vmRef);

		// creating an item element. this item will contain the vm which should
		// be added to the vapp.
		SourcedCompositionItemParamType vmItem = new SourcedCompositionItemParamType();
		vmItem.setSource(vmRef);

		// if the vm contains a network connection and if the vapp does not
		// contain any configured network. the vm's network connection should be
		// removed inorder to be attached to the vapp.
		if (vm.getNetworkConnectionSection().getNetworkConnection().size() > 0
				&& vappNetworkName == null) {
			// creating empty network connection section for the vm.
			NetworkConnectionSectionType networkConnectionSectionType = new NetworkConnectionSectionType();
			MsgType networkInfo = new MsgType();
			networkConnectionSectionType.setInfo(networkInfo);

			// adding the network connection section to the instantiation params
			// of the vapp.
			InstantiationParamsType vmInstantiationParamsType = new InstantiationParamsType();
			vmInstantiationParamsType
					.getSection()
					.add(
							new ObjectFactory()
									.createNetworkConnectionSection(networkConnectionSectionType));
			vmItem.setInstantiationParams(vmInstantiationParamsType);
		}

		// if the vm already contains a network connection section and if the
		// vapp contains a configured network. the vm could be mapped to that
		// network.
		if (vm.getNetworkConnectionSection().getNetworkConnection().size() > 0
				&& vappNetworkName != null) {
			for (NetworkConnectionType networkConnection : vm
					.getNetworkConnectionSection().getNetworkConnection()) {
				if (networkConnection.getNetworkConnectionIndex() == vm
						.getNetworkConnectionSection()
						.getPrimaryNetworkConnectionIndex()) {
					NetworkAssignmentType networkAssignment = new NetworkAssignmentType();
					networkAssignment.setInnerNetwork(networkConnection
							.getNetwork());
					networkAssignment.setContainerNetwork(vappNetworkName);
					List<NetworkAssignmentType> networkAssignments = vmItem
							.getNetworkAssignment();
					networkAssignments.add(networkAssignment);
				}
			}
		}

		// if the vm does not contain any network connection sections and if the
		// vapp contains a network configuration. we should add the vm to this
		// vapp network
		if (vm.getNetworkConnectionSection().getNetworkConnection().size() == 0
				&& vappNetworkName != null) {
			NetworkConnectionSectionType networkConnectionSectionType = new NetworkConnectionSectionType();
			MsgType networkInfo = new MsgType();
			networkConnectionSectionType.setInfo(networkInfo);

			NetworkConnectionType networkConnectionType = new NetworkConnectionType();
			networkConnectionType.setNetwork(vappNetworkName);
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
			vmItem.setInstantiationParams(vmInstantiationParamsType);
		}

		// create the recompose vapp params type.
		RecomposeVAppParamsType recomposeVAppParamsType = new RecomposeVAppParamsType();
		recomposeVAppParamsType.setName("RecomposedVapp");

		// adding the vm item.
		List<SourcedCompositionItemParamType> newItems = recomposeVAppParamsType
				.getSourcedItem();
		newItems.add(vmItem);

		return recomposeVAppParamsType;

	}

	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, UnrecoverableKeyException,
			KeyStoreException, TimeoutException {

		if (args.length < 9) {
			System.out
					.println("java ComposevApp vCloudURL user@organization password orgname vdcname vdcName2 vappName vappName vmName ");
			System.out
					.println("java ComposevApp https://vcloud user@Organization password orgname vdcname2(whereTheToBeRecomposedVappExists) vdcName(whereTheToBeAddedVmExists) toBeRecomposedvAppName vAppNameContainingToBeAddedVm vmName");
			System.exit(0);
		}

		// Client login
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(args[0], Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(args[1], args[2]);

		// find the to be recomposed vapp ref
		ReferenceType vappRef = findVappRef(args[3], args[4], args[6]);

		// find the vm ref which needs to be added to the vapp
		ReferenceType vmRef = findVmRef(args[3], args[5], args[7], args[8]);

		// get the vapp ref
		Vapp toBeRecomposedvApp = Vapp
				.getVappByReference(vcloudClient, vappRef);

		System.out.println();
		System.out.println("Recompose vApp : " + vappRef.getName());

		// recompose vapp. adding a new vm to the vapp.

		toBeRecomposedvApp.recomposeVapp(createRecomposeParams(vappRef, vmRef))
				.waitForTask(0);

		// refresh the vapp
		toBeRecomposedvApp = Vapp.getVappByReference(vcloudClient, vappRef);

		System.out.println("vApp Recomposed : "
				+ toBeRecomposedvApp.getReference().getName());

	}
}
