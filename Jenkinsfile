BUILD_STATUS = 'success';
projectSiteUri = 'https://project-site.service.lixtec.fr/lixbox-registry';
gitUri = 'https://github.com/lixbox-team/lixbox-registry.git';
mattermostUri = 'https://team.service.lixtec.fr/hooks/xwqzwmg7zpf18kkdxm3tqw1kqh';
channel = 'lixbox';
branchName = 'jdk-8'

@NonCPS
def onFailed(e) {
    currentBuild.result = 'FAILED'
    BUILD_STATUS = 'FAILED'
    def title = JOB_NAME+' - Build # '+BUILD_NUMBER+' - '+BUILD_STATUS+'!';
    def msg = 'The '+JOB_NAME+' - Build # '+BUILD_NUMBER+' is '+BUILD_STATUS+'. \n Check console output at '+BUILD_URL+' to view the results or check the project site at '+projectSiteUri;   
    mattermostSend channel: channel, color: '#dd4040', endpoint: mattermostUri, message: msg, text: title
}
    
node('slave-gradle-graalvm') {
    stage('Init'){
        echo 'Initialisation started'
        try{
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew --stop'
        }
        catch (e){
        }
        try
        {
            deleteDir()
            git credentialsId: '2a48fbab-f642-4bc5-98f1-0c7166aacd1b', url: gitUri, branch: branchName
            sh 'chmod -R 755 ${WORKSPACE}'
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew clean --stacktrace'            
        }
        catch (e){
//            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
            onFailed(e);
            error e
        }
        echo 'Initialisation finished'
    }
    
    stage('Check'){
        echo 'Check started'
        try{
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -x buildNative check --stacktrace'            
        }
        catch (e){
//            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
            onFailed(e);
            error e
        }
        echo 'Check finished'
    }
    
    stage('Distribution for test'){
        echo 'Distribution for test started'
        retry(2){ 
            try{
                withCredentials([usernamePassword(credentialsId: '2a48fbab-f642-4bc5-98f1-0c7166aacd1b', usernameVariable: 'JENKINS_USR', passwordVariable: 'JENKINS_PWD')]) {
                    sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -Dorg.ajoberstar.grgit.auth.username=${JENKINS_USR} -Dorg.ajoberstar.grgit.auth.password=${JENKINS_PWD} --stacktrace publish'
                }            
            }
            catch (e){
//                sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
                onFailed(e);
                error e
            }
        }
        echo 'Distribution for test finished'
    }
       
    stage('Code review & report'){
        echo 'Code review & report started'
        try{
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -x test --stacktrace sonarqube checkSonarQualityGate'
        }
        catch (e){
//            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
            onFailed(e);
            error e
        }
        echo 'Code review & report finished'
    }
    
    stage('Project site'){
        echo 'Project site started'
        try{
//            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew --stacktrace site uploadSite'
        }
        catch (e){
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
            onFailed(e);
            error e
        }
        echo 'Project site finished'
    }
    
    stage('Build Native App'){
        echo 'Build Native App started'
        try{
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew buildNative --stacktrace'            
        }
        catch (e){
            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
            onFailed(e);
            error e
        }
        echo 'Build Native App finished'
    }
    
    stage('Distribution for production'){
        echo 'Distribution for production started'
        retry(2){ 
            try{
                withCredentials([usernamePassword(credentialsId: '2a48fbab-f642-4bc5-98f1-0c7166aacd1b', usernameVariable: 'JENKINS_USR', passwordVariable: 'JENKINS_PWD')]) {
                    sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -Dorg.ajoberstar.grgit.auth.username=${JENKINS_USR} -Dorg.ajoberstar.grgit.auth.password=${JENKINS_PWD} -Penv=prod --stacktrace publish'
                }
            }
            catch (e){
//                sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
                onFailed(e);
                error e
            }
        }
        echo 'Distribution for production finished'
    }
    
    stage('Expose docker solution'){
        echo 'Expose docker solution started'
        retry(2){ 
            try{
                withCredentials([usernamePassword(credentialsId: 'DOCKERHUB', usernameVariable: 'JENKINS_USR', passwordVariable: 'JENKINS_PWD')]) {
                    sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -Pdockerhub_username=${JENKINS_USR} -Pdockerhub_password=${JENKINS_PWD} --stacktrace buildAndPushDockerImages'
                }
                if(!currentBuild.result)
                {
                   currentBuild.result = 'SUCCESS'
                   BUILD_STATUS = 'SUCCESS'
                }
                def title = JOB_NAME+' - Build # '+BUILD_NUMBER+' - '+BUILD_STATUS+'!';
                def msg = 'The '+JOB_NAME+' - Build # '+BUILD_NUMBER+' is '+BUILD_STATUS+'. \n Check console output at '+BUILD_URL+' to view the results.';
                mattermostSend channel: channel, color: 'rgb(184, 255, 184)', endpoint: mattermostUri, message: msg, text: title
            }
            catch (e){
                sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew site uploadSite --stacktrace'
                onFailed(e);
                error e
            }
        }
        echo 'Expose docker solution finished'
    }
}