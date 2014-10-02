#!/bin/bash
#!/bin/sh

for i in {1..5}
do
   java -Xms1024m -Xmx2548m -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.DocumentSemanticParser    Production root salle20mimis job_post_backup1 "id,title,recordText,detail_page_text" "detail_page_main_content,language" " detail_page_main_content is null "  30000
 
done
exit 0




#!/bin/bash
#!/bin/sh


#0. ASSUMPTION: we already create two dictionaries for all the records with language 'en' and 'nl'; The select option for that is " language = 'X' "  !!!!!!!!!!!!!!!!

 


#1. 			##########################  DICIONARY  ##########################  
#   UPDATE DICTIONARY FOR ENGLISH AND DUTCH  - we assume here there is already created the dictionary for all the records in our corpus thats why we don't delete it initially
#	Select condition:  "document_vector is NULL AND language = 'X' AND valid = 1"    //we need to update the dictionary for the records that we are processing  - Smoothing is not applied in the next tasks   	  
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDictionary dictionary_jobs_en feature en 1 3 3 25 true true true Production root salle20mimis job_post "id,navigation_id,title,recordText,detail_page_main_content,detail_page_url" "title,recordText,detail_page_main_content,detail_page_url" " document_vector is  NULL AND language = 'en'  AND valid = 1" 400000 1000000 100 false true
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDictionary dictionary_jobs_nl feature nl 1 3 3 25 true true true Production root salle20mimis job_post "id,navigation_id,title,recordText,detail_page_main_content,detail_page_url" "title,recordText,detail_page_main_content,detail_page_url" " document_vector is  NULL AND language = 'nl'  AND valid = 1" 400000 1000000 100 false true 








#2. 			##########################  DOCUMENT VECTORS  ##########################
#   CREATE DOCUMENT VECTORS FOR ENGLISH AND DUTCH - we assume here that the records are already parsed from the dictionary..since there is no smoothing
#	Select condition:  "document_vector is not NULL AND language = 'X' AND valid = 1"   
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDocumentVectors Production root salle20mimis job_post "id,title,recordText,detail_page_main_content,detail_page_url" " document_vector is  NULL AND valid = 1 and  language = 'en' " 40000 "title:0.7,recordText:0.1,detail_page_main_content:0.1,detail_page_url:0.1" dictionary_jobs_en feature en false 1 2 3 25 10 1 true ""  10 false true true true false document_vector
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDocumentVectors Production root salle20mimis job_post "id,title,recordText,detail_page_main_content,detail_page_url" " document_vector is  NULL AND valid = 1 and  language = 'nl' " 40000 "title:0.7,recordText:0.1,detail_page_main_content:0.1,detail_page_url:0.1" dictionary_jobs_en feature nl false 1 2 3 25 10 1 true ""  10 false true true true false document_vector








#3. 			##########################  CLUSTERING  ##########################
# Cluster similar document vectors together FOR ENGLISH AND DUTCH, assign a label to each group and save it to database(todo the last two parts - @see corresponding issues in the machine learning project)
#	Select condition:  " language = 'X' AND valid = 1"   
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooClustering Production root salle20mimis job_post "id,title,document_vector" "  valid = 1 and LANGUAGE = 'en' " 4000 en 0.5 0.7 true 20 9 20 0.2 0.05
java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooClustering Production root salle20mimis job_post "id,title,document_vector" "  valid = 1 and LANGUAGE = 'nl' " 4000 nl 0.5 0.7 true 20 9 20 0.2 0.05



exit 0

