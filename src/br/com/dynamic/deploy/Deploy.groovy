package br.com.dynamic.deploy

import br.com.dynamic.deploy.CreateCredential

class Deploy{
    def call (jenkins) {

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
                    // INTERNAL API SERVER 
                    env.APISERVER="https://kubernetes.default.svc"
                    // DEFAULT POD SERVICEACCOUNT 
                    env.SERVICEACCOUNT="/var/run/secrets/kubernetes.io/serviceaccount"
                    // DEFAULT POD TOKEN 
                    env.TOKEN=sh script:"cat ${env.SERVICEACCOUNT}/token", returnStdout: true
                    
                    createCredential(env.TOKEN)

                    jenkins.withKubeConfig([
                        credentialsId: 'minikube-user',
                        serverUrl: 'https://host.minikube.internal:32776',
                    ]) {
                        jenkins.sh label: 'Deploy on minikube 🚀', script:"""
                            helm package \${HELM_CHART_NAME} &&
                            helm upgrade --install --atomic --debug --namespace=\${KUBE_NAMESPACE} \${HELM_RELEASE_NAME} --set-string image.tag=\${APP_VERSION}.\${GIT_COMMIT} ./\${HELM_CHART_NAME}*.tgz
                        """
                    }

                    jenkins.echo 'Deploy successful ! 🌟'
                }
            }
        }
    }
}
