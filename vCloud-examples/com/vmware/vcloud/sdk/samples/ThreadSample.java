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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.MediaType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Media;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.ImageType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 *
 * This sample illustrates the MultiThreading.
 *
 * ThreadSample does upload, downloads and also does a GET onto the
 * vappTemplate.
 *
 * For this sample make sure that the downloading vappTemplate and the uploading
 * media location are on the same vdc.
 *
 * The Maximum default connections per host can be configured using
 * setMaxConnections() method. If not set defaults to 2.
 *
 *
 * @author Ecosystem Engineering
 *
 *
 */

public class ThreadSample {

	public static VcloudClient client;

	/**
	 * Upload method.
	 *
	 * @param vdcId
	 * @param fileInputStream
	 * @param fileSize
	 * @throws VCloudException
	 * @throws FileNotFoundException
	 */
	public void uploadMediaFile(Vdc vdc, InputStream fileInputStream,
			long fileSize) throws VCloudException, FileNotFoundException {
		class upload extends Thread {
			Vdc vdc;
			long fileSize;
			InputStream fileInputStream;

			public upload(Vdc vdc, InputStream fileInputStream, long fileSize) {
				this.fileInputStream = fileInputStream;
				this.fileSize = fileSize;
				this.vdc = vdc;
				start();
			}

			public void run() {
				try {
					System.out.println("Upload Thread ");

					MediaType mediaParams = new MediaType();
					mediaParams.setName("HelloVcloudMedia");
					mediaParams.setDescription("HelloVcloudMedia Description");
					mediaParams.setSize(Long.valueOf(fileSize));
					mediaParams.setImageType(ImageType.ISO.value());

					vdc = Vdc.getVdcByReference(client, vdc.getReference());
					Media newMedia = vdc.createMedia(mediaParams);
					System.out.println("	Upload Started");
					newMedia.uploadFile("file", fileInputStream, fileSize);

					while (newMedia.getResource().getStatus() != 1) {
						Thread.sleep(5000);
						newMedia = Media.getMediaByReference(client, newMedia
								.getReference());
					}
					System.out.println("	Upload Complete");
				} catch (VCloudException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		new upload(vdc, fileInputStream, fileSize);

	}

	/**
	 * Downloading the vappTemplate file
	 *
	 * @param writeToFile
	 * @param vAppTemplateId
	 */
	public void downloadvAppTemplateFile(String writeToFile,
			ReferenceType vAppTemplateRef) {

		class download extends Thread {
			String writeToFile;

			ReferenceType vAppTemplateRef;

			download(String writeToFile, ReferenceType vAppTemplateRef) {
				this.writeToFile = writeToFile;
				this.vAppTemplateRef = vAppTemplateRef;
				start();
			}

			/**
			 * From the inputstream writes to the specified file
			 *
			 * @param inputStream
			 * @param fileName
			 * @throws IOException
			 */
			void writetofile(InputStream inputStream, String fileName)
					throws IOException {
				System.out.println("	Downloading " + writeToFile);
				File f = new File(fileName);

				OutputStream out = new FileOutputStream(f);
				byte buf[] = new byte[1024];
				int len;
				while ((len = inputStream.read(buf)) > 0)
					out.write(buf, 0, len);
				out.close();
				inputStream.close();
				System.out.println("	Downloaded " + writeToFile);
			}

			/**
			 * Start running the thread
			 */
			public void run() {
				try {
					System.out.println("Download Thread " + writeToFile);
					VappTemplate vappTemplate = VappTemplate
							.getVappTemplateByReference(client, vAppTemplateRef);
					for (String fileName : vappTemplate.getDownloadFileNames()
							.keySet())
						writetofile(vappTemplate.downloadFile(fileName),
								writeToFile);

				} catch (VCloudException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		new download(writeToFile, vAppTemplateRef);
	}

	/**
	 * Get the vappTemplate
	 *
	 * @param count
	 * @param vAppTemplateId
	 * @throws VCloudException
	 */
	public void getVappTemplate(String count, ReferenceType vAppTemplateRef)
			throws VCloudException {
		class getVappTemplate extends Thread {
			String count;
			ReferenceType vAppTemplateRef;

			public getVappTemplate(String count, ReferenceType vAppTemplateRef) {
				this.vAppTemplateRef = vAppTemplateRef;
				this.count = count;
				start();
			}

			public void run() {
				System.out.println("GET vAppTemplate Thread " + count);
				VappTemplate vappTemplate = null;
				try {
					vappTemplate = VappTemplate.getVappTemplateByReference(
							client, vAppTemplateRef);
				} catch (VCloudException e) {
					e.printStackTrace();
				}
				System.out.println("	GET vAppTemplate Thread " + count);
				System.out.println("		Name: "
						+ vappTemplate.getResource().getName());
				System.out.println("		Status: "
						+ vappTemplate.getVappTemplateStatus());
			}
		}
		new getVappTemplate(count, vAppTemplateRef);
	}

	/**
	 * Finding a vdc
	 *
	 * @param vdcName
	 * @param orgName
	 * @return {@link Vdc}
	 * @throws VCloudException
	 */
	public static Vdc findVdc(String vdcName, String orgName)
			throws VCloudException {
		ReferenceType orgRef = client.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(client,
				orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		return Vdc.getVdcByReference(client, vdcRef);
	}

	/**
	 * Search the vapp template reference. Since the vapptemplate is not unique
	 * under a vdc. This method returns the first occurance of the vapptemplate
	 * in that vdc.
	 *
	 * @return
	 * @throws VCloudException
	 */
	private static ReferenceType findVappTemplateRef(String vappTemplateName,
			String vdcName, String orgName) throws VCloudException {
		ReferenceType orgRef = client.getOrgRefsByName().get(orgName);
		Organization org = Organization.getOrganizationByReference(client,
				orgRef);
		ReferenceType vdcRef = org.getVdcRefByName(vdcName);
		Vdc vdc = Vdc.getVdcByReference(client, vdcRef);
		for (ReferenceType vappTemplateRef : vdc.getVappTemplateRefs())
			if (vappTemplateRef.getName().equals(vappTemplateName))
				return vappTemplateRef;
		return null;
	}

	/**
	 * ThreadSample Program Usage
	 */
	public void getUsage() {
		System.out
				.println("java ThreadSample vCloudURL user@vcloud-organization password downloadvAppTemplateName vdcName orgName mediaFileLocation");
		System.out
				.println("java ThreadSample https://vcloud user@organization password vappTemplateName vdcName orgName media.iso");
		System.exit(0);
	}

	/**
	 * Main Method
	 *
	 * @param args
	 * @throws HttpException
	 * @throws VCloudException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	public static void main(String args[]) throws HttpException,
			VCloudException, IOException, KeyManagementException,
			NoSuchAlgorithmException, UnrecoverableKeyException,
			KeyStoreException {

		ThreadSample threadSample = new ThreadSample();
		if (args.length < 7)
			threadSample.getUsage();
		VcloudClient.setLogLevel(Level.OFF);
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
		client.setMaxConnections(10);
		client.login(args[1], args[2]);

		// Thread which uploads a media file
		File mediaFile = new File(args[6]);
		FileInputStream mediaFileInputStream = new FileInputStream(mediaFile);
		threadSample.uploadMediaFile(findVdc(args[4], args[5]),
				mediaFileInputStream, mediaFile.length());

		ReferenceType vappTemplateRef = findVappTemplateRef(args[3], args[4],
				args[5]);
		// Threads which download
		for (int i = 0; i < 5; i++)
			threadSample.downloadvAppTemplateFile("call" + i, vappTemplateRef);

		// Thread which GET's
		threadSample.getVappTemplate("call" + 1, vappTemplateRef);
	}
}
