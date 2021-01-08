package br.com.dynamic.deploy

import br.com.dynamic.deploy.CreateCredential

class Deploy{
    String credentialId =  "minikube-user"
    String credentialDescription = "User for pipeline demo deploy"

    def call (jenkins) {

        jenkins.env.SERVICEACCOUNTTOKEN="/var/run/secrets/kubernetes.io/serviceaccount/token"
        jenkins.env.TOKEN= jenkins.sh script: "cat ${jenkins.env.SERVICEACCOUNTTOKEN}", returnStdout: true
        CreateCredential.createSecretText(jenkins.env.TOKEN, credentialId, credentialDescription)

        jenkins.podTemplate(
            containers: [
                jenkins.containerTemplate(name: 'helm', image: 'alpine/helm:3.4.2', ttyEnabled: true, command: 'cat', alwaysPullImage: false)
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
                            kubectl config view --raw >~/.kube/config &&
                            helm package \${HELM_CHART_NAME} &&
                            helm upgrade --install --debug --namespace=\${KUBE_NAMESPACE} \${HELM_RELEASE_NAME} --set-string image.tag=\${APP_VERSION}.\${GIT_COMMIT} ./\${HELM_CHART_NAME}*.tgz
                        """
                    }

                    jenkins.echo 'Deploy successful ! 🌟'
                }
            }
        }
    }

}
