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

import com.vmware.vcloud.api.rest.schema.AdminVdcStorageProfileType;
import com.vmware.vcloud.api.rest.schema.IdentifiableResourceType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.OrgType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VdcStorageProfile;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.admin.AdminVdcStorageProfile;
import com.vmware.vcloud.sdk.constants.Version;

public class ReplicateFirewall {
	
	private static void GetFirewallRules(VcloudClient vcloudClient) throws VCloudException {
		Collection<ReferenceType> orgRefs = vcloudClient.getOrgRefs();
/*		for ( ReferenceType orgRef : orgRefs ){
			System.out.println(orgRef.getName());
			Organization org = Organization.getOrganizationByReference(vcloudClient, orgRef);
			Collection<ReferenceType> vdcRefs = org.getVdcRefs();
			for ( ReferenceType vdcRef : vdcRefs ){
				Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);
				Collection<ReferenceType> storRefs = vdc.getVdcStorageProfileRefs();
				for (ReferenceType storeRef : storRefs){
					AdminVdcStorageProfile stor =  AdminVdcStorageProfile.getAdminVdcStorageProfileByReference(vcloudClient, storeRef);
					
				}
				 
				
				
			}	
		}*/
		ReferenceType orgRef = vcloudClient.getOrgRefByName("sa.eduserv.org.uk");
		Organization org = Organization.getOrganizationByReference(vcloudClient, orgRef);
		ReferenceType vdcRef = org.getVdcRefByName("SDC1Z01-ALLOC-01");
		Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);
		ReferenceType vappRef = vdc.getVappRefByName("Zerto-test-SDC");
		Vapp vapp = Vapp.getVappByReference(vcloudClient, vappRef);
		//NetworkConfigurationType vAppNetConf = vapp.getVappNetworkConfigurationByName("Zerto-SDC-network").getConfiguration();
		//vapp.getVappNetworkConfigurationByName("test").setConfiguration(vAppNetConf);
		System.out.print(vapp.getVappNetworkConfigurationByName("Zerto-SDC-network"));
	}

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

		GetFirewallRules(vcloudClient);

		
	}

}

