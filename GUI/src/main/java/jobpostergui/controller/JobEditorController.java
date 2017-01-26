package jobpostergui.controller;

import java.io.IOException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import jobposter.model.Job;
import jobposter.model.JobType;
import jobposter.model.Mapper;
import jobpostergui.MainApp;
import jobpostergui.model.JobForTableView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class JobEditorController {
    
    @FXML
    private TableView<JobForTableView> jobTable;
    @FXML
    private TableColumn<JobForTableView, String> htmlFileNameColumn;
    @FXML
    private TableColumn<JobForTableView, Boolean> visibleColumn;
    @FXML
    private Button saveJobsButton;
    @FXML
    private Button loadJobsButton;
    @FXML
    private Button addJobButton;
    
    
    @FXML
    private CheckBox visibleCheckBox;
    @FXML
    private TextField fileNameField;
    @FXML
    private HTMLEditor contentHtmlEditor;
    @FXML
    private Button deleteButton;
    @FXML
    private ComboBox jobTypeComboBox;
            
    private MainApp mainApp;
        
    private JobForTableView currentlySelectedJob = null;
    
    @FXML
    private Label statusLabel;    
    
    private StringProperty statusString =  new SimpleStringProperty("status of connection to S3 bucket: idle");
    
    public JobEditorController() {                            
    }
    
    @FXML
    private void initialize() {
        htmlFileNameColumn.setCellValueFactory(cellData -> cellData.getValue().htmlFileKeyProperty());
        visibleColumn.setCellValueFactory(param -> param.getValue().visibleProperty());
        visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));
        
        showJobDetails(null);
        
        jobTable.getSelectionModel().selectedItemProperty().addListener(
                (obervable, oldValue, newValue) -> showJobDetails(newValue));          
        
        setFontOfHtmlEditor();
        
        fileNameField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldPropertyValue, Boolean newPropertyValue) {                
                if (!newPropertyValue) {
                    handleFileNameEdited();                    
                }                
            }
        });
        
        jobTypeComboBox.setItems(FXCollections.observableArrayList("IT", "office"));
        saveJobsButton.setDisable(true);
                
        
        statusLabel.textProperty().bindBidirectional(statusString);        
    }
    
    private void showJobDetails(JobForTableView jobForTableView) {
        if (currentlySelectedJob != null) {
            String oldJobContent = currentlySelectedJob.getHtmlContent();
            updateJobHtmlContent();            
            // apparently,the `fileNameField` listener is not triggered if you 
            // click on an item in the `jobTable`, so handleFileNameEdited
            // needs to be called here. Wish I could avoid this ...
            // The event listener works very well if you click on any other item.
            handleFileNameEdited();
        }
        currentlySelectedJob = jobForTableView;
        if (jobForTableView != null) {
            visibleCheckBox.setSelected(jobForTableView.isVisible());
            fileNameField.setText(jobForTableView.getHtmlFileKey());
            contentHtmlEditor.setHtmlText(jobForTableView.getHtmlContent());
            if (jobForTableView.getJob().getJobType() == JobType.OFFICE) {                
                jobTypeComboBox.setValue("office");
            }
            else {                
                jobTypeComboBox.setValue("IT");
            }
            
        }
        else {
            visibleCheckBox.setSelected(false);
            fileNameField.setText("");            
        }
    }
    
    /**
     * Is called by the main application to give a reference back to itself.
     * 
     * @param mainApp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        // Add observable list data to the table
        jobTable.setItems(mainApp.getJobsForTableView());
    }

    
    @FXML
    private void handleFileNameEdited() {
        if (currentlySelectedJob == null) return;
        
        String oldFileName = currentlySelectedJob.getHtmlFileKey();
        String newFileName = fileNameField.getText();
        
        if(!newFileName.contains(".html")) {            
            Alert alert = new Alert(AlertType.WARNING);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle("Not a html file!");
            alert.setHeaderText("Not a html file!");
            alert.setContentText("File name needs a '.html' suffix.\nWill now reset to valid name.");
            alert.showAndWait();
            
            fileNameField.setText(oldFileName);
            return;
        }  
        
        if (oldFileName.equals(newFileName)) return;
        
        currentlySelectedJob.setHtmlFileKey(newFileName);
        mainApp.propagateRenamingOfFileToAllLists(oldFileName, newFileName);
        if (mainApp.hasChangesToSave()) {
            saveJobsButton.setDisable(false);
        }        
    }

    
    @FXML
    private void handleJobTypeSelected() {
        if (currentlySelectedJob == null) return;

        Job job = currentlySelectedJob.getJob();
        JobType oldJobType = job.getJobType();
        String jobTypeString = (String) jobTypeComboBox.getValue();
        if ("office".equals(jobTypeString)) {
            job.setJobType(JobType.OFFICE);                
        }
        else {
            job.setJobType(JobType.IT);
        }
        
        if (oldJobType == job.getJobType()) return;
        
        String htmlString = getBodyOfDocumentInHtmlEditor();//contentHtmlEditor.getHtmlText();
        String htmlStringWithJobTypeUpdated = Mapper.insertJobTypeIntoHtmlString(htmlString, job.getJobType());            
        contentHtmlEditor.setHtmlText(htmlStringWithJobTypeUpdated);
        updateJobHtmlContent();
    }
    
    @FXML
    private void handleVisibleCheckBoxClicked() {
        if (currentlySelectedJob == null) return;
        
        Boolean visibilityBefore = currentlySelectedJob.isVisible();
        currentlySelectedJob.setVisible(visibleCheckBox.isSelected());
        
        if (visibilityBefore == currentlySelectedJob.isVisible()) return;
        
        mainApp.addToFilesThatNeedToBeSaved(currentlySelectedJob.getHtmlFileKey());
        saveJobsButton.setDisable(false);
        
    }
    @FXML
    private void handleDeleteButtonClicked() {
        fileNameField.setText("");
        contentHtmlEditor.setHtmlText("");
        visibleCheckBox.setSelected(false);
        if (currentlySelectedJob == null) return;
        
        currentlySelectedJob.setVisible(false);
        mainApp.deleteFromJobsForTableView(currentlySelectedJob);
        
        if (mainApp.hasChangesToSave()) {
            saveJobsButton.setDisable(false);
        }
    }
        
    @FXML
    private void handleSaveJobsButtonClicked() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call(){
                updateMessage("writing jobs to S3 bucket ...");
                updateJobHtmlContent();               
                mainApp.writeJobsToAmazonS3();
                saveJobsButton.setDisable(true);
                updateMessage("idle");
                return null;
            }            
        };
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            if ("idle".equals(newMessage)){
                setStatusMessageIdle();
            } else {
                setStatusMessage(newMessage);
            }
        });
        new Thread(task).start(); 
    }    
    
    
    @FXML
    private void handleLoadJobsButtonClicked() throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException{             
                updateMessage("reading jobs from S3 bucket ...");
                mainApp.loadJobsFromAmazonS3();
                jobTable.setItems(mainApp.getJobsForTableView());
                updateMessage("idle");
                return null;
            }
        };   
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            if ("idle".equals(newMessage)){
                setStatusMessageIdle();
            } else {
                setStatusMessage(newMessage);
            }
        });
        new Thread(task).start();        
    }
    
    @FXML
    private void handleAddJobButtonClicked() {
        mainApp.showNewJobDialog();
        if (mainApp.hasChangesToSave()) {
            saveJobsButton.setDisable(false);
        }
    }
    
    
    private void updateJobHtmlContent() {
        if (currentlySelectedJob == null) return;
        
        String oldHtmlContent = currentlySelectedJob.getHtmlContent();
        currentlySelectedJob.setHtmlContent(getBodyOfDocumentInHtmlEditor());//contentHtmlEditor.getHtmlText());
        
        if (oldHtmlContent.trim().equals(currentlySelectedJob.getHtmlContent().trim())) return;        
        
        mainApp.addToFilesThatNeedToBeSaved(currentlySelectedJob.getHtmlFileKey());
        saveJobsButton.setDisable(false);        
    }
    
    private String getBodyOfDocumentInHtmlEditor(){
        Document htmlDocument = Jsoup.parse(contentHtmlEditor.getHtmlText());
        Element body = htmlDocument.select("body").first();
        return body.html();
    }
    
    /**
     * Trying to set the stupid font of the Editor!
     * Not working so far.
     */
    private void setFontOfHtmlEditor() {
        /*
        String feedback = "";
        int i = 0;
        List<String> cssIdentifier = Arrays.asList(".radio-button", ".top-toolbar", 
                ".combo-box", "combo-box", ".combo-box-base", ".combo-box-popup",
                ".bottom-toolbar", "ChoiceBox", ".choice-box");
        for (String identifier: cssIdentifier){
            Set<Node> lookupAll = contentHtmlEditor.lookupAll(identifier);
            feedback += identifier + ": " +Integer.toString(lookupAll.size()) + "<br>";
            if (".bottom-toolbar".equals(identifier) || ".top-toolbar".equals(identifier)){
                feedback += "==================================<br>";
                for (Node node: lookupAll){
                    if (node instanceof Parent){
                        ObservableList<Node> childrenUnmodifiable = ((Parent) node).getChildrenUnmodifiable();
                        feedback += "Children: " + childrenUnmodifiable.size() + "<br>";
                    }
                }
                feedback += "==================================<br>";
            }

        }
        
        
        /*
        for (Node candidate: (contentHtmlEditor.lookupAll("MenuButton"))){
            System.err.println(i);
            if(candidate instanceof MenuButton && i == 1) {
                MenuButton fontSelectionButton = (MenuButton) candidate;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {                        
                        List<MenuItem> fontSelections = fontSelectionButton.getItems();
                        System.err.println(fontSelections.size());
                        if (!fontSelections.isEmpty()) {
                            MenuItem item = fontSelections.get(0);
                            if (item instanceof RadioMenuItem) {
                                System.out.println(item.getText());
                                ((RadioMenuItem) item).setSelected(true);
                            }
                        }
                    }
                    
                });                
            }            
            i++;
            contentHtmlEditor.setHtmlText("tried to set " + i + "!");
        }
        */        
        //contentHtmlEditor.setHtmlText(feedback);
    }

    private void setStatusMessage(String message) {
        statusLabel.setTextFill(Color.web("#CC1010"));
        statusString.set("status of connection to S3 bucket: " + message);
    }

    private void setStatusMessageIdle() {
        statusLabel.setTextFill(Color.web("#000000"));
        statusString.set("status of connection to S3 bucket: idle");
    }
}
