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
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.AdminCatalogType;
import com.vmware.vcloud.api.rest.schema.AdminOrgType;
import com.vmware.vcloud.api.rest.schema.CapacityWithUsageType;
import com.vmware.vcloud.api.rest.schema.ComputeCapacityType;
import com.vmware.vcloud.api.rest.schema.CreateVdcParamsType;
import com.vmware.vcloud.api.rest.schema.GatewayConfigurationType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfaceType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfacesType;
import com.vmware.vcloud.api.rest.schema.GatewayType;
import com.vmware.vcloud.api.rest.schema.IpRangeType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.IpScopeType;
import com.vmware.vcloud.api.rest.schema.IpScopesType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.OrgEmailSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgGeneralSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgLeaseSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgSettingsType;
import com.vmware.vcloud.api.rest.schema.OrgVdcNetworkType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SubnetParticipationType;
import com.vmware.vcloud.api.rest.schema.UserType;
import com.vmware.vcloud.api.rest.schema.VdcStorageProfileParamsType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminCatalog;
import com.vmware.vcloud.sdk.admin.AdminOrgVdcNetwork;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;
import com.vmware.vcloud.sdk.admin.ProviderVdc;
import com.vmware.vcloud.sdk.admin.User;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.AllocationModelType;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.GatewayBackingConfigValuesType;
import com.vmware.vcloud.sdk.constants.GatewayEnums;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * vCloud Java SDK sample that demonstrates self provisioning operations when
 * on-boarding a customer @ vCloud Service Provider. These operations includes
 * Administrative Tasks such as Create Organization, Create vDC, Add Catalog &
 * User to the Organization etc.
 * 
 * @author Ecosystem Engineering
 * 
 */

public class SelfProvisioningSample {

	private static VcloudClient client;
	private static VcloudAdmin adminClient;
	private static String providerVdcName;
	private static String externalNetworkName;
	private static String networkPoolName;
	private static AdminVdc adminVdc;
	private static EdgeGateway edgeGateway;

	/**
	 * @param args
	 * @throws TimeoutException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	public static void main(String[] args) throws TimeoutException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException {

		try {

			if (args.length < 6) {
				usage();
			}

			providerVdcName = args[3];
			externalNetworkName = args[4];
			networkPoolName = args[5];

			VcloudClient.setLogLevel(Level.OFF);
			client = new VcloudClient(args[0], Version.V5_1);
			client.registerScheme("https", 443, FakeSSLSocketFactory
					.getInstance());
			client.login(args[1], args[2]);

			adminClient = client.getVcloudAdmin();

			AdminOrgType adminOrgType = new AdminOrgType();
			OrgSettingsType orgSettings = new OrgSettingsType();
			setOrgProfile(adminOrgType);
			populateOrgSettings(orgSettings);
			adminOrgType.setSettings(orgSettings);

			System.out.println("API Self Provisioning Sample");
			System.out.println("----------------------------");

			// Create organization
			System.out.println("");
			AdminOrganization adminOrg = adminClient
					.createAdminOrg(adminOrgType);
			System.out.println("Creating API Sample Org : "
					+ adminOrg.getResource().getName() + " : "
					+ adminOrg.getResource().getHref());
			List<Task> tasks = adminOrg.getTasks();
			if (tasks.size() > 0)
				tasks.get(0).waitForTask(0);

			// Create vDC You may end using one of the following.
			addVdc(adminClient, adminOrg);
			addPayAsYouGoVdc(adminClient, adminOrg);

			// Create user on the organization
			addUserToOrg(adminOrg);

			// Create catalog on the organization
			addCatalog(adminOrg);

			// Create org vdc networks on the organizaiton
			addBridgedOrgVdcNetwork(adminOrg);
			addNatRoutedOrgVdcNetwork(adminOrg);
			addIsolatedOrgVdcNetwork(adminOrg);

		} catch (VCloudException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Adding catalog to the organization
	 * 
	 * @param adminOrg
	 * @throws TimeoutException
	 */
	private static void addCatalog(AdminOrganization adminOrg)
			throws TimeoutException {
		AdminCatalogType newCatalogType = new AdminCatalogType();
		try {
			// Name this Catalog
			newCatalogType.setName("API_Sample_Catalog");
			newCatalogType.setDescription("API Sample Catalog Description");

			// Share this Catalog
			newCatalogType.setIsPublished(true);

			AdminCatalog adminCatalog = adminOrg.createCatalog(newCatalogType);
			System.out.println("Creating API Sample Catalog : "
					+ adminCatalog.getResource().getName() + " : "
					+ adminCatalog.getResource().getHref());
			List<Task> tasks = adminCatalog.getTasks();
			if (tasks.size() > 0)
				tasks.get(0).waitForTask(0);

		} catch (VCloudException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Adding user to the organization
	 * 
	 * @param adminOrg
	 * @throws TimeoutException
	 */
	private static void addUserToOrg(AdminOrganization adminOrg)
			throws TimeoutException {
		UserType newUserType = new UserType();

		// Credentias
		newUserType.setName("API_Sample_user");
		newUserType.setPassword("password");
		newUserType.setIsEnabled(true);

		// Role : 'Organization Administrator'
		ReferenceType usrRoleRef = adminClient
				.getRoleRefByName("Organization Administrator");
		newUserType.setRole(usrRoleRef);

		// COntact Info:
		newUserType.setFullName("User Full Name");
		newUserType.setEmailAddress("user@company.com");
		// Use defaults for rest of the fields.

		try {
			User user = adminOrg.createUser(newUserType);

			System.out.println("Creating API Sample User : "
					+ user.getResource().getName() + " : "
					+ user.getResource().getHref());
			List<Task> tasks = user.getTasks();
			if (tasks.size() > 0)
				tasks.get(0).waitForTask(0);

		} catch (VCloudException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adding isolated org vdc network to the organization
	 * 
	 * @param adminOrg
	 *            {@link AdminOrganization}
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void addIsolatedOrgVdcNetwork(AdminOrganization adminOrg)
			throws VCloudException, TimeoutException {

		OrgVdcNetworkType OrgVdcNetworkParams = new OrgVdcNetworkType();
		OrgVdcNetworkParams.setName("Isolated_Org_Vdc_Network");
		OrgVdcNetworkParams.setDescription("Org vdc network of type Isolated");

		// COnfigure Internal IP Settings
		NetworkConfigurationType netConfig = new NetworkConfigurationType();
		netConfig.setRetainNetInfoAcrossDeployments(true);

		IpScopeType ipScope = new IpScopeType();
		ipScope.setNetmask("255.255.255.0");
		ipScope.setGateway("192.168.1.200");
		ipScope.setIsEnabled(true);
		ipScope.setIsInherited(true);

		// IP Ranges
		IpRangesType ipRangesType = new IpRangesType();
		IpRangeType ipRangeType = new IpRangeType();
		ipRangeType.setStartAddress("192.168.1.2");
		ipRangeType.setEndAddress("192.168.1.100");

		ipRangesType.getIpRange().add(ipRangeType);

		ipScope.setIpRanges(ipRangesType);
		ipScope.setIsEnabled(true);
		IpScopesType ipScopes = new IpScopesType();
		ipScopes.getIpScope().add(ipScope);
		netConfig.setIpScopes(ipScopes);
		netConfig.setFenceMode(FenceModeValuesType.ISOLATED.value());

		OrgVdcNetworkParams.setEdgeGateway(edgeGateway.getReference());
		OrgVdcNetworkParams.setConfiguration(netConfig);

		System.out.println("Creating Isolated Org vDC Network");
		try {
			AdminOrgVdcNetwork orgVdcNet =  adminVdc.createOrgVdcNetwork(OrgVdcNetworkParams);
			if(orgVdcNet.getTasks().size() > 0) {
				orgVdcNet.getTasks().get(0).waitForTask(0);
			}
			System.out.println("	Isolated Org vDC Network : " + orgVdcNet.getResource().getName() + " created - "
					+ orgVdcNet.getResource().getHref());
		} catch (VCloudException e) {
			System.out.println("FAILED: creating org vdc network - "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * Adding nat routed org vdc network to the organization
	 * 
	 * @param adminOrg
	 *            {@link AdminOrganization}
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void addNatRoutedOrgVdcNetwork(AdminOrganization adminOrg)
			throws VCloudException, TimeoutException {

		OrgVdcNetworkType OrgVdcNetworkParams = new OrgVdcNetworkType();
		OrgVdcNetworkParams.setName("Nat-Routed_Org_Vdc_Network");
		OrgVdcNetworkParams
				.setDescription("Org vdc network of type Nat-Routed");

		// COnfigure Internal IP Settings
		NetworkConfigurationType netConfig = new NetworkConfigurationType();
		netConfig.setRetainNetInfoAcrossDeployments(true);

		IpScopeType ipScope = new IpScopeType();
		ipScope.setNetmask("255.255.255.0");
		ipScope.setGateway("192.168.1.200");
		ipScope.setIsEnabled(true);
		ipScope.setIsInherited(true);

		// IP Ranges
		IpRangesType ipRangesType = new IpRangesType();
		IpRangeType ipRangeType = new IpRangeType();
		ipRangeType.setStartAddress("192.168.1.2");
		ipRangeType.setEndAddress("192.168.1.100");

		ipRangesType.getIpRange().add(ipRangeType);

		ipScope.setIpRanges(ipRangesType);
		ipScope.setIsEnabled(true);
		IpScopesType ipScopes = new IpScopesType();
		ipScopes.getIpScope().add(ipScope);
		netConfig.setIpScopes(ipScopes);
		netConfig.setFenceMode(FenceModeValuesType.NATROUTED.value());
		ReferenceType externalNetRef = getExternalNetworkRef(externalNetworkName);
		GatewayType gateway = createEdgeGatewayParams(externalNetRef,
				"Edge_Gateway_Name");
		edgeGateway = adminVdc.createEdgeGateway(gateway);
		if(edgeGateway.getTasks().size() > 0) {
			edgeGateway.getTasks().get(0).waitForTask(0);
		}

		OrgVdcNetworkParams.setEdgeGateway(edgeGateway.getReference());
		OrgVdcNetworkParams.setConfiguration(netConfig);
		System.out.println("Creating Nat-Routed Org vDC Network");
		try {
			AdminOrgVdcNetwork orgVdcNet =  adminVdc.createOrgVdcNetwork(OrgVdcNetworkParams);
			if(orgVdcNet.getTasks().size() > 0) {
				orgVdcNet.getTasks().get(0).waitForTask(0);
			}
			System.out.println("	Nat-Routed Org vDC Network : " + orgVdcNet.getResource().getName() + " created - "
					+ orgVdcNet.getResource().getHref());
		} catch (VCloudException e) {
			System.out.println("FAILED: creating org vdc network - "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * Adding an bridged org vdc network to the organization
	 * 
	 * @param adminOrg
	 *            {@link AdminOrganization}
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void addBridgedOrgVdcNetwork(AdminOrganization adminOrg)
			throws VCloudException, TimeoutException {

		OrgVdcNetworkType orgVdcNetworkParams = new OrgVdcNetworkType();
		orgVdcNetworkParams.setName("Bridged_Org_Vdc_Network");
		orgVdcNetworkParams.setDescription("Org vdc network of type Bridge");
		NetworkConfigurationType networkConfig = new NetworkConfigurationType();
		networkConfig.setParentNetwork(client.getVcloudAdminExtension()
				.getVMWExternalNetworkRefsByName().values().iterator().next());
		networkConfig.setFenceMode(FenceModeValuesType.BRIDGED.value());
		orgVdcNetworkParams.setConfiguration(networkConfig);
		System.out.println("Creating Bridged Org vDC Network");
		try {
			AdminOrgVdcNetwork orgVdcNet = adminVdc.createOrgVdcNetwork(orgVdcNetworkParams);
			if(orgVdcNet.getTasks().size() > 0) {
				orgVdcNet.getTasks().get(0).waitForTask(0);
			}
			System.out.println("	Bridged Org vDC Network : " + orgVdcNet.getResource().getName() + " created - "
					+ orgVdcNet.getResource().getHref());
		} catch (VCloudException e) {
			System.out.println("FAILED: creating org vdc network - "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * Gets External Network Reference
	 * 
	 * @param networkName
	 *            {@link String}
	 * @return {@link ReferenceType}
	 * 
	 * @throws VCloudException
	 */
	private static ReferenceType getExternalNetworkRef(String networkName)
			throws VCloudException {
		return client.getVcloudAdmin().getExternalNetworkRefByName(networkName);
	}

	/**
	 * Create params for Edge Gateway
	 * 
	 * @param externalNetwork
	 *            {@link ReferenceType}
	 * @return GatewayType
	 * @throws VCloudException
	 */
	private static GatewayType createEdgeGatewayParams(
			ReferenceType externalNetwork, String edgeGatewayName)
			throws VCloudException {
		GatewayType gatewayParams = new GatewayType();
		gatewayParams.setName(edgeGatewayName);
		gatewayParams.setDescription("ee-gateway desc");
		GatewayConfigurationType gatewayConfig = new GatewayConfigurationType();
		gatewayConfig
				.setGatewayBackingConfig(GatewayBackingConfigValuesType.COMPACT
						.value());
		GatewayInterfaceType gatewayInterface = new GatewayInterfaceType();
		gatewayInterface.setDisplayName("gateway interface");
		gatewayInterface.setNetwork(externalNetwork);
		gatewayInterface.setInterfaceType(GatewayEnums.UPLINK.value());
		SubnetParticipationType subnetParticipationType = new SubnetParticipationType();
		subnetParticipationType.setGateway("10.147.74.253");
		subnetParticipationType.setNetmask("255.255.255.0");
		IpRangesType ipRanges = new IpRangesType();
		IpRangeType ipRange = new IpRangeType();
		ipRange.setStartAddress("10.147.74.238");
		ipRange.setEndAddress("10.147.74.239");
		ipRanges.getIpRange().add(ipRange);
		subnetParticipationType.setIpRanges(ipRanges);
		gatewayInterface.getSubnetParticipation().add(subnetParticipationType);
		gatewayInterface.setUseForDefaultRoute(true);
		GatewayInterfacesType interfaces = new GatewayInterfacesType();
		interfaces.getGatewayInterface().add(gatewayInterface);
		gatewayConfig.setGatewayInterfaces(interfaces);

		gatewayParams.setConfiguration(gatewayConfig);

		return gatewayParams;
	}

	/**
	 * Adding the pay as you go vdc.
	 * 
	 * @param adminClient
	 * @param adminOrg
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void addPayAsYouGoVdc(VcloudAdmin adminClient,
			AdminOrganization adminOrg) throws VCloudException,
			TimeoutException {
		CreateVdcParamsType createVdcParams = new CreateVdcParamsType();

		// Select Provider VDC
		ReferenceType pvdcRef = adminClient
				.getProviderVdcRefByName(providerVdcName);
		createVdcParams.setProviderVdcReference(pvdcRef);
		ProviderVdc pvdc = ProviderVdc.getProviderVdcByReference(client,
				pvdcRef);

		// Select Allocation Model - 'Pay As You Go' Model
		createVdcParams.setAllocationModel(AllocationModelType.ALLOCATIONVAPP
				.value());

		createVdcParams.setResourceGuaranteedCpu(0.25); // 25% CPU Resources
		// guaranteed
		createVdcParams.setResourceGuaranteedMemory(0.99); // 99% Memory
		// resources
		// guaranteed
		// Rest all Defaults for the 'Pay As You Go Model' configuration.

		// COmpute Capacity -- this is needed. UI Uses defaults.
		ComputeCapacityType computeCapacity = new ComputeCapacityType();
		CapacityWithUsageType cpu = new CapacityWithUsageType();
		cpu.setAllocated(Long.parseLong("10"));
		cpu.setOverhead(Long.parseLong("0"));
		cpu.setUnits("MHz");
		cpu.setUsed(Long.parseLong("0"));
		cpu.setLimit(Long.parseLong("0"));

		computeCapacity.setCpu(cpu);

		CapacityWithUsageType mem = new CapacityWithUsageType();
		mem.setAllocated(Long.parseLong("10"));
		mem.setOverhead(Long.parseLong("0"));
		mem.setUnits("MB");
		mem.setUsed(Long.parseLong("0"));
		mem.setLimit(Long.parseLong("1000"));

		computeCapacity.setMemory(mem);

		createVdcParams.setComputeCapacity(computeCapacity);

		// Select Network Pool
		ReferenceType netPoolRef = pvdc
				.getVMWNetworkPoolRefByName(networkPoolName);
		createVdcParams.setNetworkPoolReference(netPoolRef);
		createVdcParams.setNetworkQuota(24);

		// Name this Organization vDC
		createVdcParams.setName("API_Sample_Pay_as_you_go_vdc");
		createVdcParams.setDescription("API Sample - Pay as you go vdc");
		createVdcParams.setIsEnabled(true);

		VdcStorageProfileParamsType vdcStorageProfile = new VdcStorageProfileParamsType();
		vdcStorageProfile.setEnabled(true);
		vdcStorageProfile.setDefault(true);
		vdcStorageProfile.setLimit(10);
		vdcStorageProfile.setUnits("MB");

		ReferenceType providerVdcStorageProfileRef = pvdc
				.getProviderVdcStorageProfileRefs().iterator().next();
		vdcStorageProfile
				.setProviderVdcStorageProfile(providerVdcStorageProfileRef);
		createVdcParams.getVdcStorageProfile().add(vdcStorageProfile);

		AdminVdc adminVdc = adminOrg.createAdminVdc(createVdcParams);

		System.out.println("Creating API Sample Pay As You Go Vdc : "
				+ adminVdc.getResource().getName() + " : "
				+ adminVdc.getResource().getHref());
		List<Task> tasks = adminVdc.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);

	}

	/**
	 * Adding the allocation pool vdc
	 * 
	 * @param adminClient
	 * @param adminOrg
	 * @throws VCloudException
	 * @throws TimeoutException
	 */
	private static void addVdc(VcloudAdmin adminClient,
			AdminOrganization adminOrg) throws VCloudException,
			TimeoutException {
		CreateVdcParamsType createVdcParams = new CreateVdcParamsType();
		createVdcParams.setName("API_Sample_Allocation_Pool_vdc");
		createVdcParams.setIsEnabled(true);

		createVdcParams.setResourceGuaranteedCpu(1.00);
		createVdcParams.setResourceGuaranteedMemory(1.00);

		createVdcParams.setVmQuota(25);
		createVdcParams
				.setDescription("API Sample Test - Self provisioning using API");

		// Change the provider vdc name. The to be Creating vdc will be backed
		// by
		// this provider vdc.
		ReferenceType pvdcRef = adminClient
				.getProviderVdcRefByName(providerVdcName);
		ProviderVdc pvdc = ProviderVdc.getProviderVdcByReference(client,
				pvdcRef);
		createVdcParams.setProviderVdcReference(pvdcRef);

		// Change the type of provider vdc
		createVdcParams.setAllocationModel(AllocationModelType.ALLOCATIONPOOL
				.value());

		// Change the compute capacities cpu, memory
		ComputeCapacityType computeCapacity = new ComputeCapacityType();
		CapacityWithUsageType cpu = new CapacityWithUsageType();
		cpu.setAllocated(Long.parseLong("10"));
		cpu.setOverhead(Long.parseLong("0"));
		cpu.setUnits("MHz");
		cpu.setUsed(Long.parseLong("0"));
		cpu.setLimit(Long.parseLong("0"));

		computeCapacity.setCpu(cpu);

		CapacityWithUsageType mem = new CapacityWithUsageType();
		mem.setAllocated(Long.parseLong("10"));
		mem.setOverhead(Long.parseLong("0"));
		mem.setUnits("MB");
		mem.setUsed(Long.parseLong("0"));
		mem.setLimit(Long.parseLong("1000"));

		computeCapacity.setMemory(mem);

		createVdcParams.setComputeCapacity(computeCapacity);

		// Change the network pool reference
		ReferenceType netPoolRef = pvdc
				.getVMWNetworkPoolRefByName(networkPoolName);

		createVdcParams.setNetworkPoolReference(netPoolRef);
		createVdcParams.setNetworkQuota(1024);

		VdcStorageProfileParamsType vdcStorageProfile = new VdcStorageProfileParamsType();
		vdcStorageProfile.setEnabled(true);
		vdcStorageProfile.setDefault(true);
		vdcStorageProfile.setLimit(10);
		vdcStorageProfile.setUnits("MB");

		ReferenceType providerVdcStorageProfileRef = pvdc
				.getProviderVdcStorageProfileRefs().iterator().next();
		vdcStorageProfile
				.setProviderVdcStorageProfile(providerVdcStorageProfileRef);
		createVdcParams.getVdcStorageProfile().add(vdcStorageProfile);

		adminVdc = adminOrg.createAdminVdc(createVdcParams);

		System.out.println("Creating API Sample Allocation Pool Vdc : "
				+ adminVdc.getResource().getName() + " : "
				+ adminVdc.getResource().getHref());
		List<Task> tasks = adminVdc.getTasks();
		if (tasks.size() > 0)
			tasks.get(0).waitForTask(0);
	}

	/**
	 * Populating the organization settings.
	 * 
	 * @param orgSettings
	 */
	private static void populateOrgSettings(OrgSettingsType orgSettings) {

		OrgGeneralSettingsType orgGeneralSettingsType = new OrgGeneralSettingsType();

		// Add Local Users
		// None at this time

		// Catalog Publishing
		orgGeneralSettingsType.setCanPublishCatalogs(true);

		// Email Preferences
		OrgEmailSettingsType orgEmailSettings = new OrgEmailSettingsType();
		orgEmailSettings.setIsDefaultSmtpServer(true);
		orgEmailSettings.setFromEmailAddress("user@company.com");
		orgEmailSettings.setDefaultSubjectPrefix("Java SDK API Sample");
		orgEmailSettings.getAlertEmailTo().add("user@company.com");
		orgSettings.setOrgEmailSettings(orgEmailSettings);

		// Policies
		orgGeneralSettingsType.setDeployedVMQuota(100);
		orgGeneralSettingsType.setStoredVmQuota(0);
		orgSettings.setOrgGeneralSettings(orgGeneralSettingsType);
		OrgLeaseSettingsType orgLeaseSettings = new OrgLeaseSettingsType();
		orgLeaseSettings.setDeploymentLeaseSeconds(86400);
		orgLeaseSettings.setStorageLeaseSeconds(864000);
		orgSettings.setVAppLeaseSettings(orgLeaseSettings);

	}

	/**
	 * Setting the Organization name, description and the fullname
	 * 
	 * @param adminOrgType
	 */
	private static void setOrgProfile(AdminOrgType adminOrgType) {
		adminOrgType.setFullName("API Sample Org");
		adminOrgType.setDescription("API Sample - Self provisioning using API");
		adminOrgType.setName("API_Sample_Org");
		adminOrgType.setIsEnabled(true);
	}

	/**
	 * EdgeGateway Usage
	 */
	public static void usage() {
		System.out
				.println("java SelfProvisioningSample vCloudURL user@organization password ProviderVdcName ExternalNetworkName NetworkPoolName");
		System.out
				.println("java SelfProvisioningSample https://vcloud user@System password providervdcname externalnetworkname networkpoolname");
		System.exit(0);
	}
}