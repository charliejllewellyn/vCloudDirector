package com.vmware.vcloud.sdk.samples;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.http.HttpException;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.constants.Version;

public class vCloudQuery {

	public static void main(String args[]) throws HttpException,
		VCloudException, IOException, KeyManagementException,
		NoSuchAlgorithmException, UnrecoverableKeyException,
		KeyStoreException {

		// Client login
		String[] LoginDetails = {"https://compute.cloud.eduserv.org.uk", "cla.admin@system", "r3m0t3!0g0n"};
		VcloudClient.setLogLevel(Level.OFF);
		VcloudClient vcloudClient = new VcloudClient(LoginDetails[0], Version.V1_5);
		vcloudClient.registerScheme("https", 443, FakeSSLSocketFactory
				.getInstance());
		vcloudClient.login(LoginDetails[1], LoginDetails[2]);
		ReferenceType orgRef = vcloudClient.getOrgRefByName("sa.eduserv.org.uk");
		Organization Org = Organization.getOrganizationByReference(vcloudClient, orgRef);
		Collection<ReferenceType> OrgNets = Org.getNetworkRefs();
		for ( ReferenceType OrgNet : OrgNets ){
			System.out.println(OrgNet.getName());
			System.out.println(OrgNet.getType());
		}
	}

}
