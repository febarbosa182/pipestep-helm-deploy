package br.com.dynamic.deploy

import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret

class CreateCredential{
  static def createSecretText(String credentialToken, String credentialId, String credentialDescription){
    String keyfile = "/tmp/key"
    Credentials c = (Credentials) new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId, 
        credentialDescription, 
        Secret.fromString(credentialToken))
    def ksm1 = new CertificateCredentialsImpl.UploadedKeyStoreSource(keyfile)
    Credentials ck1 = new CertificateCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId, 
        credentialDescription, 
        credentialToken
        , ksm1)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), ck1)
  }
}