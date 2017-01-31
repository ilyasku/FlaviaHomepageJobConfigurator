package jobpostergui;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jobposter.model.Job;
import jobposter.model.Mapper;
import jobposter.model.WrapperAmazonS3;
import jobpostergui.controller.JobEditorController;
import jobpostergui.controller.NewJobController;
import jobpostergui.model.JobForTableView;


public class MainApp extends Application {

    private static final String BUCKET_NAME = "www-jobs.flavia-it.de";
    //private static final String BUCKET_NAME = "job-poster-gui-test-bucket";
    private static final String JOB_JSON_FILE_KEY = "jobs.json";
        
    private Stage primaryStage;
    private BorderPane rootLayout;
    
    private ObservableList<JobForTableView> jobsForTableView;
    
    private Map<String, Job> jobs;    
    private Boolean jobsWereLoaded = false;        
    
    private WrapperAmazonS3 wrapperAmazonS3;   
    
    private List<String> htmlFileNames = new ArrayList<>();
    private List<String> jobsToBeDeleted = new ArrayList<>();
    private List<String> htmlFilesThatNeedToBeSaved = new ArrayList<>();        

    private Label statusLabel;
    
    public boolean skipAddingToSaveListSinceDeletionIsHandled = false;
    
    public MainApp(){        
        wrapperAmazonS3 = new WrapperAmazonS3();
        wrapperAmazonS3.setBucketName(BUCKET_NAME);
        wrapperAmazonS3.setJsonFileKey(JOB_JSON_FILE_KEY);
        wrapperAmazonS3.setAmazonS3(createAmazonS3Object());        
    }
    
    @Override
    public void start(Stage primaryStage){
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Flavia Job Poster");

        initRootLayout();

        showJobEditor();   
        
        //Platform.setImplicitExit(false);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (jobsToBeDeleted.isEmpty() && htmlFilesThatNeedToBeSaved.isEmpty()) return;
                
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Confirm exit");
                alert.setHeaderText("Looks like there are unsaved changes.");
                alert.setContentText("Do you really want to quit?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() != ButtonType.OK){                    
                    event.consume();
                }
            }
        });
    }
    
    public void initRootLayout() {
        try {
            
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            
            Scene scene = new Scene(rootLayout);
            //scene.getStylesheets().add("/styles/styles.css");
            primaryStage.setScene(scene);
            primaryStage.show();  
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shows the job editor inside the root layout.
     */
    public void showJobEditor() {
        try {
            
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/JobEditor.fxml"));
            AnchorPane jobEditor = (AnchorPane) loader.load();

            
            rootLayout.setCenter(jobEditor);
            
            JobEditorController controller = loader.getController();
            controller.setMainApp(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadJobsFromAmazonS3() throws IOException {
        htmlFileNames = wrapperAmazonS3.getHtmlFileNames();
        jobsToBeDeleted = new ArrayList<>();
        htmlFilesThatNeedToBeSaved = new ArrayList<>();
        jobs = new HashMap<>();
        for (String htmlFileName: htmlFileNames) {
            Job job = wrapperAmazonS3.getJob(htmlFileName);
            job.setVisible(false);
            jobs.put(htmlFileName, job);
        }        
                
        List<String> visibleJobs = wrapperAmazonS3.getVisibleJobs();
        for (String fileNameOfVisibleJob: visibleJobs) {
            Job jobObject = jobs.get(fileNameOfVisibleJob);
            jobObject.setVisible(true);
        }
        
        createJobsForTableView(jobs.values());
        jobsWereLoaded = true;
    }
    
    public void propagateRenamingOfFileToAllLists(String oldFileName, String newFileName) {
        Job jobObject = jobs.get(oldFileName);
        jobs.remove(oldFileName);
        jobs.put(newFileName, jobObject);
        if (htmlFilesThatNeedToBeSaved.contains(oldFileName)) {
            htmlFilesThatNeedToBeSaved.remove(oldFileName);            
        }
        addJobToDeleteList(oldFileName);
        addToFilesThatNeedToBeSaved(newFileName);
    }
    
    public void addToFilesThatNeedToBeSaved(String htmlFileName) {
        if (skipAddingToSaveListSinceDeletionIsHandled) return;
        if (!htmlFilesThatNeedToBeSaved.contains(htmlFileName)) {
            htmlFilesThatNeedToBeSaved.add(htmlFileName);
        }
    }
    
    public void removeFromFilesThatNeedToBeSaved(String htmlFileName){
        if (htmlFilesThatNeedToBeSaved.contains(htmlFileName)) {
            htmlFilesThatNeedToBeSaved.remove(htmlFileName);
        }
    }
    
    public void writeJobsToAmazonS3(){
        if (jobsWereLoaded) {
            List<String> visibleJobs = new ArrayList<>();
            for (Entry<String, Job> entry: jobs.entrySet()){
                String fileName = entry.getKey();
                Job job  = entry.getValue();
                if (fileName != job.getHtmlFileKey()) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.initOwner(primaryStage);
                    alert.setTitle("Something went awfully wrong!");
                    alert.setHeaderText("File names not matching!");
                    alert.setContentText("In the map of jobs, the file name referencing\n"
                            + "to the job object does not match the filename\n"
                            + "stored in the job object!\n"
                            + "Name in map: "+fileName + "\n"
                            + "Name in job object: " + job.getHtmlFileKey());
                    alert.showAndWait();
                    primaryStage.close();
                    System.exit(1);
                }
                if (htmlFilesThatNeedToBeSaved.contains(fileName)) {
                    try {
                        System.out.println("saving file " + fileName);
                        wrapperAmazonS3.writeJobToAmazonS3(job);
                        if (!htmlFileNames.contains(fileName)){
                            htmlFileNames.add(fileName);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (job.isVisible()){
                    visibleJobs.add(fileName);
                }
            }
            ArrayNode jobsJson = Mapper.buildJsonNodeOfVisibleJobs(visibleJobs, jobs);
            try {
                wrapperAmazonS3.writeJsonStringToAmazonS3(jobsJson.toString());
            } catch (IOException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            deleteJobsFromAmazonS3();
            htmlFilesThatNeedToBeSaved = new ArrayList<>();
        }
        
    }
    
    public void addJobToDeleteList(String fileName) {
        if (htmlFileNames.contains(fileName)) {
            jobsToBeDeleted.add(fileName);
        }
    }
    
    
    public void addJobForTableView(Job job) {
        JobForTableView jobForTableView = new JobForTableView(job);
        jobsForTableView.add(jobForTableView);
    }
    
    private void deleteJobsFromAmazonS3() {
        for (String fileName: jobsToBeDeleted) {
            System.out.println("deleting " + fileName);
            try {
                wrapperAmazonS3.deleteFile(fileName);
            } catch (Exception ex){
                System.out.println("Exception while trying to delete file " + fileName + ":");                
                System.out.println(ex.toString());
            }            
        }
        jobsToBeDeleted = new ArrayList<>();
    }
    
    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public Set<String> getHtmlFileNames() {
        return jobs.keySet();
    }
    
    public static void main(String[] args) throws Exception {
        launch(args);
    }
    
    public ObservableList<JobForTableView> getJobsForTableView(){
        return jobsForTableView;
    }
    
    public boolean showNewJobDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/NewJob.fxml"));
            AnchorPane page = (AnchorPane) loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add new job");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            
            NewJobController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setMainApp(this);
            
            dialogStage.showAndWait();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        
    }
    
    public void addJob(Job job){
        jobs.put(job.getHtmlFileKey(), job);
    }

    private void createJobsForTableView(Collection<Job> listOfJobs) {
        jobsForTableView = FXCollections.observableArrayList();
        for (Job job: listOfJobs){            
            addJobForTableView(job);
        }        
    }        

    private AmazonS3 createAmazonS3Object() {
        AWSCredentials credentials = null;
        try{
            credentials = new ProfileCredentialsProvider().getCredentials();
        }
        catch (Exception e){
            System.err.println("ERROR: Cannot load the credentials from the credential profiles file. \n"
                    + "Please make sure that your credentials file is at the correct location \n"
                    + "(Linux: ~/.aws/credentials ; Windows: C:\\User\\<user-name>\\.aws\\credentials),\n"
                    + "and is in valid format.");
            System.exit(0);
        }                
        AmazonS3 s3 = new AmazonS3Client(credentials);
        Region region = Region.getRegion(Regions.EU_CENTRAL_1);
        s3.setRegion(region);
        return s3;
    }

    public void deleteFromJobsForTableView(JobForTableView currentlySelectedJob) {
        addJobToDeleteList(currentlySelectedJob.getHtmlFileKey());
        jobsForTableView.remove(currentlySelectedJob);
    }
    
    public Boolean hasChangesToSave() {
        if (jobsToBeDeleted.isEmpty() && htmlFilesThatNeedToBeSaved.isEmpty()) {
            return false;
        }
        return true;
    }

}
