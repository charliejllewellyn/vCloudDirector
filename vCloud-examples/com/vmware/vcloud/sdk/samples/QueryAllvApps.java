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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;

import com.vmware.vcloud.api.rest.schema.QueryResultAdminVAppRecordType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Expression;
import com.vmware.vcloud.sdk.Filter;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.RecordResult;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.extensions.ExtensionQueryService;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloud.sdk.constants.query.ExpressionType;
import com.vmware.vcloud.sdk.constants.query.QueryAdminVAppField;
import com.vmware.vcloud.sdk.constants.query.QueryRecordType;
import com.vmware.vcloud.sdk.constants.query.QueryReferenceType;
import com.vmware.vcloud.sdk.constants.query.SortType;

/**
 *
 * Query Service Sample.
 *
 * This samples queries all the vapps in the vCloud. This sample can be run only
 * by a system admin. Querying here is done in two different ways.
 *
 * 1. General Query - Fetching results in two formats (references and records).
 *
 * 2. Specialized Query - Fetching results in two formats (references and
 * records).
 *
 * @author Ecosystem Engineering.
 */

public class QueryAllvApps {

	private static VcloudClient vcloudClient;
	private static VcloudAdminExtension adminExtension;
	private static ExtensionQueryService queryService;

	/**
	 * General Query - Fetching results in references format.
	 *
	 * @throws VCloudException
	 */
	public static void generalizedQueryReferences() throws VCloudException {

		// a. General Query without any query params.
		System.out.println("Generalized Query - References Format");
		System.out.println("-------------------------------------");
		// Execute the query with the query type.
		// This is the query that gets created -
		// https://cloud/api/query?type=adminVAppAll&format=references
		ReferenceResult referenceResult = queryService
				.queryReferences(QueryReferenceType.ADMINVAPP);
		for (ReferenceType vappReference : referenceResult.getReferences()) {
			System.out.println("	" + vappReference.getName() + " - "
					+ vappReference.getHref());
		}

		// b. General Query with query params. These query params can be page
		// no, page size, the fields to be retrieved, sorting, offset, filter
		// etc.

		// Creating a linked hash map of sort fields.
		LinkedHashMap<QueryAdminVAppField, SortType> sortFields = new LinkedHashMap<QueryAdminVAppField, SortType>();
		sortFields.put(QueryAdminVAppField.NAME, SortType.SORT_ASC);

		// Creating a collection of fields.
		Set<QueryAdminVAppField> fields = new HashSet<QueryAdminVAppField>();
		fields.add(QueryAdminVAppField.NAME);

		// Creating a filter for the query.
		Expression expression = new Expression(QueryAdminVAppField.NAME, "*",
				ExpressionType.EQUALS);
		Filter filter = new Filter(expression);

		// Creating the Query Params and add them to it.
		QueryParams<QueryAdminVAppField> queryParams = new QueryParams<QueryAdminVAppField>();
		queryParams.setPageSize(100);
		queryParams.setPage(1);
		queryParams.setFilter(filter);
		queryParams.setFields(fields);
		queryParams.setSortFields(sortFields);

		// Execute the query with the query type and the query params.
		// This is the query that gets created -
		// https://cloud/api/query?type=adminVAppAll&sortAsc=name&fields=name&page=1&pageSize=100&filter=(name==*)&format=references
		referenceResult = queryService.queryReferences(
				QueryReferenceType.ADMINVAPP, queryParams);
		System.out.println("Generalized Query with params - References Format");
		System.out.println("-------------------------------------------------");
		for (ReferenceType vappReference : referenceResult.getReferences()) {
			System.out.println("	" + vappReference.getName() + " - "
					+ vappReference.getHref());
		}
		System.out.println();
	}

	/**
	 * Specialized Query - Fetching results in references format.
	 *
	 * @throws VCloudException
	 */
	public static void specializedQueryReferences() throws VCloudException {

		// a. Specialized Query without any query params.
		System.out.println("Specialized Query - References format");
		System.out.println("-------------------------------------");
		// Execute the query with the query type.
		// This is the query that gets created -
		// https://cloud/api/admin/extension/vapps/query?&format=references
		ReferenceResult referenceResult = queryService.queryAllVappReferences();
		for (ReferenceType vappReference : referenceResult.getReferences()) {
			System.out.println("	" + vappReference.getName() + " - "
					+ vappReference.getHref());
		}

		// b. Specialized Query with query params. These query params can be
		// page no,page size, the fields to be retrieved, sorting, offset,
		// filter etc.

		// Creating a linked hash map of sort fields.
		LinkedHashMap<QueryAdminVAppField, SortType> sortFields = new LinkedHashMap<QueryAdminVAppField, SortType>();
		sortFields.put(QueryAdminVAppField.NAME, SortType.SORT_ASC);

		// Creating a collection of fields.
		Set<QueryAdminVAppField> fields = new HashSet<QueryAdminVAppField>();
		fields.add(QueryAdminVAppField.NAME);

		// Creating a filter for the query.
		Expression expression = new Expression(QueryAdminVAppField.NAME, "*",
				ExpressionType.EQUALS);
		Filter filter = new Filter(expression);

		// Creating the Query Params and add them to it.
		QueryParams<QueryAdminVAppField> queryParams = new QueryParams<QueryAdminVAppField>();
		queryParams.setPageSize(100);
		queryParams.setPage(1);
		queryParams.setFilter(filter);
		queryParams.setFields(fields);
		queryParams.setSortFields(sortFields);

		// Execute the query with the query type and the query params.
		// This is the query that gets created -
		// https://cloud/api/admin/extension/vapps/query?&sortAsc=name&fields=name&page=1&pageSize=100&filter=(name==*)&format=references
		referenceResult = queryService.queryAllVappReferences(queryParams);
		System.out.println("Specialized Query with params - References Format");
		System.out.println("-------------------------------------------------");
		for (ReferenceType vappReference : referenceResult.getReferences()) {
			System.out.println("	" + vappReference.getName() + " - "
					+ vappReference.getHref());
		}

		System.out.println();

	}

	/**
	 * General Query - Fetching results in Records format.
	 *
	 * @throws VCloudException
	 */
	@SuppressWarnings("unchecked")
	public static void generalizedQueryRecords() throws VCloudException {

		// a. General Query without any query params.
		System.out.println("Generalized Query - Records Format");
		System.out.println("----------------------------------");
		// Execute the query with the query type.
		// This is the query that gets created -
		// https://cloud/api/query?type=adminVAppAll&format=records
		RecordResult<QueryResultAdminVAppRecordType> recordResult = queryService
				.queryRecords(QueryRecordType.ADMINVAPP);
		for (QueryResultAdminVAppRecordType vappRecord : recordResult
				.getRecords()) {
			System.out.println("	Name : " + vappRecord.getName());
			System.out.println("		Created Date : "
					+ vappRecord.getCreationDate());
			System.out.println("		CPU Mhz : "
					+ vappRecord.getCpuAllocationMhz());
			System.out.println("		Memory Mb : "
					+ vappRecord.getMemoryAllocationMB());
		}
		// b. General Query with query params. These query params can be page
		// no, page size, the fields to be retrieved, sorting, offset, filter
		// etc.

		// Creating a linked hash map of sort fields.
		LinkedHashMap<QueryAdminVAppField, SortType> sortFields = new LinkedHashMap<QueryAdminVAppField, SortType>();
		sortFields.put(QueryAdminVAppField.NAME, SortType.SORT_ASC);

		// Creating a collection of fields.
		Set<QueryAdminVAppField> fields = new HashSet<QueryAdminVAppField>();
		fields.add(QueryAdminVAppField.NAME);

		// Creating a filter for the query.
		Expression expression = new Expression(QueryAdminVAppField.NAME, "*",
				ExpressionType.EQUALS);
		Filter filter = new Filter(expression);

		// Creating the Query Params and add them to it.
		QueryParams<QueryAdminVAppField> queryParams = new QueryParams<QueryAdminVAppField>();
		queryParams.setPageSize(100);
		queryParams.setPage(1);
		queryParams.setFilter(filter);
		queryParams.setSortFields(sortFields);

		// Execute the query with the query type and the query params.
		// This is the query that gets created -
		// https://cloud/api/query?type=adminVAppAll&sortAsc=name&fields=name&page=1&pageSize=100&filter=(name==*)&format=records
		recordResult = queryService.queryRecords(QueryRecordType.ADMINVAPP,
				queryParams);
		System.out.println("Generalized Query with params - Records Format");
		System.out.println("----------------------------------------------");
		for (QueryResultAdminVAppRecordType vappRecord : recordResult
				.getRecords()) {
			System.out.println("	Name : " + vappRecord.getName());
			System.out.println("		Created Date : "
					+ vappRecord.getCreationDate());
			System.out.println("		CPU Mhz : "
					+ vappRecord.getCpuAllocationMhz());
			System.out.println("		Memory Mb : "
					+ vappRecord.getMemoryAllocationMB());
		}
		System.out.println();
	}

	/**
	 * Specialized Query - Fetching results in records format.
	 *
	 * @throws VCloudException
	 */
	public static void specializedQueryRecords() throws VCloudException {

		// a. Specialized Query without any query params.
		System.out.println("Specialized Query - Records format");
		System.out.println("----------------------------------");
		// Execute the query with the query type.
		// This is the query that gets created -
		// https://cloud/api/admin/extension/vapps/query?&format=records
		RecordResult<QueryResultAdminVAppRecordType> recordResult = queryService
				.queryAllVappRecords();
		for (QueryResultAdminVAppRecordType vappRecord : recordResult
				.getRecords()) {
			System.out.println("	Name : " + vappRecord.getName());
			System.out.println("		Created Date : "
					+ vappRecord.getCreationDate());
			System.out.println("		CPU Mhz : "
					+ vappRecord.getCpuAllocationMhz());
			System.out.println("		Memory Mb : "
					+ vappRecord.getMemoryAllocationMB());
		}

		// b. Specialized Query with query params. These query params can be
		// page no,page size, the fields to be retrieved, sorting, offset,
		// filter etc.

		// Creating a linked hash map of sort fields.
		LinkedHashMap<QueryAdminVAppField, SortType> sortFields = new LinkedHashMap<QueryAdminVAppField, SortType>();
		sortFields.put(QueryAdminVAppField.NAME, SortType.SORT_ASC);

		// Creating a collection of fields.
		Set<QueryAdminVAppField> fields = new HashSet<QueryAdminVAppField>();
		fields.add(QueryAdminVAppField.NAME);

		// Creating a filter for the query.
		Expression expression = new Expression(QueryAdminVAppField.NAME, "*",
				ExpressionType.EQUALS);
		Filter filter = new Filter(expression);

		// Creating the Query Params and add them to it.
		QueryParams<QueryAdminVAppField> queryParams = new QueryParams<QueryAdminVAppField>();
		queryParams.setPageSize(100);
		queryParams.setPage(1);
		queryParams.setFilter(filter);
		queryParams.setSortFields(sortFields);

		// Execute the query with the query type and the query params.
		// This is the query that gets created -
		// https://cloud/api/admin/extension/vapps/query?&sortAsc=name&fields=name&page=1&pageSize=100&filter=(name==*)&format=records
		recordResult = queryService.queryAllVappRecords(queryParams);
		System.out.println("Specialized Query with params - Records Format");
		System.out.println("----------------------------------------------");
		for (QueryResultAdminVAppRecordType vappRecord : recordResult
				.getRecords()) {
			System.out.println("	Name : " + vappRecord.getName());
			System.out.println("		Created Date : "
					+ vappRecord.getCreationDate());
			System.out.println("		CPU Mhz : "
					+ vappRecord.getCpuAllocationMhz());
			System.out.println("		Memory Mb : "
					+ vappRecord.getMemoryAllocationMB());
		}
		System.out.println();

	}

	public static void main(String args[]) throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException, VCloudException {

		if (args.length < 3) {
			System.out
					.println("java QueryAllvApps vCloudURL user@organization password");
			System.out
					.println("java QueryAllvApps https://vcloud user@System password");
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

		// Two different ways to get the same data.
		// 1. The Generalized Query - References Format.
		generalizedQueryReferences();
		// The Generalized Query - Records Format.
		generalizedQueryRecords();
		// 2. The Specialized Query - - References Format.
		specializedQueryReferences();
		// The Specialized Query - - Records Format.
		specializedQueryRecords();
	}
}
