package jobposter.model;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FlaviaJobPosterIntegrationTest {
        
    private static final String TEST_BUCKET_NAME = "flavia-job-poster-test-bucket";
    
    private static AmazonS3 s3;
    
    

    @BeforeClass
    public static void setUp(){
        System.out.println("==========================================");
        System.out.println("setting up integration test");
        AWSCredentials credentials = null;
        try{
            credentials = new ProfileCredentialsProvider().getCredentials();
        }
        catch (Exception e){
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }                
        s3 = new AmazonS3Client(credentials);
        Region region = Region.getRegion(Regions.EU_CENTRAL_1);
        s3.setRegion(region);
        
        try{
            createTestBucket();
        
        try{
            uploadInitialJobHtmlFilesToBucket();
        }
        catch (IOException ex){
            throw new RuntimeException("Unable to create file temporary file to push to S3 bucket");
        }
        
        try {
            uploadInitialJobJsonFileToBucket();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create file temporary file to push to S3 bucket");
        }
        }
        catch (AmazonServiceException ase) {
            printAmazonServiceException(ase);
        }
        catch (AmazonClientException ace) {
            printAmazonClientException(ace);
        }
        System.out.println("==========================================");
    }
    
    @AfterClass
    public static void tearDown(){
        System.out.println("==========================================");
        System.out.println("tearing down integration test");
        try{
            
            deleteAllItemsInTestBucket();
            deleteTestBucket();
            
        }
        catch (AmazonServiceException ase) {
            printAmazonServiceException(ase);
        }
        catch (AmazonClientException ace) {
            printAmazonClientException(ace);
        }
        System.out.println("integration test done");
        System.out.println("==========================================");
    }
    
    /*
    ============================================================================
                               TESTS
    ============================================================================
    */        
    
    
    @Test
    public void testA_ReadJobsFromBucket() throws IOException{
        WrapperAmazonS3 wrapperAmazonS3 = new WrapperAmazonS3();
        wrapperAmazonS3.setBucketName(TEST_BUCKET_NAME);
        wrapperAmazonS3.setJsonFileKey("jobs.json");
        wrapperAmazonS3.setAmazonS3(s3);
        
        List<String> htmlFileNames = wrapperAmazonS3.getHtmlFileNames();
        Map<String, Job> allJobs = new HashMap<>();
        for (String htmlFileName: htmlFileNames) {
            allJobs.put(htmlFileName, wrapperAmazonS3.getJob(htmlFileName));
        }
        
        assertTrue(allJobs.size() == 2);
        assertTrue(allJobs.containsKey("test-job-1.html"));
        assertTrue(allJobs.containsKey("test-job-2.html"));
        Job job1 = allJobs.get("test-job-1.html");
        Job job2 = allJobs.get("test-job-2.html");
        assertTrue(job1.getHtmlFileKey().equals("test-job-1.html"));
        assertTrue(job2.getHtmlFileKey().equals("test-job-2.html"));
    }
        
    @Test
    public void testB_ReadVisibilityFromBucket() throws IOException{
        WrapperAmazonS3 wrapperAmazonS3 = new WrapperAmazonS3();
        wrapperAmazonS3.setBucketName(TEST_BUCKET_NAME);
        wrapperAmazonS3.setJsonFileKey("jobs.json");
        wrapperAmazonS3.setAmazonS3(s3);
        
        List<String> visibleJobs = wrapperAmazonS3.getVisibleJobs();
        assertTrue(visibleJobs.size() == 1);
        assertTrue(visibleJobs.contains("test-job-2.html"));
    }        
    
    @Test
    public void testC_updateJobByHtmlContent() throws IOException {
        WrapperAmazonS3 wrapperAmazonS3 = new WrapperAmazonS3();
        wrapperAmazonS3.setBucketName(TEST_BUCKET_NAME);
        wrapperAmazonS3.setJsonFileKey("jobs.json");
        wrapperAmazonS3.setAmazonS3(s3);
        
        List<String> htmlFileNames = wrapperAmazonS3.getHtmlFileNames();
        Map<String, Job> allJobs = new HashMap<>();
        for (String htmlFileName: htmlFileNames) {
            allJobs.put(htmlFileName, wrapperAmazonS3.getJob(htmlFileName));
        }               
        
        Job testJob2 = allJobs.get("test-job-2.html");                       
        assertTrue(testJob2.getJobType() == JobType.OFFICE);
        assertTrue("Pizzabäcker*in (m/w)".equals(testJob2.getTitle()));
        
        Job testJob1 = allJobs.get("test-job-1.html");                
        assertTrue(testJob1.getJobType() == JobType.IT);
        assertTrue("Spaßbremse (m/w)".equals(testJob1.getTitle()));
        
    }
    
    @Test 
    public void testZ_editAndCreateFilesAndUpdateFilesOnS3Accordingly() throws IOException {
        WrapperAmazonS3 wrapperAmazonS3 = new WrapperAmazonS3();
        wrapperAmazonS3.setBucketName(TEST_BUCKET_NAME);
        wrapperAmazonS3.setJsonFileKey("jobs.json");
        wrapperAmazonS3.setAmazonS3(s3);
        
        List<String> htmlFileNames = wrapperAmazonS3.getHtmlFileNames();
        Map<String, Job> allJobs = new HashMap<>();
        for (String htmlFileName: htmlFileNames) {
            allJobs.put(htmlFileName, wrapperAmazonS3.getJob(htmlFileName));
        }
        
        // edit the existing allJobs
        Job job1 = allJobs.get("test-job-1.html");
        job1.setVisible(true);
                
        job1.setHtmlContent(job1.getHtmlContent() + "append!");
        
        Job job2 = allJobs.get("test-job-2.html");
        job2.setVisible(false);
        
        // create new job
        Job newJob = new Job();  
        newJob.setHtmlFileKey("test-job-3.html");
        newJob.setVisible(true);        

        String htmlStringOfNewJob = createHtmlStringOfNewJob();
        Mapper.updateJobByHtmlContent(newJob, htmlStringOfNewJob);
        
        allJobs.put("test-job-3.html", newJob);                
        
        // write
        List<String> visibleJobs = new ArrayList<>();
        for (Job job: allJobs.values()){
            wrapperAmazonS3.writeJobToAmazonS3(job);
            if (job.isVisible()){
                visibleJobs.add(job.getHtmlFileKey());
            }
        }
        
        ArrayNode jobsJson = Mapper.buildJsonNodeOfVisibleJobs(visibleJobs, allJobs);
        wrapperAmazonS3.writeJsonStringToAmazonS3(jobsJson.toString());
        
        
        // check whether objects were written correctly                
        htmlFileNames = wrapperAmazonS3.getHtmlFileNames();
        allJobs = new HashMap<>();
        for (String htmlFileName: htmlFileNames) {
            allJobs.put(htmlFileName, wrapperAmazonS3.getJob(htmlFileName));
        }
               
        assertTrue(3 == allJobs.size());
        assertTrue(allJobs.containsKey("test-job-3.html"));
        
        visibleJobs = wrapperAmazonS3.getVisibleJobs();
        assertTrue(2 == visibleJobs.size());
        assertTrue(visibleJobs.contains("test-job-1.html"));
        assertTrue(visibleJobs.contains("test-job-3.html"));

        job1 = allJobs.get("test-job-1.html");        
        assertTrue(job1.getHtmlContent().contains("append!"));
        
    }
    
    /*
    ============================================================================
                          PRIVATE FUNCTIONS FOR TESTS
    ============================================================================
    */
    
    private String createHtmlStringOfNewJob() {
        String htmlContent = "<span>\n"
                + "<h2 data-job-type=\"it\">Go-getter (m/w)</h2>"
                + "<p>Du wirst ordentlich was bewegen, auch nach deinem Tod? Du machst Wasser zu Wein?\n"
                + "Du unterstützt die Volksfront von Judäa?\n"
                + "We want you!</p>\n"
                + "</span>";
        return htmlContent;
    }
    
    /*
    ============================================================================
                    PRIVATE FUNCTIONS FOR SET UP AND TEAR DOWN
    ============================================================================
    */
    
    private static void createTestBucket(){
        System.out.println("creating test bucket");
        s3.createBucket(TEST_BUCKET_NAME);
    }
    
    private static void uploadInitialJobHtmlFilesToBucket() throws IOException{
        s3.putObject(new PutObjectRequest(TEST_BUCKET_NAME, "test-job-1.html", createFirstInitialJobFile()));
        s3.putObject(new PutObjectRequest(TEST_BUCKET_NAME, "test-job-2.html", createSecondInitialJobFile()));
    }

    private static void uploadInitialJobJsonFileToBucket() throws IOException {
        s3.putObject(TEST_BUCKET_NAME, "jobs.json", createInitialJobsJsonFile());
    }
    
    private static File createFirstInitialJobFile() throws IOException {
        File file = File.createTempFile("test-job-1", ".html");
        file.deleteOnExit();
        
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("<span>\n");
        writer.write("<h2 data-job-type=\"it\">Spaßbremse (m/w)</h2>");
        writer.write("<p>Wir lachen hier einfach zu viel und wir suchen jemanden,\n");
        writer.write("der da mal auf die Bremse tritt. Es wäre außerdem wünschenswert,\n");
        writer.write("wenn du einen großen Kuchenappetit mitbrächtest.</p>");
        writer.write("</span>\n");
        writer.close();
        
        return file;
    }
    
    private static File createSecondInitialJobFile() throws IOException {
        File file = File.createTempFile("test-job-2", ".html");
        file.deleteOnExit();
        
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("<span>\n");
        writer.write("<h2 data-job-type=\"office\">Pizzabäcker*in (m/w)</h2>");
        writer.write("<p>Du suchst einen kreativen Job? Du willst handwerklich arbeiten und\n");
        writer.write("gleichzeitig mit neuester Technologie. Dann komm zu uns!\n");
        writer.write("Wir suchen einen aufgeschlossenen Senior Pizzaentwickler.\n");
        writer.write("Als erstes müsstest du allerdings dein eigenes hausinternes\n");
        writer.write(" Pizzabestellsystem entwickeln.</p>\n");
        writer.write("</span>");
        writer.close();
        
        return file;
    }

    private static File createInitialJobsJsonFile() throws IOException {
        File file = File.createTempFile("jobs", ".json");
        file.deleteOnExit();
        
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("[\n");
        writer.write("  {\n");
        writer.write("    \"title\": \"Pizzabäcker*in (m/w)\",\n");
        writer.write("    \"path\": \"test-job-2.html\",\n");
        writer.write("    \"vacancyType\": \"office\"\n");
        writer.write("  }\n");
        writer.write("]\n");

        writer.close();
        
        return file;
    }

    private static void deleteAllItemsInTestBucket() {
        System.out.println("deleting objects from bucket " + TEST_BUCKET_NAME);
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName(TEST_BUCKET_NAME));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println("deleting object with key " + objectSummary.getKey());
            s3.deleteObject(TEST_BUCKET_NAME, objectSummary.getKey());
        }
    }
    
    private static void deleteTestBucket() {
        System.out.println("deleting bucket " + TEST_BUCKET_NAME);
        s3.deleteBucket(TEST_BUCKET_NAME);
    }
    
    private static void printAmazonServiceException(AmazonServiceException ase){
        System.out.println("Caught an AmazonServiceException, which means your request made it "
                + "to Amazon S3, but was rejected with an error response for some reason.");
        System.out.println("Error Message:    " + ase.getMessage());
        System.out.println("HTTP Status Code: " + ase.getStatusCode());
        System.out.println("AWS Error Code:   " + ase.getErrorCode());
        System.out.println("Error Type:       " + ase.getErrorType());
        System.out.println("Request ID:       " + ase.getRequestId());
    }
    
    private static void printAmazonClientException(AmazonClientException ace){
        System.out.println("Caught an AmazonClientException, which means the client encountered "
                + "a serious internal problem while trying to communicate with S3, "
                + "such as not being able to access the network.");
        System.out.println("Error Message: " + ace.getMessage());        
    }




    
    
    
}
