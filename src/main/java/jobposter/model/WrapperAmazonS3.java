package jobposter.model;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class WrapperAmazonS3 {
    
    private AmazonS3 amazonS3;
    private String bucketName;
    private String jsonFileKey;        
    
    public List<String> getHtmlFileNames() {
        List<String> htmlFileNames = new ArrayList<>();
        ObjectListing objectListing = amazonS3.listObjects(bucketName);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()){
            String fileName = objectSummary.getKey();
            if (isHtml(fileName)) {
                htmlFileNames.add(fileName);
            }
        }
        return htmlFileNames;
    }
    
    public void deleteFile(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
    }
    
    public Job getJob(String htmlFileName) throws IOException {
        Job job = new Job();
        job.setHtmlFileKey(htmlFileName);
        String htmlContent = getContentOfHtmlFile(htmlFileName);        
        Mapper.updateJobByHtmlContent(job, htmlContent);
        return job;
    }
    
    public List<String> getVisibleJobs() throws IOException{
        String visibilityJsonString = getJobStatusJsonFile();
        return Mapper.extractVisibleJobsFromJson(visibilityJsonString);
    }    
    
    public void writeJobToAmazonS3(Job job) throws IOException {
        File temporaryHtmlFile = createTemporaryFile(job.getHtmlContent(), job.getHtmlFileKey());
        writeFileToAmazonS3(job.getHtmlFileKey(), temporaryHtmlFile);
    }
    
    public void writeJsonStringToAmazonS3(String jsonString) throws IOException {
        File temporaryJsonFile = createTemporaryFile(jsonString, jsonFileKey);
        writeFileToAmazonS3(jsonFileKey, temporaryJsonFile);
    }      

    private File createTemporaryFile(String fileContent, String fileName) throws IOException {
        String[] fileNameAndExtension = fileName.split("\\.");
        File file = File.createTempFile(fileNameAndExtension[0], fileNameAndExtension[1]);
        file.deleteOnExit();
        
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write(fileContent);
        writer.close();
        
        return file;
    }

    private void writeFileToAmazonS3(String fileKey, File file) {
        amazonS3.putObject(bucketName, fileKey, file);
    }

    private String getJobStatusJsonFile() throws IOException{
        S3Object jsonFileAsS3Object = amazonS3.getObject(bucketName, jsonFileKey);
        return s3ObjectContentToString(jsonFileAsS3Object.getObjectContent());
    }



    private String s3ObjectContentToString(S3ObjectInputStream objectContent) throws IOException {                
        return ReaderS3ObjectContent.InputStreamToString(objectContent);
    }  

    private boolean isHtml(String key) {
        if (key.contains(".html")){
            return true;
        }
        return false;
    }

    private String getContentOfHtmlFile(String htmlFileKey) throws IOException {
        S3Object htmlFile = amazonS3.getObject(bucketName, htmlFileKey);
        return s3ObjectContentToString(htmlFile.getObjectContent());
    }
    
    public void setAmazonS3(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }
    
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getJsonFileKey() {
        return jsonFileKey;
    }

    public void setJsonFileKey(String jsonFileKey) {
        this.jsonFileKey = jsonFileKey;
    }
    
}
