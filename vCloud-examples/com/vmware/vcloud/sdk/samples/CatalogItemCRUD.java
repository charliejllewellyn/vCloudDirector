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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.CatalogItemType;
import com.vmware.vcloud.api.rest.schema.MediaType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.sdk.Catalog;
import com.vmware.vcloud.sdk.CatalogItem;
import com.vmware.vcloud.sdk.Media;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.ImageType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Create, Update & Delete a CatalogItem. The input vAppTemplate should not be
 * part of any other catalog.
 *
 * @author Ecosystem Engineering
 *
 */
public class CatalogItemCRUD {

	private static VcloudClient client;

	/**
	 * Create and empty media and return its reference.
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static ReferenceType createMediaRef(String orgName, String vdcName,
			String newMediaName) throws VCloudException {
		ReferenceType orgRef = client.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(client,
				orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		Vdc vdc = Vdc.getVdcByReference(client, vdcRef);
		MediaType mediaParams = new MediaType();
		mediaParams.setName(newMediaName);
		mediaParams.setDescription("ee media testing desc");
		mediaParams.setImageType(ImageType.ISO.value());
		mediaParams.setSize(Long.parseLong("23565312"));
		Media media = vdc.createMedia(mediaParams);
		return media.getReference();
	}

	/**
	 * Constructing a New CatalogItem
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static CatalogItemType getCatalogItem(String orgName,
			String vdcName, String newMediaName) throws VCloudException {

		CatalogItemType catalogItem = new CatalogItemType();
		catalogItem.setName("ee catalog item");
		catalogItem.setDescription("ee catalog item description");
		catalogItem
				.setEntity(createMediaRef(orgName, vdcName, newMediaName));
		return catalogItem;
	}

	/**
	 * Search the catalog
	 *
	 * @param orgName
	 * @param catalogName
	 * @return
	 * @throws VCloudException
	 */
	private static Catalog searchCatalog(String orgName, String catalogName)
			throws VCloudException {
		ReferenceType orgRef = client.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(client,
				orgRef);
		ReferenceType catalogRef = null;
		for (ReferenceType ref : org.getCatalogRefs()) {
			if (ref.getName().equals(catalogName))
				catalogRef = ref;
		}
		return Catalog.getCatalogByReference(client, catalogRef);
	}

	/**
	 * Check for tasks if any
	 *
	 * @param catalogItem
	 * @return {@link Task}
	 * @throws VCloudException
	 */
	public static Task returnTask(CatalogItem catalogItem)
			throws VCloudException {
		TasksInProgressType tasksInProgress = ((CatalogItemType) catalogItem
				.getResource()).getTasks();
		if (tasksInProgress != null)
			for (TaskType task : tasksInProgress.getTask()) {
				return new Task(client, task);
			}
		return null;
	}

	/**
	 * Sample Usage
	 */
	public static void usage() {
		System.out
				.println("java CatalogItemCRUD VcloudUrl Username@vcloud-organization Password orgName CatalogName vdcName newMediaName");
		System.out
				.println("java CatalogItemCRUD https://vcloud user@Orgnanization password orgName catalogName vdcName newMediaName");
		System.exit(0);
	}

	/**
	 * Main method, which does Adding, Updating and Deleting Catalog
	 *
	 * @param args
	 * @throws HttpException
	 * @throws SecurityException
	 * @throws FileNotFoundException
	 * @throws VCloudException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws TimeoutException
	 */
	public static void main(String args[]) throws HttpException,
			SecurityException, FileNotFoundException, VCloudException,
			IOException, KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException, TimeoutException {

		if (args.length < 7)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud Login");
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.login(args[1], args[2]);
		System.out.println("	Login Success\n");

		System.out.println("Get Catalog");
		Catalog catalog = searchCatalog(args[3], args[4]);
		System.out.println("	" + catalog.getResource().getHref() + "\n");

		System.out.println("Add CatalogItem");
		CatalogItem catalogItem = catalog.addCatalogItem(getCatalogItem(
				args[3], args[5], args[6]));
		Task task = returnTask(catalogItem);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + catalogItem.getResource().getHref() + "\n");

		System.out.println("Update CatalogItem");
		CatalogItem updatedCatalogItem = catalogItem
				.updateCatalogItem(((CatalogItemType) catalogItem.getResource()));
		task = returnTask(updatedCatalogItem);
		if (task != null)
			task.waitForTask(0);
		System.out.println("	" + updatedCatalogItem.getResource().getHref()
				+ "\n");

		System.out.println("Delete CatalogItem");
		updatedCatalogItem.delete();
		System.out.println("	Deleted");

	}
}
