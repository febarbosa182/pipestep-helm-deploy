import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret

class CreateCredential{
  // CREATE A CREDENTIAL WITH GIVEN SERVICEACCOUNT TOKEN 
  @NonCPS
  def createCredential(String serviceAccountToken) {
      String keyfile = "/tmp/key"
      Credentials c = (Credentials) new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                "minikube-user",
                "User for pipeline demo",
                Secret.fromString(serviceAccountToken)
              )
      def ksm1 = new CertificateCredentialsImpl.UploadedKeyStoreSource(keyfile)
      Credentials ck1 = new CertificateCredentialsImpl(CredentialsScope.GLOBAL,
                "minikube-user",
                "User for pipeline demo",
                serviceAccountToken
                , ksm1)
      
      SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
      SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), ck1)
  }
}