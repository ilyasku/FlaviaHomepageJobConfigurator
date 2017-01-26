package jobpostergui.controller;

import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jobposter.model.Job;
import jobposter.model.Mapper;
import jobpostergui.MainApp;

public class NewJobController {

    private Stage dialogStage;
    private boolean addClicked = false;
    private MainApp mainApp;
    
    @FXML
    private Button cancelButton;
    @FXML
    private Button addButton;
    @FXML
    private TextField fileNameField;
    @FXML
    private TextField titleField;
    
    @FXML
    private void initialze(){        
    };
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    @FXML
    private void handleCancelButtonClicked() {
        dialogStage.close();
    }
    
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    
    @FXML
    private void handleAddButtonClicked() {
        if (!fileNameUnique()){
            Alert alert = new Alert(AlertType.WARNING);
            alert.initOwner(dialogStage);
            alert.setTitle("File name not unique.");
            alert.setHeaderText("File name not unique.");
            alert.setContentText("A file by that name already exists.\nPlease choose a different name!");
            alert.showAndWait();
        }
        else if (!fileNameValid()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.initOwner(dialogStage);
            alert.setTitle("Not a html file!");
            alert.setHeaderText("Not a html file!");
            alert.setContentText("File name needs a '.html' suffix.\nPlease change it accordingly!");
            alert.showAndWait();
        }
        else if (titleField.getText().isEmpty()){
            Alert alert = new Alert(AlertType.WARNING);
            alert.initOwner(dialogStage);
            alert.setTitle("Empty title!");
            alert.setHeaderText("Empty title!");
            alert.setContentText("Please enter some title for the new job.");
            alert.showAndWait();
        }
        else {
            System.out.println("file name fine!");
            Job job = createJob();
            job.setVisible(false);
            mainApp.addJob(job);
            mainApp.addJobForTableView(job);
            dialogStage.close();
        }
    }

    private boolean fileNameUnique() {
        Set<String> fileNames = mainApp.getHtmlFileNames();
        return !fileNames.contains(fileNameField.getText());
    }

    private boolean fileNameValid() {
        String fileName = fileNameField.getText();
        return fileName.contains(".html");
    }

    private Job createJob() {
        Job job = new Job();
        job.setHtmlFileKey(fileNameField.getText());
        String htmlContent = generateInitialHtmlContent();
        Mapper.updateJobByHtmlContent(job, htmlContent);
        return job;
    }

    private String generateInitialHtmlContent() {
        String title = titleField.getText();
        String htmlContent = "<h2 data-job-type=\"it\">" + title  + "</h2>";
        htmlContent += "<p> Put job description here! </p>";
        return htmlContent;
    }
    
}
