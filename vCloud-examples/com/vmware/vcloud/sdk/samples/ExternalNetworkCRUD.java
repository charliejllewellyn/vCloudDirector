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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.IpRangeType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.IpScopeType;
import com.vmware.vcloud.api.rest.schema.IpScopesType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.QueryResultPortgroupRecordType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.extension.VMWExternalNetworkType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefType;
import com.vmware.vcloud.sdk.Expression;
import com.vmware.vcloud.sdk.Filter;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.QueryService;
import com.vmware.vcloud.sdk.RecordResult;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.VMWExternalNetwork;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.constants.VimObjectTypeEnum;
import com.vmware.vcloud.sdk.constants.query.ExpressionType;
import com.vmware.vcloud.sdk.constants.query.FilterType;
import com.vmware.vcloud.sdk.constants.query.QueryPortgroupField;
import com.vmware.vcloud.sdk.constants.query.QueryRecordType;

/**
 * Creating, Updating, Getting and Deleting External Network.
 * 
 * Should be system administrator.
 * A vSphere port group should be available.
 * If the port group uses VLAN, it can use only a single VLAN.
 * Port groups with VLAN trunking are not supported.
 * 
 * @since SDK 5.1
 * @author Ecosystem Engineering
 * 
 */

public class ExternalNetworkCRUD {

	private static VcloudClient client;
	private static String portGroupType = "";

	/**
	 * ExternalNetworkCRUD Usage
	 */
	public static void usage() {
		System.out
				.println("java ExternalNetworkCRUD Vcloud user@organization password VimServerName PortGroupName ExternalNetworkName");
		System.out
				.println("java ExternalNetworkCRUD https://vcloud username@Organization password vimservername portgroupname externalnetworkname");
		System.exit(0);
	}

	/**
	 * Check for tasks if any
	 * 
	 * @param externalNetwork
	 *            {@link VMWExternalNetwork}
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(VMWExternalNetwork externalNetwork)
			throws VCloudException {
		TasksInProgressType tasksInProgress = externalNetwork.getResource()
				.getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Creates External Network
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * @return VMWExternalNetworkType
	 * @throws VCloudException
	 */
	private static VMWExternalNetworkType createExternalNetworkParams(
			ReferenceType vimServerRef, String moRef, String externalNetworkName)
			throws VCloudException {
		VMWExternalNetworkType vmwExternalNetworkType = new VMWExternalNetworkType();
		vmwExternalNetworkType.setName(externalNetworkName);
		vmwExternalNetworkType.setDescription("external network description");

		VimObjectRefType vimObjRef = new VimObjectRefType();
		vimObjRef.setMoRef(moRef);
		vimObjRef.setVimObjectType(portGroupType);
		vimObjRef.setVimServerRef(vimServerRef);

		// creating an isolated vapp network
		NetworkConfigurationType networkConfiguration = new NetworkConfigurationType();
		networkConfiguration.setFenceMode(FenceModeValuesType.ISOLATED.value());
		IpScopeType ipScope = new IpScopeType();
		ipScope.setNetmask("255.255.255.0");
		ipScope.setGateway("192.168.111.254");
		ipScope.setDns1("1.2.3.4");
		ipScope.setDnsSuffix("sample.vmware.com");
		ipScope.setIsInherited(false);

		IpScopesType ipScopes = new IpScopesType();
		ipScopes.getIpScope().add(ipScope);

		// IP Ranges
		IpRangesType ipRangesType = new IpRangesType();
		IpRangeType ipRangeType = new IpRangeType();
		ipRangeType.setStartAddress("192.168.111.1");
		ipRangeType.setEndAddress("192.168.111.19");
		ipRangesType.getIpRange().add(ipRangeType);
		ipScope.setIpRanges(ipRangesType);
		networkConfiguration.setIpScopes(ipScopes);
		vmwExternalNetworkType.setConfiguration(networkConfiguration);
		vmwExternalNetworkType.setVimPortGroupRef(vimObjRef);

		return vmwExternalNetworkType;
	}

	/**
	 * Updates External Network
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * 
	 * @return VMWExternalNetworkType
	 * @throws VCloudException
	 */
	private static VMWExternalNetworkType updateExternalNetworkParams(
			ReferenceType vimServerRef, String moRef, String externalNetworkName)
			throws VCloudException {
		VMWExternalNetworkType vmwExternalNetworkType = new VMWExternalNetworkType();
		vmwExternalNetworkType.setName(externalNetworkName + "_Updated");
		vmwExternalNetworkType
				.setDescription("Updated description for external network");

		VimObjectRefType vimObjRef = new VimObjectRefType();
		vimObjRef.setMoRef(moRef);
		vimObjRef.setVimObjectType(portGroupType);
		vimObjRef.setVimServerRef(vimServerRef);
		// creating an isolated vapp network
		NetworkConfigurationType networkConfiguration = new NetworkConfigurationType();
		networkConfiguration.setFenceMode(FenceModeValuesType.ISOLATED.value());
		IpScopeType ipScope = new IpScopeType();
		ipScope.setNetmask("255.255.255.0");
		ipScope.setGateway("192.168.111.254");
		ipScope.setDns1("1.2.3.4");
		ipScope.setDnsSuffix("sample.vmware.com");
		ipScope.setIsInherited(false);

		IpScopesType ipScopes = new IpScopesType();
		ipScopes.getIpScope().add(ipScope);

		// IP Ranges
		IpRangesType ipRangesType = new IpRangesType();
		IpRangeType ipRangeType = new IpRangeType();
		ipRangeType.setStartAddress("192.168.111.1");
		ipRangeType.setEndAddress("192.168.111.19");
		ipRangesType.getIpRange().add(ipRangeType);
		ipScope.setIpRanges(ipRangesType);
		networkConfiguration.setIpScopes(ipScopes);
		vmwExternalNetworkType.setConfiguration(networkConfiguration);
		vmwExternalNetworkType.setVimPortGroupRef(vimObjRef);

		return vmwExternalNetworkType;
	}

	/**
	 * Get Vim Server Reference
	 * 
	 * @param vimServerName
	 *            {@link String}
	 * 
	 * @return ReferenceType
	 * @throws VCloudException
	 */
	private static ReferenceType getVimServerRef(String vimServerName)
			throws VCloudException {
		return client.getVcloudAdminExtension().getVMWVimServerRefsByName()
				.get(vimServerName);
	}

	/**
	 * Get Port Group Moref
	 * 
	 * @param portGroup
	 *            {@link String}
	 * @param vimServerName
	 *            {@link String}
	 * 
	 * @return {@link String}
	 * @throws VCloudException
	 */
	private static String getPortGroupMoref(String portGroup,
					String vimServerName)
			throws VCloudException {
		String moref = "";

		Expression portGroupNameExpression = new Expression(
				QueryPortgroupField.NAME, portGroup,
				ExpressionType.EQUALS);
		Expression vcNameExpression = new Expression(
				QueryPortgroupField.VCNAME, vimServerName,
				ExpressionType.EQUALS);

		List<Expression> expressions = new ArrayList<Expression>();
		expressions.add(portGroupNameExpression);
		expressions.add(vcNameExpression);

		Filter filter = new Filter(FilterType.AND, expressions);

		QueryParams<QueryPortgroupField> queryParams = new QueryParams<QueryPortgroupField>();
		queryParams.setFilter(filter);

		QueryService queryService = client.getQueryService();
		RecordResult<QueryResultPortgroupRecordType> portGroupResult = queryService
				.queryRecords(QueryRecordType.PORTGROUP, queryParams);
		if (portGroupResult.getRecords().size() > 0) {
			moref = portGroupResult.getRecords().get(0).getMoref();
			portGroupType = portGroupResult.getRecords().get(0).getPortgroupType();
		}
		else {
			System.err.println("Port Group " + portGroup
				+ " not found in vc " + vimServerName);
		}

		return moref;
	}

	/**
	 * Main method, which does Creating, Updating, Getting and Deleting External
	 * Network
	 * 
	 * @param args
	 * 
	 * @throws VCloudExceptio
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws VCloudException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, TimeoutException {

		if (args.length < 6)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud Login");
		client = new VcloudClient(args[0], Version.V5_1);

		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);

		System.out.println("Login Success\n");

		System.out.println("Create External Network");
		ReferenceType vimServerRef = getVimServerRef(args[3]);
		String portGroupMoref = getPortGroupMoref(args[4], args[3]);

		VMWExternalNetworkType vmwExternalNetwork = createExternalNetworkParams(
				vimServerRef, portGroupMoref, args[5]);
		VMWExternalNetwork externalNetwork = client.getVcloudAdminExtension()
				.createVMWExternalNetwork(vmwExternalNetwork);
		Task externalNetworkTask = returnTask(externalNetwork);
		if (externalNetworkTask != null)
			externalNetworkTask.waitForTask(0);
		System.out.println("External Network Created");
		System.out.println("	External Network:	"
				+ externalNetwork.getResource().getName());

		System.out.println("Update External Network");
		VMWExternalNetworkType vmwExternalNetworkForUpdate = updateExternalNetworkParams(
				vimServerRef, portGroupMoref, args[5]);
		externalNetwork = externalNetwork.updateVMWExternalNetwork(vmwExternalNetworkForUpdate);
		if(externalNetwork.getTasks().size()>0){ 
		    externalNetwork.getTasks().get(0).waitForTask(0);
		}
		System.out.println("External Network Updated");
		System.out.println("	Updated External Network:	"
				+ externalNetwork.getResource().getName());

		System.out.println("Get External Network");
		System.out.println("	"+VMWExternalNetwork.getVMWExternalNetworkByReference(
				client, externalNetwork.getReference())
				.getResource().getName());

		System.out.println("Delete External Network");
		externalNetwork.delete().waitForTask(0);
		System.out.println("External Network deleted");
	}
}