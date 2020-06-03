pipeline {
   agent any

   stages {
      stage('Build') {
         steps {
            sh """
            cd ${params.repo_root}/demos/webinar-2020-5-6/01-ad_hoc/02-package
            BUCKET=${params.bucket} MODEL_PATH=${params.model_path} METADATA_PATH=${params.metadata_path} ./run.sh
            """
         }
      }
   }
}
