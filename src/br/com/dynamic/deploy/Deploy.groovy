package br.com.dynamic.deploy

import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret

import br.com.dynamic.deploy.CreateCredential

class Deploy{
    String credentialId = "minikube-user"
    String credentialDescription = "User for pipeline demo"

    def call (jenkins) {

        // DEFAULT POD SERVICEACCOUNT 
        jenkins.env.SERVICEACCOUNTTOKEN="/var/run/secrets/kubernetes.io/serviceaccount/token"
        // DEFAULT POD TOKEN 
        jenkins.env.TOKEN= jenkins.sh script:"cat ${jenkins.env.SERVICEACCOUNTTOKEN}", returnStdout: true
        
        createCredential(jenkins.env.TOKEN, credentialId, credentialDescription)

        jenkins.podTemplate(
            containers: [
                jenkins.containerTemplate(name: 'helm', image: 'alpine/helm:3.4.1', ttyEnabled: true, command: 'cat', alwaysPullImage: false)
            ],
            yamlMergeStrategy: jenkins.merge(),
            workspaceVolume: jenkins.persistentVolumeClaimWorkspaceVolume(
                claimName: "pvc-${jenkins.env.JENKINS_AGENT_NAME}",
                readOnly: false
            )
        )
        {
            jenkins.node(jenkins.POD_LABEL){
                jenkins.container('helm'){
                    jenkins.echo "Deploy Step"
                    jenkins.withKubeConfig([
                        credentialsId: credentialId,
                        serverUrl: 'https://kubernetes.default.svc/api',
                    ]) {
                        jenkins.sh label: 'Deploy on minikube 🚀', script:"""
                            helm package \${HELM_CHART_NAME} &&
                            helm upgrade --install --debug --namespace=\${KUBE_NAMESPACE} \${HELM_RELEASE_NAME} --set-string image.tag=\${APP_VERSION}.\${GIT_COMMIT} ./\${HELM_CHART_NAME}*.tgz
                        """
                    }

                    jenkins.echo 'Deploy successful ! 🌟'
                }
            }
        }
    }

    // CREATE A CREDENTIAL WITH GIVEN SERVICEACCOUNT TOKEN 
    @NonCPS
    def createCredential(String serviceAccountToken, String credentialId, String credentialDescription) {
        String keyfile = "/tmp/key"
        Credentials c = (Credentials) new StringCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    credentialId,
                    credentialDescription,
                    Secret.fromString(serviceAccountToken)
                )
        def ksm1 = new CertificateCredentialsImpl.UploadedKeyStoreSource(keyfile)
        Credentials ck1 = new CertificateCredentialsImpl(CredentialsScope.GLOBAL,
                    credentialId,
                    credentialDescription,
                    serviceAccountToken
                    , ksm1)
        
        SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
        SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), ck1)
    }
}
