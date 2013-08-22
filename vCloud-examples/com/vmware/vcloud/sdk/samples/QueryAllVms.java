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
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.QueryResultAdminVMRecordType;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.RecordResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.ExtensionQueryService;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.constants.query.QueryAdminVMField;
import com.vmware.vcloud.sdk.constants.query.QueryRecordType;

/**
 *
 * Query Service Sample.
 *
 * This samples queries all the vm's and its information from the vCloud. This
 * sample can be run only by a system admin.
 *
 * @author Ecosystem Engineering.
 */

public class QueryAllVms {

	private static VcloudClient vcloudClient;
	private static VcloudAdminExtension adminExtension;
	private static ExtensionQueryService queryService;
	private static int MAX_PAGE_SIZE = 128;
	private static int noOfVms;

	/**
	 * Query all the vms.
	 *
	 * @throws VCloudException
	 */
	public static void QueryAllVMs() throws VCloudException {

		System.out.println("Query All VM's");
		System.out.println("--------------");

		QueryParams<QueryAdminVMField> vmQueryParams = new QueryParams<QueryAdminVMField>();
		vmQueryParams.setPageSize(MAX_PAGE_SIZE);

		RecordResult<QueryResultAdminVMRecordType> vmRecordResult = queryService
				.queryRecords(QueryRecordType.ADMINVM, vmQueryParams);

		while (vmRecordResult.getRecords().size() > 0) {
			displayVms(vmRecordResult);
			try {
				vmRecordResult = vmRecordResult.getNextPage();
			} catch (VCloudException e) {
				break;
			}
		}
		System.out.println("\nTotal No of VM's - " + noOfVms);

	}

	/**
	 * Display some of the VM's information.
	 *
	 * @param vmRecordResult
	 */
	public static void displayVms(
			RecordResult<QueryResultAdminVMRecordType> vmRecordResult) {
		for (QueryResultAdminVMRecordType vmRecord : vmRecordResult
				.getRecords()) {
			noOfVms++;
			System.out.println("VMName : " + vmRecord.getName() + " VMMoref : "
					+ vmRecord.getMoref());
		}
	}

	/**
	 * Start
	 *
	 * @param args
	 * @throws KeyManagementException
	 * @throws UnrecoverableKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws VCloudException
	 */
	public static void main(String args[]) throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException, VCloudException {

		if (args.length < 3) {
			System.out
					.println("java QueryAllVMs vCloudURL user@organization password");
			System.out
					.println("java QueryAllVMs https://vcloud user@System password");
			System.exit(0);
		}

		// Client login
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(args[0], Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(args[1], args[2]);

		// Getting the VcloudAdminExtension
		adminExtension = vcloudClient.getVcloudAdminExtension();

		// Getting the Admin Extension Query Service.
		queryService = adminExtension.getExtensionQueryService();

		// Query All the vms and its information.
		QueryAllVMs();
	}
}
