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
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.CatalogType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Catalog;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * This sample lists the Catalog of the vCloud Inventory
 *
 * @author Ecosystem Engineering
 *
 */

public class CatalogInventorySample {

	public VcloudClient vcloudClient;
	public HashMap<String, ReferenceType> organizationsMap;

	/**
	 * Login to the API with credentials.
	 *
	 * @param vCloudURL
	 * @param username
	 * @param password
	 * @throws IOException
	 * @throws VCloudException
	 * @throws HttpException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	public CatalogInventorySample(String vCloudURL, String username,
			String password) throws HttpException, VCloudException,
			IOException, KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException {
		VcloudClient.setLogLevel(Level.OFF);
		vcloudClient = new VcloudClient(vCloudURL, Version.V5_1);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(username, password);
		organizationsMap = vcloudClient.getOrgRefsByName();
		if (organizationsMap.isEmpty()) {
			System.out.println("Try Logging in with valid details");
			System.exit(0);
		}
	}

	/**
	 * Lists the vCloud Inventory as
	 *
	 * Organization Catalog CatalogItem
	 *
	 * @throws VCloudException
	 */
	public void listInventory() throws VCloudException {
		System.out.println("Organization					Catalog						CatalogItem");
		System.out.println("------------        				-------						-----------");
		System.out.println();
		if (!organizationsMap.isEmpty()) {
			for (String organizationName : organizationsMap.keySet()) {
				ReferenceType organizationReference = organizationsMap
						.get(organizationName);
				System.out.print(organizationName);
				System.out.println();
				System.out.println(organizationReference.getHref());
				Organization organization = Organization
						.getOrganizationByReference(vcloudClient,
								organizationReference);
				Collection<ReferenceType> catalogLinks = organization
						.getCatalogRefs();
				if (!catalogLinks.isEmpty()) {
					for (ReferenceType catalogLink : catalogLinks) {
						Catalog catalog = Catalog.getCatalogByReference(
								vcloudClient, catalogLink);
						CatalogType catalogParams = catalog.getResource();
						System.out.print("						" + catalogParams.getName());
						System.out.println();
						System.out.println("						" + catalogLink.getHref());
						Collection<ReferenceType> catalogItemReferences = catalog
								.getCatalogItemReferences();
						if (!catalogItemReferences.isEmpty()) {
							for (ReferenceType catalogItemReference : catalogItemReferences) {
								System.out.print("												"
										+ catalogItemReference.getName());
								System.out.println();
								System.out.println("												"
										+ catalogItemReference.getHref());
							}
							System.out.println();
						} else
							System.out
									.println("												No CatalogItems Found");
					}
					System.out.println();
				} else
					System.out.println("						No Catalogs Found");
			}
		} else {
			System.out.println("No Organizations");
			System.exit(0);
		}
	}

	/**
	 * CatalogInventorySample Program Usage
	 */
	public static void getUsage() {
		System.out
				.println("java CatalogInventorySample vCloudURL user@vcloud-organization password");
		System.out
				.println("java CatalogInventorySample https://vcloud user@vcloud-organization password");
		System.exit(0);
	}

	/**
	 * Starting method for the CatalogInventorySample
	 *
	 * @param args
	 * @throws IOException
	 * @throws HttpException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */

	public static void main(String args[]) throws HttpException, IOException,
			KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException {

		if (args.length < 3)
			CatalogInventorySample.getUsage();
		try {
			CatalogInventorySample catalogInventorySample = new CatalogInventorySample(
					args[0], args[1], args[2]);
			catalogInventorySample.listInventory();
		} catch (VCloudException e) {
			System.out.println(e.getMessage());
		}

	}

}
