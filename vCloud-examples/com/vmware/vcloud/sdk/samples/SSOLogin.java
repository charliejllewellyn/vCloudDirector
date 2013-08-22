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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.w3c.dom.Element;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * Login to vCD using the SAML Assertion XML from vSphere SSO/SAML IDP's
 *
 * @author Administrator
 *
 */
public class SSOLogin {

	private static VcloudClient client;

	/**
	 * Sample Usage
	 */
	public static void usage() {
		System.out.println("java SSOLogin VcloudUrl OrgName");
		System.out.println("java SSOLogin https://vcloud orgName");
		System.exit(0);
	}

	/**
	 * Main method - Login to the vCloud using the SAML Assertion XML
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
	 * @throws DatatypeConfigurationException
	 * @throws TransformerException
	 */
	public static void main(String args[]) throws HttpException,
			SecurityException, FileNotFoundException, VCloudException,
			IOException, KeyManagementException, NoSuchAlgorithmException,
			UnrecoverableKeyException, KeyStoreException, TimeoutException, TransformerException, DatatypeConfigurationException {

		if (args.length < 2)
			usage();

		VcloudClient.setLogLevel(Level.OFF);
		System.out.println("Vcloud SSO Login");
		System.out.println("----------------");
		client = new VcloudClient(args[0], Version.V5_1);
		client.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());

		Element tokenElement = null;

		// This code snippet needs the vSphere Management SDK's - SSO Client and samples jar (ssoclient.jar & samples.jar)

/*
 		Utils.trustAllHttpsCertificates();
		String[] ssoArgs = {
				"https://vSphereSSO:7444/ims/STSService",
				"user@domain",
				"vmware",
				"path to keystore file",
				"keystore password", "alias used in keystore" };
		SecurityUtil userCert = SecurityUtil.loadFromKeystore(ssoArgs[3],
				ssoArgs[4], ssoArgs[5]);
		tokenElement = AcquireHoKTokenByUserCredentialSample.getToken(ssoArgs,
					userCert.getPrivateKey(), userCert.getUserCert());
*/
		client.ssoLogin(tokenElement, args[1]);
		System.out.println("	Logged in using the SAML Assertion");

		client.getVcloudAdmin();
		System.out.println("	Get Vcloud Admin");

		client.logout();
		System.out.println("	Logout");

	}
}
