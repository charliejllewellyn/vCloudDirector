package com.vmware.vcloud.sdk.samples;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.CatalogItemType;
import com.vmware.vcloud.api.rest.schema.ComposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
import com.vmware.vcloud.api.rest.schema.UploadVAppTemplateParamsType;
import com.vmware.vcloud.sdk.Catalog;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Blank VM's Sample.
 *
 * 1. UPLOAD VAPPTEMPLATE :: Uploads the selfcontained ovf xml which contains a
 * vapp and a blank vm(1 vcpu and 512 mb) with no disks/nics.
 *
 * 2. ADD VAPP TEMPLATE TO CATALOG :: Attaches the uploaded vapptemplate to a
 * catalog.
 *
 * 3. COMPOSE NEW VAPP :: Composes a vapp (with 'n' Blank VM's) using the newly
 * uploaded vapptemplate.
 *
 * @author Ecosystem Engineering
 */

public class BlankVMs {

	private static VcloudClient vcloudClient;
	private static Vdc vdc;
	private static VappTemplate vappTemplate;
	private static Vapp vapp;

	private static String blankVmXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<ovf:Envelope xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1\" xmlns:rasd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData\" xmlns:vssd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2.22.0/CIM_VirtualSystemSettingData.xsd http://schemas.dmtf.org/ovf/envelope/1 http://schemas.dmtf.org/ovf/envelope/1/dsp8023_1.1.0.xsd http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2.22.0/CIM_ResourceAllocationSettingData.xsd\">"
			+ "    <ovf:References/>"
			+ "    <ovf:DiskSection>"
			+ "        <ovf:Info>Virtual disk information</ovf:Info>"
			+ "    </ovf:DiskSection>"
			+ "    <ovf:NetworkSection>"
			+ "        <ovf:Info>The list of logical networks</ovf:Info>"
			+ "    </ovf:NetworkSection>"
			+ "    <ovf:VirtualSystem ovf:id=\"vmname\">"
			+ "        <ovf:Info>A virtual machine: </ovf:Info>"
			+ "        <ovf:Name>vmname</ovf:Name>"
			+ "        <ovf:OperatingSystemSection xmlns:vmw=\"http://www.vmware.com/schema/ovf\" ovf:id=\"69\" vmw:osType=\"winNetStandardGuest\">"
			+ "            <ovf:Info>Specifies the operating system installed</ovf:Info>"
			+ "            <ovf:Description>Microsoft Windows Server 2003, Standard Edition (32-bit)</ovf:Description>"
			+ "        </ovf:OperatingSystemSection>"
			+ "        <ovf:VirtualHardwareSection ovf:transport=\"\">"
			+ "            <ovf:Info>Virtual hardware requirements</ovf:Info>"
			+ "            <ovf:System>"
			+ "                <vssd:ElementName>Virtual Hardware Family</vssd:ElementName>"
			+ "                <vssd:InstanceID>0</vssd:InstanceID>"
			+ "                <vssd:VirtualSystemIdentifier>vmname</vssd:VirtualSystemIdentifier>"
			+ "                <vssd:VirtualSystemType>vmx-07</vssd:VirtualSystemType>"
			+ "            </ovf:System>"
			+ "            <ovf:Item>"
			+ "                <rasd:Address>0</rasd:Address>"
			+ "                <rasd:Description>IDE Controller</rasd:Description>"
			+ "                <rasd:ElementName>IDE Controller 0</rasd:ElementName>"
			+ "                <rasd:InstanceID>1</rasd:InstanceID>"
			+ "                <rasd:ResourceType>5</rasd:ResourceType>"
			+ "            </ovf:Item>"
			+ "            <ovf:Item>"
			+ "                <rasd:AddressOnParent>0</rasd:AddressOnParent>"
			+ "                <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>"
			+ "                <rasd:Description>CD/DVD Drive</rasd:Description>"
			+ "                <rasd:ElementName>CD/DVD Drive 1</rasd:ElementName>"
			+ "                <rasd:HostResource/>"
			+ "                <rasd:InstanceID>3000</rasd:InstanceID>"
			+ "                <rasd:Parent>1</rasd:Parent>"
			+ "                <rasd:ResourceType>15</rasd:ResourceType>"
			+ "            </ovf:Item>"
			+ "            <ovf:Item>"
			+ "                <rasd:AddressOnParent>0</rasd:AddressOnParent>"
			+ "                <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>"
			+ "                <rasd:Description>Floppy Drive</rasd:Description>"
			+ "                <rasd:ElementName>Floppy Drive 1</rasd:ElementName>"
			+ "                <rasd:HostResource/>"
			+ "                <rasd:InstanceID>8000</rasd:InstanceID>"
			+ "                <rasd:ResourceType>14</rasd:ResourceType>"
			+ "            </ovf:Item>"
			+ "            <ovf:Item>"
			+ "                <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>"
			+ "                <rasd:Description>Number of Virtual CPUs</rasd:Description>"
			+ "                <rasd:ElementName>1 virtual CPU(s)</rasd:ElementName>"
			+ "                <rasd:InstanceID>2</rasd:InstanceID>"
			+ "                <rasd:Limit>256</rasd:Limit>"
			+ "                <rasd:Reservation>2</rasd:Reservation>"
			+ "                <rasd:ResourceType>3</rasd:ResourceType>"
			+ "                <rasd:VirtualQuantity>1</rasd:VirtualQuantity>"
			+ "                <rasd:Weight>1000</rasd:Weight>"
			+ "            </ovf:Item>"
			+ "            <ovf:Item>"
			+ "                <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>"
			+ "                <rasd:Description>Memory Size</rasd:Description>"
			+ "                <rasd:ElementName>512 MB of memory</rasd:ElementName>"
			+ "                <rasd:InstanceID>3</rasd:InstanceID>"
			+ "                <rasd:Limit>512</rasd:Limit>"
			+ "                <rasd:Reservation>512</rasd:Reservation>"
			+ "                <rasd:ResourceType>4</rasd:ResourceType>"
			+ "                <rasd:VirtualQuantity>512</rasd:VirtualQuantity>"
			+ "                <rasd:Weight>5120</rasd:Weight>"
			+ "            </ovf:Item>"
			+ "        </ovf:VirtualHardwareSection>"
			+ "    </ovf:VirtualSystem>" + "</ovf:Envelope>" + "";

	/**
	 * Uploading the ovf package(vapp with 1 blank vm) as a vapp template.
	 *
	 * @param vAppTemplateName
	 * @param vAppTemplateDesc
	 * @param descriptorInputStream
	 * @return {@link VappTemplate}
	 * @throws VCloudException
	 * @throws IOException
	 */
	public static VappTemplate uploadVappTemplate(String vAppTemplateName,
			String vAppTemplateDesc, InputStream descriptorInputStream)
			throws VCloudException, IOException {
		// Creating an vapptemplate with the provided name and description
		UploadVAppTemplateParamsType vappTemplParams = new UploadVAppTemplateParamsType();
		vappTemplParams.setDescription(vAppTemplateDesc);
		vappTemplParams.setName(vAppTemplateName);
		VappTemplate vappTemplate = vdc.createVappTemplate(vappTemplParams);

		vappTemplate.uploadOVFFile(descriptorInputStream, descriptorInputStream
				.available());
		vappTemplate = VappTemplate.getVappTemplateByReference(vcloudClient,
				vappTemplate.getReference());
		// waiting until the ovf descriptor uplaoded flag is true.
		while (!vappTemplate.getResource().isOvfDescriptorUploaded()) {
			vappTemplate = VappTemplate.getVappTemplateByReference(
					vcloudClient, vappTemplate.getReference());
		}
		descriptorInputStream.close();

		vappTemplate = VappTemplate.getVappTemplateByReference(vcloudClient,
				vappTemplate.getReference());
		// waiting until the vapptemplate gets resolved.
		while (vappTemplate.getResource().getStatus() != 8) {
			vappTemplate = VappTemplate.getVappTemplateByReference(
					vcloudClient, vappTemplate.getReference());
		}
		return vappTemplate;

	}

	/**
	 * Adding the newly created vapptemplate to the catalog.
	 *
	 * @param catalogRef
	 * @param vAppTemplateName
	 * @throws VCloudException
	 */
	private static void addVappTemplateToCatalog(ReferenceType catalogRef,
			String vAppTemplateName) throws VCloudException {
		if (catalogRef == null) {
			System.out.println("Catalog not found");
			System.exit(0);
		}
		CatalogItemType catalogItemType = new CatalogItemType();
		catalogItemType.setName(vAppTemplateName);
		catalogItemType.setDescription(vAppTemplateName);
		catalogItemType.setEntity(vappTemplate.getReference());
		Catalog catalog = Catalog.getCatalogByReference(vcloudClient,
				catalogRef);
		catalog.addCatalogItem(catalogItemType);
	}

	/**
	 * Creating Vapp with blank vms.
	 *
	 * @param vAppName
	 *            :: Vapp Name
	 * @param vmName
	 *            :: vm Name
	 * @param noOfBlankVms
	 *            :: no of blank vms needed in the vapp.
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void createVapp(String vAppName, String vmName,
			Integer noOfBlankVms) throws VCloudException, TimeoutException {
		ComposeVAppParamsType composeVAppParamsType = new ComposeVAppParamsType();
		composeVAppParamsType.setName(vAppName);
		composeVAppParamsType.setDescription(vAppName);
		composeVAppParamsType.setPowerOn(false);
		composeVAppParamsType.setDeploy(false);

		// adding the specified no of blank vms.
		for (int i = 0; i < noOfBlankVms; i++) {
			ReferenceType vappTemplateRef = new ReferenceType();
			vappTemplateRef.setName(vmName + "-" + i);
			vappTemplateRef.setHref(vappTemplate.getChildren().get(0)
					.getReference().getHref());
			SourcedCompositionItemParamType vappTemplateItem = new SourcedCompositionItemParamType();
			vappTemplateItem.setSource(vappTemplateRef);
			composeVAppParamsType.getSourcedItem().add(vappTemplateItem);
		}

		vapp = vdc.composeVapp(composeVAppParamsType);
		vapp.getTasks().get(0).waitForTask(0);
		vapp = Vapp.getVappByReference(vcloudClient, vapp.getReference());

	}

	/**
	 * Creating Vapp with 'n' blank vm's sample.
	 *
	 * @param args
	 * @throws VCloudException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws TimeoutException
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 * @throws InterruptedException
	 */
	public static void main(String args[]) throws VCloudException,
			KeyManagementException, NoSuchAlgorithmException, IOException,
			InstantiationException, IllegalAccessException, TimeoutException,
			UnrecoverableKeyException, KeyStoreException, InterruptedException {

		if (args.length < 10) {
			System.err.println("USAGE");
			System.err.println("-----");
			System.err
					.println("vcloudUrl username password orgName vdcName newVappTemplateName existingCatalogName newVappName newVmName noOfBlankVms");
			System.err
					.println("https://cloud-rtqa-be.eng.vmware.com vadmin@System akimbi AutomationOrg AutomationVdc VappTemplate-BlankVM Catalog-VappTemplate-BlankVM Vapp-BlankVMs BlankVM 5");
			System.exit(0);
		}

		String vCloudUrl = args[0];
		String username = args[1];
		String password = args[2];
		String orgName = args[3];
		String vdcName = args[4];
		String vAppTemplateName = args[5];
		String catalogName = args[6];
		String vAppName = args[7];
		String vmName = args[8];
		Integer noOfBlankVms = Integer.valueOf(args[9]);

		System.out.println("Blank VM's Sample");
		System.out.println("-----------------");

		vcloudClient = new VcloudClient(vCloudUrl, Version.V5_1);
		// change log levels if needed.
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(username, password);

		// find org
		Organization org = Organization.getOrganizationByReference(
				vcloudClient, vcloudClient.getOrgRefByName(orgName));
		System.out.println("Organization :: " + org.getReference().getName());

		// find vdc
		vdc = Vdc.getVdcByReference(vcloudClient, org.getVdcRefByName(vdcName));
		System.out.println("Vdc :: " + vdc.getReference().getName());

		// upload vapptemplate
		vappTemplate = uploadVappTemplate(vAppTemplateName, vAppTemplateName,
				new ByteArrayInputStream(blankVmXML.getBytes()));
		System.out.println("Created VappTemplate :: " + vAppTemplateName);

		// add to catalog
		ReferenceType catalogRef = null;
		for (ReferenceType reference : org.getCatalogRefs()) {
			if (reference.getName().equals(catalogName)) {
				catalogRef = reference;
				break;
			}
		}
		addVappTemplateToCatalog(catalogRef, vAppTemplateName);
		System.out.println("Added VappTemplate to Catalog :: "
				+ catalogRef.getName());

		// creating vapp with the specified no of blank vm's.
		createVapp(vAppName, vmName, noOfBlankVms);
		System.out.println("Created Vapp :: " + vapp.getReference().getName());
		System.out.println("		VM Names");
		System.out.println("		--------");
		for (VM vm : vapp.getChildrenVms()) {
			System.out.println("		" + vm.getResource().getName());
		}
	}
}
