package br.com.dynamic.deploy

import br.com.dynamic.deploy.CreateCredential

class Deploy{
    String credentialId =  "jenkins"
    String credentialDescription = "User for pipeline demo deploy"

    def call (jenkins) {
        jenkins.env.SERVICEACCOUNT="/var/run/secrets/kubernetes.io/serviceaccount"
        jenkins.env.SERVICEACCOUNTTOKEN="${jenkins.env.SERVICEACCOUNT}/token"
        jenkins.env.CACERT= jenkins.sh script: "cat ${jenkins.env.SERVICEACCOUNT}/ca.crt", returnStdout: true, label: "Get Cluster CA certificate"
        jenkins.env.TOKEN= jenkins.sh script: "cat ${jenkins.env.SERVICEACCOUNTTOKEN}", returnStdout: true, label: "Get service account token"
        CreateCredential.createSecretText(jenkins.env.TOKEN, credentialId, credentialDescription)

        jenkins.podTemplate(
            containers: [
                jenkins.containerTemplate(name: 'helm', image: 'alpine/helm:3.5.0', ttyEnabled: true, command: 'cat', alwaysPullImage: false)
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
                        serverUrl: 'https://kubernetes.default.svc',
                        // caCertificate: jenkins.env.CACERT
                    ]) {
                        jenkins.sh label: 'Pac helm chart', script: "helm package \${HELM_CHART_NAME}"
                        jenkins.sh label: 'Deploy on minikube 🚀', script:"""
                            helm upgrade --install --kubeconfig=\${KUBECONFIG} --namespace=\${KUBE_NAMESPACE} \${HELM_RELEASE_NAME} ./\${HELM_CHART_NAME}*.tgz
                        """
                    }

                    jenkins.echo 'Deploy successful ! 🌟'
                }
            }
        }
    }

}
