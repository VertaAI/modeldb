pipeline {
   agent any

   stages {
      stage('Build') {
         steps {
            sh """
            cd ${params.repo_root}/demos/webinar-2020-5-6/02-mdb_versioned/02-package
            VERTA_HOST=${params.verta_host} RUN_ID=${params.run_id} ./run.sh
            """
         }
      }
   }
}
