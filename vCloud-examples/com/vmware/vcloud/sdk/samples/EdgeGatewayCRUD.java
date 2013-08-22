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
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.xml.bind.JAXBElement;

import com.vmware.vcloud.api.rest.schema.DhcpServiceType;
import com.vmware.vcloud.api.rest.schema.FirewallServiceType;
import com.vmware.vcloud.api.rest.schema.GatewayConfigurationType;
import com.vmware.vcloud.api.rest.schema.GatewayFeaturesType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfaceType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfacesType;
import com.vmware.vcloud.api.rest.schema.GatewayType;
import com.vmware.vcloud.api.rest.schema.IpRangeType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.IpsecVpnServiceType;
import com.vmware.vcloud.api.rest.schema.IpsecVpnTunnelType;
import com.vmware.vcloud.api.rest.schema.LBPersistenceType;
import com.vmware.vcloud.api.rest.schema.LBPoolHealthCheckType;
import com.vmware.vcloud.api.rest.schema.LBPoolMemberType;
import com.vmware.vcloud.api.rest.schema.LBPoolServicePortType;
import com.vmware.vcloud.api.rest.schema.LBVirtualServerServiceProfileType;
import com.vmware.vcloud.api.rest.schema.LoadBalancerPoolType;
import com.vmware.vcloud.api.rest.schema.LoadBalancerServiceType;
import com.vmware.vcloud.api.rest.schema.LoadBalancerVirtualServerType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.NetworkServiceType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.StaticRouteType;
import com.vmware.vcloud.api.rest.schema.StaticRoutingServiceType;
import com.vmware.vcloud.api.rest.schema.SubnetParticipationType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;
import com.vmware.vcloud.sdk.constants.FirewallPolicyType;
import com.vmware.vcloud.sdk.constants.GatewayBackingConfigValuesType;
import com.vmware.vcloud.sdk.constants.GatewayEnums;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Creating, Updating, Getting and Deleting Edge Gateway
 *
 * Should be System Administrator.
 * Should have Admin VDC.
 * Should have External Network.
 *
 * @author Ecosystem Engineering
 *
 */

public class EdgeGatewayCRUD {

	private static VcloudClient client;

	/**
	 * EdgeGateway Usage
	 */
	public static void usage() {
		System.out
				.println("java EdgeGatewayCRUD Vcloud user@organization password OrgName AdminVdcName ExternalNetworkName EdgeGatewayName");
		System.out
				.println("java EdgeGatewayCRUD https://vcloud username@Organization password orgName adminVdcName externalNetworkName edgeGatewayName");
		System.exit(0);
	}

	/**
	 * Check for tasks if any
	 * 
	 * @param edgeGateway
	 *            {@link EdgeGateway}
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(EdgeGateway edgeGateway)
			throws VCloudException {
		TasksInProgressType tasksInProgress = edgeGateway.getResource()
				.getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Gets AdminVdc Reference
	 * 
	 * @param adminVdcName
	 *            {@link String}
	 * @param adminOrgRef
	 *            {@link ReferenceType}
	 * @return {@link ReferenceType}
	 * 
	 * @throws VCloudException
	 */
	private static ReferenceType getAdminVdcRef(String adminVdcName,
			ReferenceType adminOrgRef) throws VCloudException {
		AdminOrganization adminOrg;
		adminOrg = AdminOrganization
				.getAdminOrgByReference(client, adminOrgRef);
		return adminOrg.getAdminVdcRefByName(adminVdcName);
	}

	/**
	 * Gets Org Reference
	 * 
	 * @param orgName
	 *            {@link String}
	 * @return {@link ReferenceType}
	 * 
	 * @throws VCloudException
	 */
	private static ReferenceType getOrgRef(String orgName)
			throws VCloudException {
		return client.getVcloudAdmin().getAdminOrgRefByName(orgName);
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
		ipRange.setStartAddress("10.147.74.215");
		ipRange.setEndAddress("10.147.74.236");
		ipRanges.getIpRange().add(ipRange);
		subnetParticipationType.setIpRanges(ipRanges);
		gatewayInterface.getSubnetParticipation().add(subnetParticipationType);
		gatewayInterface.setUseForDefaultRoute(true);
		GatewayInterfacesType interfaces = new GatewayInterfacesType();
		interfaces.getGatewayInterface().add(gatewayInterface);
		gatewayConfig.setGatewayInterfaces(interfaces);

		GatewayFeaturesType gatewayFeatures = new GatewayFeaturesType();

		ObjectFactory objectFactory = new ObjectFactory();

		NatServiceType natService = new NatServiceType();
		natService.setExternalIp("10.147.74.217");
		natService.setIsEnabled(true);

		JAXBElement<NetworkServiceType> serviceType = objectFactory
				.createNetworkService(natService);
		gatewayFeatures.getNetworkService().add(serviceType);

		FirewallServiceType firewallService = new FirewallServiceType();
		firewallService.setIsEnabled(true);
		firewallService.setDefaultAction(FirewallPolicyType.DROP.value());
		firewallService.setLogDefaultAction(false);

		JAXBElement<FirewallServiceType> firewall = objectFactory
				.createFirewallService(firewallService);
		gatewayFeatures.getNetworkService().add(firewall);

		DhcpServiceType dhcpService = new DhcpServiceType();
		dhcpService.setDefaultLeaseTime(0);
		dhcpService.setIpRange(ipRange);
		dhcpService.setIsEnabled(true);
		dhcpService.setPrimaryNameServer("r2");
		dhcpService.setSubMask("255.255.255.0");
		dhcpService.setDefaultLeaseTime(3600);
		dhcpService.setMaxLeaseTime(7200);

		JAXBElement<DhcpServiceType> dhcp = objectFactory
				.createDhcpService(dhcpService);
		gatewayFeatures.getNetworkService().add(dhcp);

		LoadBalancerServiceType loadBalancer = new LoadBalancerServiceType();

		LoadBalancerPoolType pool = new LoadBalancerPoolType();
		pool.setDescription("Pool Desc");
		pool.setName("PoolName");
		pool.setOperational(true);

		LBPoolHealthCheckType lBPoolHealthCheck = new LBPoolHealthCheckType();
		lBPoolHealthCheck.setHealthThreshold("2");
		lBPoolHealthCheck.setUnhealthThreshold("3");
		lBPoolHealthCheck.setInterval("5");
		lBPoolHealthCheck.setMode("HTTP");
		lBPoolHealthCheck.setTimeout("15");

		LBPoolMemberType lBPoolMember = new LBPoolMemberType();
		lBPoolMember.setIpAddress("10.147.74.220");
		lBPoolMember.setWeight("1");

		LBPoolServicePortType lBPoolServicePort = new LBPoolServicePortType();
		lBPoolServicePort.setIsEnabled(true);
		lBPoolServicePort.setAlgorithm("ROUND_ROBIN");
		lBPoolServicePort.setHealthCheckPort("80");
		lBPoolServicePort.getHealthCheck().add(lBPoolHealthCheck);
		lBPoolServicePort.setProtocol("HTTP");
		lBPoolServicePort.setPort("80");

		pool.getServicePort().add(lBPoolServicePort);

		pool.getMember().add(lBPoolMember);
		loadBalancer.getPool().add(pool);

		LoadBalancerVirtualServerType loadBalancerVirtualServer = new LoadBalancerVirtualServerType();
		loadBalancerVirtualServer.setDescription("desc");
		loadBalancerVirtualServer.setIsEnabled(true);
		loadBalancerVirtualServer.setIpAddress("10.147.74.222");
		loadBalancerVirtualServer.setName("VirtualServerName");
		loadBalancerVirtualServer.setPool("PoolName");
		loadBalancerVirtualServer.setLogging(true);
		loadBalancerVirtualServer.setInterface(externalNetwork);

		LBVirtualServerServiceProfileType lBVirtualServerServiceProfile = new LBVirtualServerServiceProfileType();
		lBVirtualServerServiceProfile.setProtocol("HTTP");
		lBVirtualServerServiceProfile.setPort("80");
		lBVirtualServerServiceProfile.setIsEnabled(true);

		LBPersistenceType lBPersistence = new LBPersistenceType();
		lBPersistence.setCookieMode("INSERT");
		lBPersistence.setCookieName("CookieName");
		lBPersistence.setMethod("COOKIE");
		lBVirtualServerServiceProfile.setPersistence(lBPersistence);
		loadBalancerVirtualServer.getServiceProfile().add(
				lBVirtualServerServiceProfile);

		loadBalancer.getVirtualServer().add(loadBalancerVirtualServer);
		loadBalancer.setIsEnabled(true);

		JAXBElement<LoadBalancerServiceType> load = objectFactory
				.createLoadBalancerService(loadBalancer);
		gatewayFeatures.getNetworkService().add(load);

		StaticRoutingServiceType staticRouting = new StaticRoutingServiceType();
		staticRouting.setIsEnabled(true);
		StaticRouteType staticRoute = new StaticRouteType();
		staticRoute.setName("RouteName");
		staticRoute.setNetwork("10.147.2.0/24");
		staticRoute.setNextHopIp("10.147.74.235");
		staticRoute.setGatewayInterface(externalNetwork);
		staticRoute.setInterface("External");
		staticRouting.getStaticRoute().add(staticRoute);

		JAXBElement<StaticRoutingServiceType> route = objectFactory
				.createStaticRoutingService(staticRouting);
		gatewayFeatures.getNetworkService().add(route);

		IpsecVpnServiceType vpn = new IpsecVpnServiceType();
		vpn.setExternalIpAddress("10.147.74.211");
		vpn.setIsEnabled(false);
		vpn.setPublicIpAddress("10.147.74.218");
		IpsecVpnTunnelType ipsecVpnTunnel = new IpsecVpnTunnelType();
		ipsecVpnTunnel.setMtu(1500);
		ipsecVpnTunnel.setName("VpnName");

		JAXBElement<IpsecVpnServiceType> ipsecVpn = objectFactory
				.createIpsecVpnService(vpn);
		gatewayFeatures.getNetworkService().add(ipsecVpn);

		gatewayConfig.setEdgeGatewayServiceConfiguration(gatewayFeatures);
		gatewayParams.setConfiguration(gatewayConfig);

		return gatewayParams;
	}

	/**
	 * Updates Edge Gateway
	 * 
	 * @param externalNetwork
	 *            {@link ReferenceType}
	 * 
	 * @return GatewayType
	 * @throws VCloudException
	 */
	private static GatewayType updateEdgeGatewayParams(
			ReferenceType externalNetwork, String edgeGatewayName)
			throws VCloudException {
		GatewayType gatewayParams = new GatewayType();
		gatewayParams.setName(edgeGatewayName + "_Updated");
		gatewayParams.setDescription("updated desc");
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
		ipRange.setStartAddress("10.147.74.215");
		ipRange.setEndAddress("10.147.74.236");
		ipRanges.getIpRange().add(ipRange);
		subnetParticipationType.setIpRanges(ipRanges);
		gatewayInterface.getSubnetParticipation().add(subnetParticipationType);
		gatewayInterface.setUseForDefaultRoute(true);
		GatewayInterfacesType interfaces = new GatewayInterfacesType();
		interfaces.getGatewayInterface().add(gatewayInterface);
		gatewayConfig.setGatewayInterfaces(interfaces);

		GatewayFeaturesType gatewayFeatures = new GatewayFeaturesType();

		ObjectFactory objectFactory = new ObjectFactory();

		NatServiceType natService = new NatServiceType();
		natService.setExternalIp("10.147.74.217");
		natService.setIsEnabled(true);

		JAXBElement<NetworkServiceType> serviceType = objectFactory
				.createNetworkService(natService);
		gatewayFeatures.getNetworkService().add(serviceType);

		FirewallServiceType firewallService = new FirewallServiceType();
		firewallService.setIsEnabled(true);
		firewallService.setDefaultAction(FirewallPolicyType.DROP.value());
		firewallService.setLogDefaultAction(false);

		JAXBElement<FirewallServiceType> firewall = objectFactory
				.createFirewallService(firewallService);
		gatewayFeatures.getNetworkService().add(firewall);

		DhcpServiceType dhcpService = new DhcpServiceType();
		dhcpService.setDefaultLeaseTime(0);
		dhcpService.setIpRange(ipRange);
		dhcpService.setIsEnabled(true);
		dhcpService.setPrimaryNameServer("r2");
		dhcpService.setSubMask("255.255.255.0");
		dhcpService.setDefaultLeaseTime(3600);
		dhcpService.setMaxLeaseTime(7200);

		JAXBElement<DhcpServiceType> dhcp = objectFactory
				.createDhcpService(dhcpService);
		gatewayFeatures.getNetworkService().add(dhcp);

		LoadBalancerServiceType loadBalancer = new LoadBalancerServiceType();

		LoadBalancerPoolType pool = new LoadBalancerPoolType();
		pool.setDescription("Pool Desc");
		pool.setName("PoolName");
		pool.setOperational(true);

		LBPoolHealthCheckType lBPoolHealthCheck = new LBPoolHealthCheckType();
		lBPoolHealthCheck.setHealthThreshold("2");
		lBPoolHealthCheck.setUnhealthThreshold("3");
		lBPoolHealthCheck.setInterval("5");
		lBPoolHealthCheck.setMode("HTTP");
		lBPoolHealthCheck.setTimeout("15");

		LBPoolMemberType lBPoolMember = new LBPoolMemberType();
		lBPoolMember.setIpAddress("10.147.74.220");
		lBPoolMember.setWeight("1");

		LBPoolServicePortType lBPoolServicePort = new LBPoolServicePortType();
		lBPoolServicePort.setIsEnabled(true);
		lBPoolServicePort.setAlgorithm("ROUND_ROBIN");
		lBPoolServicePort.setHealthCheckPort("80");
		lBPoolServicePort.getHealthCheck().add(lBPoolHealthCheck);
		lBPoolServicePort.setProtocol("HTTP");
		lBPoolServicePort.setPort("80");

		pool.getServicePort().add(lBPoolServicePort);

		pool.getMember().add(lBPoolMember);
		loadBalancer.getPool().add(pool);

		LoadBalancerVirtualServerType loadBalancerVirtualServer = new LoadBalancerVirtualServerType();
		loadBalancerVirtualServer.setDescription("desc");
		loadBalancerVirtualServer.setIsEnabled(true);
		loadBalancerVirtualServer.setIpAddress("10.147.74.222");
		loadBalancerVirtualServer.setName("VirtualServerName");
		loadBalancerVirtualServer.setPool("PoolName");
		loadBalancerVirtualServer.setLogging(true);
		loadBalancerVirtualServer.setInterface(externalNetwork);

		LBVirtualServerServiceProfileType lBVirtualServerServiceProfile = new LBVirtualServerServiceProfileType();
		lBVirtualServerServiceProfile.setProtocol("HTTP");
		lBVirtualServerServiceProfile.setPort("80");
		lBVirtualServerServiceProfile.setIsEnabled(true);

		LBPersistenceType lBPersistence = new LBPersistenceType();
		lBPersistence.setCookieMode("INSERT");
		lBPersistence.setCookieName("CookieName");
		lBPersistence.setMethod("COOKIE");
		lBVirtualServerServiceProfile.setPersistence(lBPersistence);
		loadBalancerVirtualServer.getServiceProfile().add(
				lBVirtualServerServiceProfile);

		loadBalancer.getVirtualServer().add(loadBalancerVirtualServer);
		loadBalancer.setIsEnabled(true);

		JAXBElement<LoadBalancerServiceType> load = objectFactory
				.createLoadBalancerService(loadBalancer);
		gatewayFeatures.getNetworkService().add(load);

		StaticRoutingServiceType staticRouting = new StaticRoutingServiceType();
		staticRouting.setIsEnabled(true);
		StaticRouteType staticRoute = new StaticRouteType();
		staticRoute.setName("RouteName");
		staticRoute.setNetwork("10.147.2.0/24");
		staticRoute.setNextHopIp("10.147.74.235");
		staticRoute.setGatewayInterface(externalNetwork);
		staticRoute.setInterface("External");
		staticRouting.getStaticRoute().add(staticRoute);

		JAXBElement<StaticRoutingServiceType> route = objectFactory
				.createStaticRoutingService(staticRouting);
		gatewayFeatures.getNetworkService().add(route);

		IpsecVpnServiceType vpn = new IpsecVpnServiceType();
		vpn.setExternalIpAddress("10.147.74.211");
		vpn.setIsEnabled(false);
		vpn.setPublicIpAddress("10.147.74.218");
		IpsecVpnTunnelType ipsecVpnTunnel = new IpsecVpnTunnelType();
		ipsecVpnTunnel.setMtu(1500);
		ipsecVpnTunnel.setName("VpnName");

		JAXBElement<IpsecVpnServiceType> ipsecVpn = objectFactory
				.createIpsecVpnService(vpn);
		gatewayFeatures.getNetworkService().add(ipsecVpn);

		gatewayConfig.setEdgeGatewayServiceConfiguration(gatewayFeatures);
		gatewayParams.setConfiguration(gatewayConfig);

		return gatewayParams;
	}

	/**
	 * Main method, which does Creating and Updating Edge Gateway
	 * 
	 * @param args
	 * 
	 * @throws VCloudException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws VCloudException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, TimeoutException {

		if (args.length < 7)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud Login");
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);
		System.out.println("Login Success");

		ReferenceType orgRef = getOrgRef(args[3]);
		ReferenceType adminVdcRef = getAdminVdcRef(args[4], orgRef);
		ReferenceType externalNetRef = getExternalNetworkRef(args[5]);
		AdminVdc adminVdc = AdminVdc
				.getAdminVdcByReference(client, adminVdcRef);

		System.out.println("Create Edge Gateway");
		GatewayType gateway = createEdgeGatewayParams(externalNetRef, args[6]);
		EdgeGateway edgeGateway = adminVdc.createEdgeGateway(gateway);
		Task createTask = returnTask(edgeGateway);
		if (createTask != null)
			createTask.waitForTask(0);
		System.out.println("Edge Gateway Created");
		System.out.println("	Edge Gateway:	"
				+ edgeGateway.getResource().getName());

		System.out.println("Update Edge Gateway");
		gateway = updateEdgeGatewayParams(externalNetRef, args[6]);
		edgeGateway.updateEdgeGateway(gateway).waitForTask(0);
		System.out.println("Edge Gateway Updated");
		System.out.println("	Updated Edge Gateway:	"
				+ edgeGateway.getResource().getName());

		System.out.println("Get Edge Gateway");
		System.out.println("	"+EdgeGateway.getEdgeGatewayByReference(client,
				edgeGateway.getReference()).getResource().getName());

		System.out.println("Delete Edge Gateway");
		edgeGateway.delete().waitForTask(0);
		System.out.println("Edge Gateway deleted");

	}
}
