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

import com.vmware.vcloud.api.rest.schema.QueryResultDvSwitchRecordType;
import com.vmware.vcloud.api.rest.schema.QueryResultPortgroupRecordType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.extension.FencePoolType;
import com.vmware.vcloud.api.rest.schema.extension.NumericRangeType;
import com.vmware.vcloud.api.rest.schema.extension.PortGroupPoolType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefType;
import com.vmware.vcloud.api.rest.schema.extension.VimObjectRefsType;
import com.vmware.vcloud.api.rest.schema.extension.VlanPoolType;
import com.vmware.vcloud.sdk.Expression;
import com.vmware.vcloud.sdk.Filter;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.QueryService;
import com.vmware.vcloud.sdk.RecordResult;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.VMWNetworkPool;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.constants.VimObjectTypeEnum;
import com.vmware.vcloud.sdk.constants.query.ExpressionType;
import com.vmware.vcloud.sdk.constants.query.FilterType;
import com.vmware.vcloud.sdk.constants.query.QueryDvSwitchField;
import com.vmware.vcloud.sdk.constants.query.QueryPortgroupField;
import com.vmware.vcloud.sdk.constants.query.QueryRecordType;

/**
 * Creating, Getting, Updating and Deleting Network Pool
 * 
 * For Portgroup-backed network pool:
	* Verify that one or more port groups are available in vSphere.
	* The port groups must be available on each ESX/ESXi host in the cluster,
	* and each port group must use only a single VLAN.
	* Port groups with VLAN trunking are not supported.
 * For Isolation-backed network pool:
	* Verify that a vSphere distributed switch is available.
 * For Vlan-backed network pool:
	* Verify that a range of VLAN IDs and a vSphere distributed switch
	* are available in vSphere.
	* The VLAN IDs must be valid IDs that are configured in the physical switch
	* to which the ESX/ESXi servers are connected.
 * 
 * @author Ecosystem Engineering
 * 
 */

public class NetworkPoolCRUD {

	private static VcloudClient client;
	private static String portGroupType = "";

	/**
	 * NetworkPoolCRUD Usage
	 */
	public static void usage() {
		System.out
				.println("java NetworkPoolCRUD Vcloud user@organization password VimServerName PortGroupName dvSwitchName NetworkPoolName");
		System.out
				.println("java NetworkPoolCRUD https://vcloud username@Organization password vimservername portgroupname dvswitchname networkpoolname");
		System.exit(0);
	}

	/**
	 * Gets VimServer Reference
	 * 
	 * @param vimServerName
	 *            {@link String}
	 * @return {@link ReferenceType}
	 * 
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
	 * Get dvSwitch Moref
	 * 
	 * @param dvSwitchName
	 *            {@link String}
	 * @param vimServerName
	 *            {@link String}
	 * 
	 * @return {@link String}
	 * @throws VCloudException
	 */
	private static String getDVSwitchMoref(String dvSwitchName,
					String vimServerName)
			throws VCloudException {
		String moref = "";

		Expression dvSwitchExpression = new Expression(
				QueryDvSwitchField.NAME, dvSwitchName,
				ExpressionType.EQUALS);
		Expression vcNameExpression = new Expression(
				QueryDvSwitchField.VCNAME, vimServerName,
				ExpressionType.EQUALS);

		List<Expression> expressions = new ArrayList<Expression>();
		expressions.add(dvSwitchExpression);
		expressions.add(vcNameExpression);

		Filter filter = new Filter(FilterType.AND, expressions);

		QueryParams<QueryDvSwitchField> queryParams = new QueryParams<QueryDvSwitchField>();
		queryParams.setFilter(filter);

		QueryService queryService = client.getQueryService();

		RecordResult<QueryResultDvSwitchRecordType> dvSwitchResult = queryService
				.queryRecords(QueryRecordType.DVSWITCH, queryParams);
		if (dvSwitchResult.getRecords().size() > 0) {
			moref = dvSwitchResult.getRecords().get(0).getMoref();
		}
		else {
			System.err.println("Distributed Switch " + dvSwitchName
				+ " not found in vc " + vimServerName);
		}

		return moref;
	}

	/**
	 * Check for tasks if any
	 * 
	 * @param netPool
	 *            {@link VMWNetworkPool}
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(VMWNetworkPool netPool)
			throws VCloudException {
		TasksInProgressType tasksInProgress = netPool.getResource().getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Create params for portgroup-backed network pool
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * @return PortGroupPoolType
	 * @throws VCloudException
	 */
	private static PortGroupPoolType createPortGrpVMWNetworkPoolParams(
			ReferenceType vimServerRef, String moRef, String networkPoolName)
			throws VCloudException {
		VimObjectRefType vimObjRef = new VimObjectRefType();
		vimObjRef.setMoRef(moRef);
		vimObjRef.setVimObjectType(portGroupType);
		vimObjRef.setVimServerRef(vimServerRef);

		VimObjectRefsType vimObjRefs = new VimObjectRefsType();
		vimObjRefs.getVimObjectRef().add(vimObjRef);

		PortGroupPoolType portGrpPool = new PortGroupPoolType();
		portGrpPool.setName(networkPoolName + "_PortGroup_Type");
		portGrpPool.setPortGroupRefs(vimObjRefs);
		portGrpPool.setVimServer(vimServerRef);

		return portGrpPool;
	}

	/**
	 * Creates params for isolation-backed network pool
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * @return FencePoolType
	 * @throws VCloudException
	 */
	private static FencePoolType createIsolationBakedVMWNetworkPoolParams(
			ReferenceType vimServerRef, String moRef, String networkPoolName)
			throws VCloudException {
		FencePoolType fencePool = new FencePoolType();
		fencePool.setName(networkPoolName + "_FencePool_Type");
		fencePool.setDescription("Some description");
		fencePool.setFenceIdCount(12);
		fencePool.setVlanId(0);

		VimObjectRefType vimObject = new VimObjectRefType();
		vimObject.setMoRef(moRef);
		vimObject.setVimObjectType(VimObjectTypeEnum.NETWORK.value());
		vimObject.setVimServerRef(vimServerRef);
		fencePool.setVimSwitchRef(vimObject);

		return fencePool;
	}

	/**
	 * Creates params for vlan-backed network pool
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * @return VlanPoolType
	 * @throws VCloudException
	 */
	private static VlanPoolType createVlanVMWNetworkPoolParams(
			ReferenceType vimServerRef, String moRef, String networkPoolName)
			throws VCloudException {
		NumericRangeType numericRange = new NumericRangeType();
		numericRange.setStart(1);
		numericRange.setEnd(5);

		VlanPoolType vlanPool = new VlanPoolType();
		vlanPool.setName(networkPoolName + "_Vlan_Type");
		vlanPool.setDescription("Some description");
		VimObjectRefType vimObject = new VimObjectRefType();
		vimObject.setMoRef(moRef);
		vimObject.setVimObjectType(VimObjectTypeEnum.NETWORK.value());
		vimObject.setVimServerRef(vimServerRef);
		vlanPool.setVimSwitchRef(vimObject);
		vlanPool.getVlanRange().add(numericRange);

		return vlanPool;
	}

	/**
	 * Updates Network Pool
	 * 
	 * @param vimServerRef
	 *            {@link ReferenceType}
	 * @param moRef
	 *            {@link String}
	 * @return VlanPoolType
	 * @throws VCloudException
	 */
	private static VlanPoolType updateNetworkPool(ReferenceType vimServerRef,
			String moRef, String networkPoolName) throws VCloudException {
		NumericRangeType numericRange = new NumericRangeType();
		numericRange.setStart(1);
		numericRange.setEnd(5);

		VlanPoolType vlanPool = new VlanPoolType();
		vlanPool.setName(networkPoolName + "_Vlan_Type_Updated");
		vlanPool.setDescription("Updated description");
		VimObjectRefType vimObject = new VimObjectRefType();
		vimObject.setMoRef(moRef);
		vimObject.setVimObjectType(VimObjectTypeEnum.NETWORK.value());
		vimObject.setVimServerRef(vimServerRef);
		vlanPool.setVimSwitchRef(vimObject);
		vlanPool.getVlanRange().add(numericRange);

		return vlanPool;
	}

	/**
	 * Main method, which does Creating, Getting, Updating and Deleting
	 * ProviderVdc
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

		ReferenceType vimServerRef = getVimServerRef(args[3]);
		String portGroupMoref = getPortGroupMoref(args[4], args[3]);
		String dvSwitchMoref = getDVSwitchMoref(args[5], args[3]);

		System.out.println("Add Portgroup-backed Network Pool");
		PortGroupPoolType portGrpPool = createPortGrpVMWNetworkPoolParams(
				vimServerRef, portGroupMoref, args[6]);
		VMWNetworkPool portGroupNetworkPool = client.getVcloudAdminExtension()
				.createVMWNetworkPool(portGrpPool);
		Task portGrpTask = returnTask(portGroupNetworkPool);
		if (portGrpTask != null)
			portGrpTask.waitForTask(0);
		System.out.println("Portgroup-backed Network Pool Added");
		System.out.println("	Portgroup-backed Network Pool:	"
				+ portGroupNetworkPool.getResource().getName());

		System.out.println("Add Isolation-backed Network Pool");
		FencePoolType fencePool = createIsolationBakedVMWNetworkPoolParams(
				vimServerRef, dvSwitchMoref, args[6]);
		VMWNetworkPool isolationNetworkPool = client.getVcloudAdminExtension()
				.createVMWNetworkPool(fencePool);
		Task isolationTask = returnTask(isolationNetworkPool);
		if (isolationTask != null)
			isolationTask.waitForTask(0);
		System.out.println("Isolation-backed Network Pool Added");
		System.out.println("	Isolation-backed Network Pool:	"
				+ isolationNetworkPool.getResource().getName());

		System.out.println("Add Vlan-backed Network Pool");
		VlanPoolType vlanPool = createVlanVMWNetworkPoolParams(vimServerRef,
				dvSwitchMoref, args[6]);
		VMWNetworkPool vlanNetworkPool = client.getVcloudAdminExtension()
				.createVMWNetworkPool(vlanPool);
		Task vlanTask = returnTask(vlanNetworkPool);
		if (vlanTask != null)
			vlanTask.waitForTask(0);
		System.out.println("Vlan-backed Network Pool Added");
		System.out.println("	Vlan-backed Network Pool:	"
				+ vlanNetworkPool.getResource().getName());

		System.out.println("Update Vlan-backed Network Pool");
		VlanPoolType vlanPoolUpdate = updateNetworkPool(vimServerRef, dvSwitchMoref,
				args[6]);
		vlanNetworkPool = vlanNetworkPool.updateVMWNetworkPool(vlanPoolUpdate);
		if(vlanNetworkPool.getTasks().size()>0){ 
			vlanNetworkPool.getTasks().get(0).waitForTask(0);
		}
		System.out.println("Vlan-backed Network Pool Updated");
		System.out.println("	Updated Vlan-backed Network Pool:	"
				+ vlanNetworkPool.getResource().getName());

		System.out.println("Get Portgroup-backed Network Pool");
		System.out.println("	"
				+ VMWNetworkPool.getVMWNetworkPoolByReference(client,
						portGroupNetworkPool.getReference()).getResource()
						.getName());

		System.out.println("Get Isolation-backed Network Pool");
		System.out.println("	"
				+ VMWNetworkPool.getVMWNetworkPoolByReference(client,
						isolationNetworkPool.getReference()).getResource()
						.getName());

		System.out.println("Get Vlan-backed  Network Pool");
		System.out
				.println("	"
						+ VMWNetworkPool.getVMWNetworkPoolByReference(client,
								vlanNetworkPool.getReference()).getResource()
								.getName());

		System.out.println("Delete all three Network Pools");
		portGroupNetworkPool.delete().waitForTask(0);
		isolationNetworkPool.delete().waitForTask(0);
		vlanNetworkPool.delete().waitForTask(0);
		System.out.println("All Network Pools deleted");
	}
}